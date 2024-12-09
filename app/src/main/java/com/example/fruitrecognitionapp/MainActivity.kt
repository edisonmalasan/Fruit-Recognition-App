package com.example.fruitrecognitionapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            delay(3000) // Optional delay for splash screen
        }
        installSplashScreen()
        setContentView(R.layout.landing_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.landing_page)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        if (isFirstLaunch) {
            // Automatically go to GreetingActivity on first launch
            val intent = Intent(this, GreetingActivity::class.java)
            startActivity(intent)
            finish()
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
        } else {
            // Go directly to CameraActivity for subsequent launches
            val intent = Intent(this, LandingPageActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
