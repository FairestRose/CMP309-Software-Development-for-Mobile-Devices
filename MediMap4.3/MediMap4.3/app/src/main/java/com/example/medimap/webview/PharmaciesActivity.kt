package com.example.medimap.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.medimap.R

class PharmaciesActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacies)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pharmacies"

        val pharmaciesWebView: WebView = findViewById(R.id.pharmaciesWebView)

        // Enable JavaScript if the website requires it
        pharmaciesWebView.settings.javaScriptEnabled = true

        // Load a specific pharmacy website or a search results page
        val pharmacyUrl = "https://www.nhsinform.scot/scotlands-service-directory/pharmacies" // Example: NHS Find a Pharmacy
        pharmaciesWebView.loadUrl(pharmacyUrl)

        // To handle links clicked within the WebView in the WebView itself
        pharmaciesWebView.webViewClient = WebViewClient()
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