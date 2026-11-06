package com.spendnote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.spendnote.app.ui.SpendNoteApp
import com.spendnote.app.ui.theme.SpendNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendNoteTheme {
                SpendNoteApp()
            }
        }
    }
}
