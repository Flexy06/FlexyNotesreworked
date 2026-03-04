package com.example.FlexyNotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.FlexyNotes.data.NoteEntity
import com.example.FlexyNotes.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    viewModel: NotesViewModel,
    isGridView: Boolean,
    useHaptics: Boolean,
    onOpenDrawer: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsState()

    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    val haptic = LocalHapticFeedback.current

    if (selectedNoteIds.isNotEmpty()) {
        BackHandler {
            selectedNoteIds = emptySet()
        }
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Are you sure you want to permanently delete all notes in the trash? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearTrash()
                    showEmptyTrashDialog = false
                    selectedNoteIds = emptySet()
                }) {
                    Text("Empty", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
            }
        )
    }

    val showDeleteDialog = showDeleteSelectedDialog || noteToDelete != null
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteSelectedDialog = false
                noteToDelete = null
            },
            title = { Text("Delete Permanently") },
            text = { Text("Are you sure you want to permanently delete the selected note(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    if (showDeleteSelectedDialog) {
                        val notesToTrash = deletedNotes.filter { it.id in selectedNoteIds }
                        notesToTrash.forEach { viewModel.deletePermanently(it) }
                        selectedNoteIds = emptySet()
                        showDeleteSelectedDialog = false
                    } else if (noteToDelete != null) {
                        viewModel.deletePermanently(noteToDelete!!)
                        noteToDelete = null
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteSelectedDialog = false
                    noteToDelete = null
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectedNoteIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedNoteIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedNoteIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val notesToRestore = deletedNotes.filter { it.id in selectedNoteIds }
                            notesToRestore.forEach { viewModel.restoreNote(it) }
                            selectedNoteIds = emptySet()
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore selected")
                        }
                        IconButton(onClick = { showDeleteSelectedDialog = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete selected permanently")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Trash") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        if (deletedNotes.isNotEmpty()) {
                            TextButton(onClick = { showEmptyTrashDialog = true }) {
                                Text("Empty Trash")
                            }
                        }
                    }
                )
            }
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
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(deletedNotes, key = { it.id }) { note ->
                    val isSelected = selectedNoteIds.contains(note.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectedNoteIds.isNotEmpty()) {
                                        selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    }
                                },
                                onLongClick = {
                                    if (!isSelected) {
                                        if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedNoteIds = selectedNoteIds + note.id
                                    }
                                }
                            ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = note.title.ifEmpty { "Untitled" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))

                                val displayContent = if (note.isChecklist) {
                                    note.content.replace("[ ] ", "☐ ").replace("[x] ", "☑ ")
                                } else {
                                    note.content
                                }

                                Text(
                                    text = displayContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 5
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}