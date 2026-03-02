package com.example.flexynotesreworked.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flexynotesreworked.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val notes by viewModel.activeNotes.collectAsState()

    // Set to keep track of selected notes for long-press actions
    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }

    // Clears selection when pressing the back button
    if (selectedNoteIds.isNotEmpty()) {
        BackHandler {
            selectedNoteIds = emptySet()
        }
    }

    Scaffold(
        topBar = {
            if (selectedNoteIds.isNotEmpty()) {
                // Contextual Action Bar when items are selected
                TopAppBar(
                    title = { Text("${selectedNoteIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedNoteIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val notesToArchive = notes.filter { it.id in selectedNoteIds }
                            notesToArchive.forEach { viewModel.archiveNote(it) }
                            selectedNoteIds = emptySet()
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive selected")
                        }
                        IconButton(onClick = {
                            val notesToTrash = notes.filter { it.id in selectedNoteIds }
                            notesToTrash.forEach { viewModel.moveToTrash(it) }
                            selectedNoteIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                )
            } else {
                // Default TopAppBar
                TopAppBar(
                    title = { Text("Notes") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedNoteIds.isEmpty()) {
                FloatingActionButton(onClick = { onNavigateToEditor(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Create new note")
                }
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
                Text("No active notes. Create your first one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Key is required for SwipeToDismiss to correctly animate list changes
                items(notes, key = { it.id }) { note ->
                    val isSelected = selectedNoteIds.contains(note.id)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue != SwipeToDismissBoxValue.Settled) {
                                viewModel.archiveNote(note)
                                true // Confirm dismissal
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }, label = "swipeColor"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 8.dp)
                                    .background(color, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    // combinedClickable replaces normal clickable
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedNoteIds.isNotEmpty()) {
                                                // Toggle selection if action bar is active
                                                selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                            } else {
                                                onNavigateToEditor(note.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelected) {
                                                selectedNoteIds = selectedNoteIds + note.id
                                            }
                                        }
                                    ),
                                // Give visual feedback when a note is selected
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                )
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
                    )
                }
            }
        }
    }
}