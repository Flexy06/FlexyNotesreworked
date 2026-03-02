package com.example.flexynotesreworked.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexynotesreworked.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: NotesViewModel,
    onOpenDrawer: () -> Unit,
    // Note: Parameter kept for compatibility with MainActivity, even if unused here now
    onNavigateToEditor: (Long) -> Unit
) {
    val archivedNotes by viewModel.archivedNotes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (archivedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Your archive is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(archivedNotes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            // Click now automatically unarchives the note and returns it to the main list
                            .clickable { viewModel.unarchiveNote(note) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            IconButton(onClick = { viewModel.unarchiveNote(note) }) {
                                Icon(Icons.Default.Unarchive, contentDescription = "Unarchive")
                            }
                        }
                    }
                }
            }
        }
    }
}