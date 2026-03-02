package com.example.flexynotesreworked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            // hiltViewModel() automatically provides the ViewModel via Dagger Hilt
            val viewModel: NotesViewModel = hiltViewModel()
            NotesListScreen(
                viewModel = viewModel,
                onNavigateToEditor = { navController.navigate("note_editor") }
            )
        }
        composable("note_editor") {
            val viewModel: NotesViewModel = hiltViewModel()
            NoteEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: () -> Unit
) {
    // Collects the StateFlow from ViewModel and triggers a UI update when data changes
    val notes by viewModel.notes.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToEditor) {
                Icon(Icons.Default.Add, contentDescription = "Create new note")
            }
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes yet. Create your first one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { /* TODO: Open existing note to edit */ }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = note.title.ifEmpty { "Untitled" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Note") },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                viewModel.addNote(title, content)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    // Large height to properly test scrolling behavior with the keyboard
                    .height(300.dp)
            )
        }
    }
}