package com.example.flexynotesreworked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flexynotesreworked.ui.theme.FlexyNotesreworkedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Testing OLED mode with hardcoded true
            FlexyNotesreworkedTheme(isOledMode = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlexyNotesNavigation()
                }
            }
        }
    }
}

@Composable
fun FlexyNotesNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "notes_list",
        modifier = Modifier.systemBarsPadding()
    ) {
        composable("notes_list") {
            NotesListPlaceholder(
                onNavigateToEditor = { navController.navigate("note_editor") }
            )
        }
        composable("note_editor") {
            NoteEditorPlaceholder()
        }
    }
}

@Composable
fun NotesListPlaceholder(onNavigateToEditor: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onNavigateToEditor) {
            Text("Create New Note")
        }
    }
}

@Composable
fun NoteEditorPlaceholder() {
    var text by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // IME padding ensures the content stays above the keyboard
            .verticalScroll(scrollState)
            .imePadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Editor Setup Test",
            style = MaterialTheme.typography.headlineMedium
        )

        // Spacer to force the textfield to the bottom for scrolling tests
        Spacer(modifier = Modifier.height(600.dp))

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Tap here to test keyboard inset") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}