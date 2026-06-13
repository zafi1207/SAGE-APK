package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.SageApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize notification structures and schedule the daily alarm
    com.example.notifications.NotificationHelper.createNotificationChannel(this)
    com.example.notifications.NotificationHelper.scheduleDailyAlarm(this)

    val viewModel = androidx.lifecycle.ViewModelProvider(this).get(com.example.viewmodel.SageViewModel::class.java)
    if (intent?.getBooleanExtra("navigate_bills", false) == true) {
      viewModel.setRequestedTab(1)
    }

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val darkTheme = when (themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = darkTheme) {
        SageApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra("navigate_bills", false) == true) {
      val viewModel = androidx.lifecycle.ViewModelProvider(this).get(com.example.viewmodel.SageViewModel::class.java)
      viewModel.setRequestedTab(1)
    }
  }
}

