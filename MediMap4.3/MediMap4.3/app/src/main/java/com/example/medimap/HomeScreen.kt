import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.medimap.medicationactivities.EditMedicationActivity
import com.example.medimap.database.Medication
import com.example.medimap.dao.MedicationDao
import com.example.medimap.medicationsetup.MedicationSetupActivity
import com.example.medimap.MedicationViewModel
import com.example.medimap.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun HomeScreen(medicationViewModel: MedicationViewModel) {
    val medications: List<Medication> by medicationViewModel.allMedications.observeAsState(emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = { HomeTopAppBar() },
        bottomBar = { HomeBottomNavBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            DateSection()
            // More flexible image height
            LogSection(medications = medications, onMedicationTaken = { medicationId ->
                medicationViewModel.markMedicationAsTaken(medicationId, LocalTime.now())
            }, medicationViewModel = medicationViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Your Scheduled Medications",
                    style = MaterialTheme.typography.subtitle1
                )
                Button(
                    onClick = {
                        val intent = Intent(context, MedicationSetupActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(40.dp), // Make the button smaller
                    contentPadding = PaddingValues(0.dp) // Remove default padding
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Medication",
                        modifier = Modifier.size(24.dp) // Make the icon inside smaller
                    )
                }
            }
            MedicationListDisplay(medications = medications, medicationViewModel = medicationViewModel)
        }
    }
}
@Composable
fun HomeTopAppBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pill),
                    contentDescription = "Pill Icon",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    " Med Map",
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun DateSection() {

    val currentDate = remember { java.time.LocalDate.now() }
    val formattedDate = remember(currentDate) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.getDefault())
        currentDate.format(formatter)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(formattedDate, style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            days.forEachIndexed { index, day ->
                val dayOfWeek = currentDate.dayOfWeek.value
                val isSelected = (index + 1) == dayOfWeek
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, androidx.compose.ui.graphics.Color.Gray)
                        .background(if (isSelected) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = day)
                }
            }
        }
    }
}

@Composable
fun LogSection(medications: List<Medication>, onMedicationTaken: (Int) -> Unit, medicationViewModel: MedicationViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Today's Medications", style = MaterialTheme.typography.subtitle1)
            Spacer(modifier = Modifier.height(8.dp))
            if (medications.isEmpty()) {
                Text("No medications scheduled for today.")
            } else {
                medications.forEach { medication ->
                    MedicationLogItem(
                        medication = medication,
                        hasBeenTakenToday = medicationViewModel.hasBeenTakenToday(medication),
                        onTaken = { onMedicationTaken(medication.id) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun MedicationLogItem(medication: Medication, hasBeenTakenToday: Boolean, onTaken: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()

            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = medication.name, style = MaterialTheme.typography.subtitle2)
            Text(text = "Dosage: ${medication.dosage}, Timing: ${medication.timing}", style = MaterialTheme.typography.caption)
        }
        if (hasBeenTakenToday) {
            Icon(Icons.Filled.Check, contentDescription = "Medication Taken", tint = Color.Green)
        } else {
            Button(onClick = onTaken) {
                Text("Take")
            }
        }
    }
}

@Composable
fun MedicationListDisplay(medications: List<Medication>, medicationViewModel: MedicationViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (medications.isNotEmpty()) {
            Text("All Medications:", style = MaterialTheme.typography.subtitle1)
            Spacer(modifier = Modifier.height(8.dp))
            medications.forEach { medication ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${medication.name} - ${medication.dosage} - ${medication.timing}")
                    Row { // Container for the icons
                        val context = LocalContext.current
                        IconButton(onClick = {
                            val intent = Intent(context, EditMedicationActivity::class.java)
                            intent.putExtra(EditMedicationActivity.EXTRA_MEDICATION_ID, medication.id)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${medication.name}")
                        }
                        Spacer(modifier = Modifier.width(8.dp)) // Add some space between icons
                        IconButton(onClick = {
                            medicationViewModel.deleteMedication(medication)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${medication.name}", tint = Color.Red)
                        }
                    }
                }
            }
        } else {
            Text("No medications added yet.")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBottomNavBar() {
    BottomNavigation {
        BottomNavigationItem(
            selected = true,
            onClick = { /* Handle navigation */ },
            icon = {}, // No icon
            label = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp), // Adjust vertical padding as needed
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProvideTextStyle(
                        value = androidx.compose.material3.LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        content = {
                            Text(
                                text = "Home",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
            },
            alwaysShowLabel = true // Ensure the label is always shown
        )
    }
}

@Composable
fun AddMedicationDialog(onDismiss: () -> Unit, onMedicationAdded: (String, String, String, String) -> Unit) {
    var medicationName by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var dosage by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var timing by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var duration by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Medication", style = MaterialTheme.typography.h6)
                TextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text("Medication Name") })
                TextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") })
                TextField(
                    value = timing,
                    onValueChange = { timing = it },
                    label = { Text("Timing") })
                TextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
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


class MedicationViewModel(private val medicationDao: MedicationDao) : ViewModel() {
    val allMedications: LiveData<List<Medication>> = medicationDao.getAll().asLiveData()
    private val _takenMedicationIds = MutableStateFlow<Set<Int>>(emptySet())
    val takenMedicationIds = _takenMedicationIds.asStateFlow()

    fun addMedication(medication: Medication) {
        viewModelScope.launch {
            medicationDao.insert(medication)
        }
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
        _takenMedicationIds.update { it + medicationId }
    }

    fun isMedicationTaken(medicationId: Int): StateFlow<Boolean> {
        return takenMedicationIds.map { it.contains(medicationId) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
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