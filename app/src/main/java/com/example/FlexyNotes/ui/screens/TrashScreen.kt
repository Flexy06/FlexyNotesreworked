package com.example.FlexyNotes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.FlexyNotes.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NotesViewModel,
    onOpenDrawer: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    if (deletedNotes.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearTrash() }) {
                            Text("Empty Trash")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (deletedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Trash is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(deletedNotes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
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
                            // Restore button
                            IconButton(onClick = { viewModel.restoreNote(note) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore")
                            }
                            // Delete permanently button
                            IconButton(onClick = { viewModel.deletePermanently(note) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently")
                            }
                        }
                    }
                }
            }
        }
    }
}