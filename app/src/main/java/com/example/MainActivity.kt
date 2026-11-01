package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uare.core.RuntimeExplorer
import com.example.uare.ui.screens.MainExplorerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Universal Android Runtime Explorer (UARE) SDK
        RuntimeExplorer.initialize(this)

        // Register current Activity instance for live interactive reflection and object inspection
        RuntimeExplorer.registerObject(this, "MainActivity (Foreground Activity)", retainStrong = false)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainExplorerScreen()
            }
        }
    }
}
