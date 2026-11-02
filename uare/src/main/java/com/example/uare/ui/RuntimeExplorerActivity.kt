package com.example.uare.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.uare.core.RuntimeExplorer
import com.example.uare.ui.screens.MainExplorerScreen

/**
 * Pre-built Activity provided by the UARE library.
 * Any Android application can launch this Activity directly to open the Runtime Explorer.
 */
class RuntimeExplorerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure RuntimeExplorer is initialized
        RuntimeExplorer.initialize(this)
        RuntimeExplorer.registerObject(this, "RuntimeExplorerActivity (Explorer Host)", retainStrong = false)

        enableEdgeToEdge()

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                MainExplorerScreen()
            }
        }
    }
}
