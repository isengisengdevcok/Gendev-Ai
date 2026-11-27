package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.GenDevRepository
import com.example.security.configureAppCheck
import com.example.ui.GenDevApp
import com.example.ui.theme.GenDevTheme
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.GenDevViewModelFactory
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val repository = GenDevRepository(applicationContext)
        val factory = GenDevViewModelFactory(repository)
        val chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
        val studioViewModel = ViewModelProvider(this, factory)[StudioViewModel::class.java]

        setContent {
            GenDevTheme {
                GenDevApp(
                    chatViewModel = chatViewModel,
                    studioViewModel = studioViewModel
                )
            }
        }
    }
}
