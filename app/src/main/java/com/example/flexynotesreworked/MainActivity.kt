package com.example.flexynotesreworked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flexynotesreworked.ui.screens.NoteEditorScreen
import com.example.flexynotesreworked.ui.screens.NotesListScreen
import com.example.flexynotesreworked.ui.theme.FlexyNotesreworkedTheme
import com.example.flexynotesreworked.viewmodel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
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
            val viewModel: NotesViewModel = hiltViewModel()
            NotesListScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId ->
                    // Passing the note ID to the editor route (or -1 if it's a new note)
                    navController.navigate("note_editor/${noteId ?: -1L}")
                }
            )
        }

        // Defined a route with an argument 'noteId'
        composable("note_editor/{noteId}") { backStackEntry ->
            val viewModel: NotesViewModel = hiltViewModel()
            val noteIdStr = backStackEntry.arguments?.getString("noteId")
            val noteId = noteIdStr?.toLongOrNull()?.takeIf { it != -1L }

            NoteEditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}