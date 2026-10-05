package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (_: Exception) {}

        setContent {
            val currentUser by com.example.data.auth.AuthManager.currentUser.collectAsStateWithLifecycle()
            val palette by viewModel.selectedPalette.collectAsStateWithLifecycle()
            val darkModeOption by viewModel.darkModeOption.collectAsStateWithLifecycle()

            MyApplicationTheme(
                palette = palette,
                darkModeOption = darkModeOption
            ) {
                if (currentUser == null) {
                    com.example.ui.screens.auth.LoginScreen(
                        onLoginSuccess = {
                            // currentUser StateFlow updates in AuthManager
                        }
                    )
                } else {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
