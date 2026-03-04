package com.example.FlexyNotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.FlexyNotes.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: NotesViewModel,
    isGridView: Boolean,
    useHaptics: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    val haptic = LocalHapticFeedback.current

    if (selectedNoteIds.isNotEmpty()) {
        BackHandler {
            selectedNoteIds = emptySet()
        }
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
                            val notesToUnarchive = archivedNotes.filter { it.id in selectedNoteIds }
                            notesToUnarchive.forEach { viewModel.unarchiveNote(it) }
                            selectedNoteIds = emptySet()
                        }) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Unarchive selected")
                        }
                        IconButton(onClick = {
                            val notesToTrash = archivedNotes.filter { it.id in selectedNoteIds }
                            notesToTrash.forEach { viewModel.moveToTrash(it) }
                            selectedNoteIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Archive") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (archivedNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Your archive is empty.")
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(archivedNotes, key = { it.id }) { note ->
                    val isSelected = selectedNoteIds.contains(note.id)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue != SwipeToDismissBoxValue.Settled) {
                                viewModel.unarchiveNote(note)
                                true
                            } else false
                        },
                        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                    )

                    // Realtime haptic feedback
                    var previousTarget by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }
                    LaunchedEffect(dismissState) {
                        snapshotFlow { dismissState.targetValue }.collect { currentTarget ->
                            if (useHaptics) {
                                if (previousTarget == SwipeToDismissBoxValue.Settled && currentTarget != SwipeToDismissBoxValue.Settled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (previousTarget != SwipeToDismissBoxValue.Settled && currentTarget == SwipeToDismissBoxValue.Settled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            previousTarget = currentTarget
                        }
                    }

                    // Physical resistance logic
                    val rawOffset = try { dismissState.requireOffset() } catch(_: Exception) { 0f }
                    val isPastThreshold = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                    val targetResistance = if (!isPastThreshold && rawOffset != 0f) -rawOffset * 0.4f else 0f

                    val animatedResistance by animateFloatAsState(
                        targetValue = targetResistance,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "resistance"
                    )

                    val isSwiping = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled ||
                            dismissState.targetValue != SwipeToDismissBoxValue.Settled

                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.zIndex(if (isSwiping) 1f else 0f),
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                label = "swipeColor"
                            )
                            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            Box(
                                modifier = Modifier.fillMaxSize().background(color, MaterialTheme.shapes.medium).padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { translationX = animatedResistance }
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedNoteIds.isNotEmpty()) {
                                                selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                            } else onNavigateToEditor(note.id)
                                        },
                                        onLongClick = {
                                            if (!isSelected) {
                                                if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedNoteIds = selectedNoteIds + note.id
                                            }
                                        }
                                    ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = note.title.ifEmpty { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                                    if (note.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val displayContent = if (note.isChecklist) note.content.replace("[ ] ", "☐ ").replace("[x] ", "☑ ") else note.content
                                        Text(text = displayContent, style = MaterialTheme.typography.bodyMedium, maxLines = 5)
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