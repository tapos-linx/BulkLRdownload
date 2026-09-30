package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.ArchiveMainScreen
import com.example.ui.theme.LandArchiveBDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LandArchiveBDTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArchiveMainScreen()
                }
            }
        }
    }
}
