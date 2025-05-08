package com.example.medimap.medicationsetup

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.medimap.medicationactivities.AddMedicationActivity
import com.example.medimap.MainActivity
import com.example.medimap.R
import com.example.medimap.webview.PharmaciesActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MedicationSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_setup)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // Add the image and text above "Track all your medications in one place"
        val headerImage: ImageView = findViewById(R.id.headerImage)
        val headerText: TextView = findViewById(R.id.headerText)

        // Set the image.  Use a Context to get the drawable.
        headerImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pill))
        headerText.text = "Set Up Medications" // Set the text
        headerText.textSize = 20f // Set the text size (optional)


        val addMedicationButton: Button = findViewById(R.id.addMedicationButton)
        val pharmaciesButton: Button = findViewById(R.id.pharmaciesButton)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation) // Get the BottomNavigationView

        addMedicationButton.setOnClickListener {
            // Logic to add a medication
            val intent = Intent(this, AddMedicationActivity::class.java)
            startActivity(intent)
        }

        pharmaciesButton.setOnClickListener {
            // Logic to add a medication
            val intent = Intent(this, PharmaciesActivity::class.java)
            startActivity(intent)
        }

        // Set a listener for the BottomNavigationView
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    val context = this // Capture the context
                    // Start the HomeScreen Composable.  HomeScreen is a Composable, so we launch it in a Composable context.
                    startActivity(Intent(context, MainActivity::class.java).apply{
                        putExtra("navigateToHomeScreen", true)
                    })
                    true // Return true to indicate that the item click is handled
                }
                // Add cases for other bottom navigation items as needed
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}