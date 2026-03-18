package com.flexynotes.ui.screens

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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flexynotes.app.R
import com.flexynotes.viewmodel.NotesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: NotesViewModel,
    isGridView: Boolean,
    useHaptics: Boolean,
    isOledMode: Boolean = false,
    onOpenDrawer: () -> Unit,
    onNavigateToEditor: (String) -> Unit
) {
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()

    var selectedNoteIds by remember { mutableStateOf(setOf<String>()) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val minDragDistance = with(density) { 100.dp.toPx() }

    if (selectedNoteIds.isNotEmpty()) {
        BackHandler {
            selectedNoteIds = emptySet()
        }
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
                    title = { Text(stringResource(R.string.nav_archive)) },
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
                Text(stringResource(R.string.archive_empty))
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

                    val stateHolder = remember { arrayOfNulls<SwipeToDismissBoxState>(1) }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue != SwipeToDismissBoxValue.Settled) {
                                val offset = try { stateHolder[0]?.requireOffset() ?: 0f } catch (e: Exception) { 0f }
                                if (Math.abs(offset) < minDragDistance) return@rememberSwipeToDismissBoxState false

                                viewModel.unarchiveNote(note)
                                true
                            } else false
                        },
                        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                    )
                    stateHolder[0] = dismissState

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
                    val targetResistance = if (!isPastThreshold && rawOffset != 0f) -rawOffset * 0.4f else 0f

                    val animatedResistance by animateFloatAsState(
                        targetValue = targetResistance,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "resistance"
                    )

                    val isSwiping = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled ||
                            dismissState.targetValue != SwipeToDismissBoxValue.Settled

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
                        if (isDarkTheme) noteCardColorsDark[colorIndex % noteCardColorsDark.size]
                        else noteCardColors[colorIndex % noteCardColors.size]
                    } else when {
                        isOledMode && isDarkTheme -> Color(0xFF0C0C0C)
                        isDarkTheme               -> Color(0xFF242426)
                        else                      -> Color(0xFFF1F2F5)
                    }

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



                            ){
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = note.title.ifEmpty { stringResource(R.string.untitled) }, style = MaterialTheme.typography.titleMedium)
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
                                        Text(text = note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5)
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