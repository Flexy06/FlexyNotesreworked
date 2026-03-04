package com.example.FlexyNotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.FlexyNotes.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    isGridView: Boolean,
    useHaptics: Boolean,
    onGridViewToggle: () -> Unit,
    onNavigateToEditor: (Long?, Boolean) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val notes by viewModel.activeNotes.collectAsState()
    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    var showFabMenu by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    if (selectedNoteIds.isNotEmpty()) {
        BackHandler {
            selectedNoteIds = emptySet()
        }
    }

    if (showFabMenu) {
        BackHandler {
            showFabMenu = false
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
                TopAppBar(
                    title = { Text("Notes") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onGridViewToggle) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = "Toggle view"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedNoteIds.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            // Unified, large clickable pill for Text Note
                            Surface(
                                onClick = {
                                    showFabMenu = false
                                    onNavigateToEditor(null, false)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 4.dp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = "Text Note",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = "New Text Note",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Unified, large clickable pill for Checklist
                            Surface(
                                onClick = {
                                    showFabMenu = false
                                    onNavigateToEditor(null, true)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 4.dp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = "Checklist",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = "New Checklist",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFabMenu = !showFabMenu
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val rotation by animateFloatAsState(targetValue = if (showFabMenu) 45f else 0f, label = "fab_rotate")
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create new",
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        val isSelected = selectedNoteIds.contains(note.id)

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue != SwipeToDismissBoxValue.Settled) {
                                    viewModel.archiveNote(note)
                                    true
                                } else {
                                    false
                                }
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                        )

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

                        val rawOffset = try { dismissState.requireOffset() } catch(_: Exception) { 0f }
                        val isPastThreshold = dismissState.targetValue != SwipeToDismissBoxValue.Settled

                        val targetResistance = if (!isPastThreshold && rawOffset != 0f) {
                            -rawOffset * 0.4f
                        } else {
                            0f
                        }

                        val animatedResistance by animateFloatAsState(
                            targetValue = targetResistance,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "resistance"
                        )

                        val isSwiping = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled ||
                                dismissState.targetValue != SwipeToDismissBoxValue.Settled

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.zIndex(if (isSwiping) 1f else 0f),
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Transparent
                                    }, label = "swipeColor"
                                )

                                val alignment = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.CenterStart
                                }

                                val paddingStart = if (alignment == Alignment.CenterStart) 20.dp else 0.dp
                                val paddingEnd = if (alignment == Alignment.CenterEnd) 20.dp else 0.dp

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, MaterialTheme.shapes.medium)
                                        .padding(start = paddingStart, end = paddingEnd),
                                    contentAlignment = alignment
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
                                        .graphicsLayer { translationX = animatedResistance }
                                        .combinedClickable(
                                            onClick = {
                                                if (selectedNoteIds.isNotEmpty()) {
                                                    selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                                } else {
                                                    onNavigateToEditor(note.id, false)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelected) {
                                                    if (useHaptics) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
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
                                    Column(modifier = Modifier.padding(16.dp)) {
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
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showFabMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showFabMenu = false }
                )
            }
        }
    }
}