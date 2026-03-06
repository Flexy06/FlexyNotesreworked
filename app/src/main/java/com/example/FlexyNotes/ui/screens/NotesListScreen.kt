package com.flexynotes.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flexynotes.app.R
import com.flexynotes.viewmodel.NotesViewModel

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

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    val haptic = LocalHapticFeedback.current

    val displayedNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            try {
                searchFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (selectedNoteIds.isNotEmpty()) {
        BackHandler { selectedNoteIds = emptySet() }
    } else if (isSearching) {
        BackHandler {
            isSearching = false
            searchQuery = ""
        }
    } else if (showFabMenu) {
        BackHandler { showFabMenu = false }
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
            } else if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_notes)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear text")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_notes)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search notes")
                        }
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
            if (selectedNoteIds.isEmpty() && !isSearching) {
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
                                        text = stringResource(R.string.text_note),
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
                                        text = stringResource(R.string.checklist),
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
                    Text(stringResource(R.string.notes_empty))
                }
            } else if (displayedNotes.isEmpty() && isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_search_results, searchQuery))
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
                    items(displayedNotes, key = { it.id }) { note ->
                        val isSelected = selectedNoteIds.contains(note.id)

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        // Swipe right -> Archive
                                        viewModel.archiveNote(note)
                                        true
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        // Swipe left -> Trash
                                        viewModel.moveToTrash(note)
                                        true
                                    }
                                    else -> false
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
                        val targetResistance = if (!isPastThreshold && rawOffset != 0f) -rawOffset * 0.4f else 0f

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
                                val targetColor = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }

                                val color by animateColorAsState(
                                    targetValue = targetColor,
                                    label = "swipeColor"
                                )

                                val alignment = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.CenterStart
                                }

                                val paddingStart = if (alignment == Alignment.CenterStart) 20.dp else 0.dp
                                val paddingEnd = if (alignment == Alignment.CenterEnd) 20.dp else 0.dp

                                val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Archive
                                val iconTint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, MaterialTheme.shapes.medium)
                                        .padding(start = paddingStart, end = paddingEnd),
                                    contentAlignment = alignment
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) "Delete" else "Archive",
                                        tint = iconTint
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
                                                    if (isSearching) {
                                                        isSearching = false
                                                        searchQuery = ""
                                                    }
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
                                            text = note.title.ifEmpty { stringResource(R.string.untitled) },
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