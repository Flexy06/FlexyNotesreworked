package com.flexynotes.ui.screens

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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flexynotes.app.R
import com.flexynotes.data.NoteEntity
import com.flexynotes.viewmodel.NotesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    viewModel: NotesViewModel,
    isGridView: Boolean,
    useHaptics: Boolean,
    isOledMode: Boolean = false,
    onOpenDrawer: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()

    // Changed Set<Long> to Set<String>
    var selectedNoteIds by remember { mutableStateOf(setOf<String>()) }
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
            title = { Text(stringResource(R.string.empty_trash)) },
            text = { Text(stringResource(R.string.empty_trash_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearTrash()
                    showEmptyTrashDialog = false
                    selectedNoteIds = emptySet()
                }) {
                    Text(stringResource(R.string.empty), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text(stringResource(R.string.cancel)) }
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
            title = { Text(stringResource(R.string.delete_permanently)) },
            text = { Text(stringResource(R.string.delete_permanently_desc)) },
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
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteSelectedDialog = false
                    noteToDelete = null
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectedNoteIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.selected_count, selectedNoteIds.size)) },
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
                    title = { Text(stringResource(R.string.nav_trash)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        if (deletedNotes.isNotEmpty()) {
                            TextButton(onClick = { showEmptyTrashDialog = true }) {
                                Text(stringResource(R.string.empty_trash))
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
                Text(stringResource(R.string.trash_empty))
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


                    val noteCardColors = listOf(
                        Color(0xFFE8F5E9), Color(0xFFFFF8E1), Color(0xFFE3F2FD),
                        Color(0xFFFCE4EC), Color(0xFFEDE7F6), Color(0xFFE0F7FA),
                        Color(0xFFFFF3E0), Color(0xFFF3E5F5),
                    )
                    val noteCardColorsDark = listOf(
                        Color(0xFF1B2E1E), Color(0xFF2E2A14), Color(0xFF152130),
                        Color(0xFF2E1820), Color(0xFF1E1A2E), Color(0xFF122428),
                        Color(0xFF2E2418), Color(0xFF271A2E),
                    )
                    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val colorIndex = note.colorIndex
                    val noteColor = if (colorIndex != null) {
                        if (isOledMode && isDarkTheme) noteCardColorsDark[colorIndex % noteCardColorsDark.size]
                        else if (isDarkTheme) noteCardColorsDark[colorIndex % noteCardColorsDark.size]
                        else noteCardColors[colorIndex % noteCardColors.size]
                    } else when {
                        isOledMode && isDarkTheme -> Color(0xFF0C0C0C)
                        isDarkTheme               -> Color(0xFF242426)
                        else                      -> Color(0xFFF1F2F5)
                    }


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
                            BorderStroke(
                                width = 1.dp,
                                color = when {
                                    isOledMode && isDarkTheme -> Color.White.copy(alpha = 0.12f)
                                    isDarkTheme               -> Color.White.copy(alpha = 0.10f)
                                    else                      -> Color.Black.copy(alpha = 0.12f)
                                }
                            )
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else noteColor
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = note.title.ifEmpty { stringResource(R.string.untitled) },
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (note.isChecklist) {
                                val checklistItems = note.content.lines()
                                    .filter { it.startsWith("[ ] ") || it.startsWith("[x] ") }
                                    .take(5)
                                if (checklistItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    checklistItems.forEach { line ->
                                        val isChecked = line.startsWith("[x] ")
                                        val label = if (isChecked) line.removePrefix("[x] ") else line.removePrefix("[ ] ")
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = if (isChecked)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                style = if (isChecked)
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        textDecoration = TextDecoration.LineThrough
                                                    )
                                                else
                                                    MaterialTheme.typography.bodyMedium,
                                                color = if (isChecked)
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 5
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { viewModel.restoreNote(note) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore")
                                }
                                IconButton(onClick = { noteToDelete = note }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}