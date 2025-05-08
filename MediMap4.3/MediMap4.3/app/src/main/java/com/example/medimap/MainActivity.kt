// Med Map.kt - Main Activity
package com.example.medimap

import HomeScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medimap.database.Medication
import com.example.medimap.dao.MedicationDao
import com.example.medimap.database.AppDatabase
import com.example.medimap.medicationsetup.MedicationSetupActivity
import com.example.medimap.ui.theme.MediMapTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

class MedicationViewModel(private val medicationDao: MedicationDao) : ViewModel() {
    val allMedications: LiveData<List<Medication>> = medicationDao.getAll().asLiveData()

    // Modify this function to return Long
    suspend fun addMedication(medication: Medication): Long {
        return medicationDao.insert(medication)
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            medicationDao.update(medication)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationDao.delete(medication)
        }
    }

    fun markMedicationAsTaken(medicationId: Int, takenAt: LocalTime) {
        viewModelScope.launch {
            val nowMillis = System.currentTimeMillis()
            medicationDao.updateLastTaken(medicationId, nowMillis)
            // Optionally, update a LiveData or StateFlow if you want immediate UI feedback
        }
    }

    fun getMedicationById(id: Int): LiveData<Medication?> = liveData {
        emit(medicationDao.getMedicationById(id))
    }

    fun hasBeenTakenToday(medication: Medication): Boolean {
        val lastTakenTime = medication.lastTaken ?: return false
        val lastTakenDate = java.time.Instant.ofEpochMilli(lastTakenTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val today = java.time.LocalDate.now()
        return lastTakenDate == today
    }
}

class MedicationViewModelFactory(private val medicationDao: MedicationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicationViewModel(medicationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room database
        val database = AppDatabase.getInstance(applicationContext)
        val medicationDao = database.medicationDao()
        // Initialize ViewModel
        val viewModelFactory = MedicationViewModelFactory(medicationDao)
        val medicationViewModel = ViewModelProvider(this, viewModelFactory).get(MedicationViewModel::class.java)


        setContent {
            MediMapApp(medicationViewModel)
        }
    }
}

// The MediMapApp composable function sets up the app's theme and navigation.
@Composable
fun MediMapApp(medicationViewModel: MedicationViewModel) {
    MediMapTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "HomeScreen") { // Set HomeScreen as the start destination if you want it to show first
            composable("medicationList") {
                MedicationListScreen(medicationViewModel = medicationViewModel)
            }
            composable("homeScreen") {
                HomeScreen(medicationViewModel = medicationViewModel) // Pass the ViewModel here
            }
            composable("pharmaciesScreen") {
                // Content for your pharmacies screen (e.g., a WebView or a Compose UI)
                Text("Pharmacies Screen Content") // Placeholder
            }
        }
    }
}

@Composable
fun MedicationListScreen(medicationViewModel: MedicationViewModel) {
    // Create a state for the list of medications using LiveData.  Observe the LiveData
    val medications: List<Medication> by medicationViewModel.allMedications.observeAsState(emptyList())

    // Create a state to control showing or hiding the "Add Medication" dialog
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current // Get context here, within the Composable scope

    // Scaffold provides the structure for the UI (with FAB, TopAppBar, etc.)
    Scaffold(
        floatingActionButton = {
            // FloatingActionButton for adding a new medication
            FloatingActionButton(onClick = { showDialog = true }) {
                // Show the dialog when the FAB is clicked
                Icon(Icons.Filled.Add, contentDescription = "Add Medication")
            }
        },
        topBar = {
            // TopAppBar for the header of the screen
            TopAppBar(title = { Text("MediMap") })
        }
    ) { paddingValues ->
        // Column for arranging the content in a vertical sequence with padding
        Column(modifier = Modifier.padding(paddingValues)) {
            // LazyColumn to efficiently display the list of medications
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Display each medication in the list with a custom item composable
                items(medications) { medication ->
                    MedicationItem(medication = medication, onDelete = {
                        // Handle deletion of the medication when the delete icon is clicked
                        medicationViewModel.deleteMedication(medication)
                    }, onEdit = {
                        val intent = Intent(context, MedicationSetupActivity::class.java).apply {
                            putExtra("medication_id", medication.id)
                            putExtra("medication_name", medication.name)
                            putExtra("medication_dosage", medication.dosage)
                            putExtra("medication_timing", medication.timing)
                        }
                        context.startActivity(intent)
                    })
                }
            }
        }
    }

    // If the dialog flag is true, show the AddMedicationDialog
    if (showDialog) {
        // AddMedicationDialog composable for adding new medication
        AddMedicationDialog(
            onDismiss = { showDialog = false },
            onMedicationAdded = { medicationName, dosage, timing, duration ->
                // Launch a coroutine to call the suspend function
                medicationViewModel.viewModelScope.launch {
                    // Add the new medication to the list and close the dialog
                    val newMedication =
                        Medication(name = medicationName, dosage = dosage, timing = timing)
                    medicationViewModel.addMedication(newMedication)
                    showDialog = false
                }
            })
    }
}
// MedicationItem composable to display each individual medication in the list
@Composable
fun MedicationItem(medication: Medication, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth() // Make the row span the entire width
            .padding(16.dp), // Add padding around the row
        horizontalArrangement = Arrangement.SpaceBetween, // Space between the text and icon
        verticalAlignment = Alignment.CenterVertically // Align items vertically in the center
    ) {
        // Column for displaying the medication's name and dosage
        Column {
            Text(
                text = medication.name,
                style = TextStyle(fontWeight = FontWeight.Bold)
            ) // Medication name
            Text(text = "Dosage: ${medication.dosage}") // Medication dosage
        }
        Row{
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
            // IconButton to delete the medication from the list
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete") // Display the delete icon
            }
        }

    }
    Divider() // Divider between items in the list for separation
}

// AddMedicationDialog composable to display the dialog for adding a new medication
@Composable
fun AddMedicationDialog(onDismiss: () -> Unit, onMedicationAdded: (String, String, String, String) -> Unit) {
    // State to track the user's input in the dialog's text fields
    var medicationName by remember { mutableStateOf(TextFieldValue("")) }
    var dosage by remember { mutableStateOf(TextFieldValue("")) }
    var timing by remember { mutableStateOf(TextFieldValue("")) }
    var duration by remember { mutableStateOf(TextFieldValue("")) }

    // Dialog composable to show a modal window with input fields
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth() // Make the surface span the full width of the screen
                .padding(16.dp), // Add padding around the surface
            shape = MaterialTheme.shapes.medium, // Rounded corners for the dialog
            color = MaterialTheme.colors.surface // Use the app's surface color
        ) {
            // Column for arranging the dialog content vertically with spacing
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Medication", style = MaterialTheme.typography.h6) // Dialog title
                // Input field for medication name
                TextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text("Medication Name") })
                // Input field for dosage
                TextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") })
                // Input field for timing
                TextField(
                    value = timing,
                    onValueChange = { timing = it },
                    label = { Text("Timing") })
                // Input field for duration
                TextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") })
                // Row for arranging the action buttons horizontally
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // Align buttons to the right
                ) {
                    // "Cancel" button that dismisses the dialog without adding medication
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    // "Add" button that adds the medication to the list and dismisses the dialog
                    TextButton(onClick = {
                        onMedicationAdded(medicationName.text, dosage.text, timing.text, duration.text)
                    }) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

// Preview of the MedicationListScreen to visualize the UI
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {

    //MedicationListScreen() // Show the medication list screen in the preview
}