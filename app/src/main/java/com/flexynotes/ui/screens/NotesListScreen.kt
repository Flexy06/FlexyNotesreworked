package com.flexynotes.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flexynotes.app.R
import com.flexynotes.viewmodel.NotesViewModel
import com.flexynotes.worker.SyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    isGridView: Boolean,
    useHaptics: Boolean,
    isOledMode: Boolean = false,
    isAnySyncEnabled: Boolean = false,
    onGridViewToggle: () -> Unit,
    onNavigateToEditor: (String?, Boolean) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val notes by viewModel.activeNotes.collectAsStateWithLifecycle()
    var selectedNoteIds by remember { mutableStateOf(setOf<String>()) }
    var showFabMenu by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val minDragDistance = with(density) { 100.dp.toPx() }

    // Pull to Refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isSearching,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                                    (fadeOut() + scaleOut(targetScale = 0.95f))
                        },
                        label = "search_transition"
                    ) { targetIsSearching ->
                        if (targetIsSearching) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 4.dp
                            ) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(stringResource(R.string.search_notes)) },
                                    singleLine = true,
                                    leadingIcon = {
                                        IconButton(onClick = {
                                            isSearching = false
                                            searchQuery = ""
                                        }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                                        }
                                    },
                                    trailingIcon = {
                                        AnimatedVisibility(
                                            visible = searchQuery.isNotEmpty(),
                                            enter = fadeIn() + scaleIn(),
                                            exit = fadeOut() + scaleOut()
                                        ) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear text")
                                            }
                                        }
                                    },
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
                            }
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
                                    // Removed the static sync button from here
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedNoteIds.isEmpty() && !isSearching) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.8f,
                            transformOrigin = TransformOrigin(1f, 1f)
                        ) + slideInVertically(initialOffsetY = { 50 }),
                        exit = fadeOut() + scaleOut(
                            targetScale = 0.8f,
                            transformOrigin = TransformOrigin(1f, 1f)
                        ) + slideOutVertically(targetOffsetY = { 50 })
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
                        containerColor = if (showFabMenu) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (showFabMenu) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (showFabMenu) 45f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "fab_rotate"
                        )
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

            val contentState = when {
                notes.isEmpty() -> "EMPTY"
                displayedNotes.isEmpty() && isSearching -> "NO_RESULTS"
                else -> "CONTENT"
            }

            Crossfade(
                targetState = contentState,
                label = "content_crossfade",
                animationSpec = tween(durationMillis = 300),
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    "EMPTY" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.notes_empty))
                        }
                    }
                    "NO_RESULTS" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_search_results, searchQuery))
                        }
                    }
                    "CONTENT" -> {
                        // NEW: PullToRefreshBox wrapping the LazyGrid
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                if (isAnySyncEnabled) {
                                    SyncManager.triggerImmediateDownload(context)
                                    SyncManager.triggerImmediateUpload(context)
                                    Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No cloud sync configured", Toast.LENGTH_SHORT).show()
                                }

                                coroutineScope.launch {
                                    delay(1500)
                                    isRefreshing = false
                                }
                            },
                            state = pullRefreshState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp
                            ) {
                                items(displayedNotes, key = { it.id }) { note ->
                                    val isSelected = selectedNoteIds.contains(note.id)

                                    val stateHolder = remember { arrayOfNulls<SwipeToDismissBoxState>(1) }

                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            when (dismissValue) {
                                                SwipeToDismissBoxValue.StartToEnd -> {
                                                    val offset = try { stateHolder[0]?.requireOffset() ?: 0f } catch (e: Exception) { 0f }
                                                    if (Math.abs(offset) < minDragDistance) return@rememberSwipeToDismissBoxState false

                                                    viewModel.archiveNote(note)
                                                    true
                                                }
                                                SwipeToDismissBoxValue.EndToStart -> {
                                                    val offset = try { stateHolder[0]?.requireOffset() ?: 0f } catch (e: Exception) { 0f }
                                                    if (Math.abs(offset) < minDragDistance) return@rememberSwipeToDismissBoxState false

                                                    viewModel.moveToTrash(note)
                                                    true
                                                }
                                                else -> false
                                            }
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
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "resistance"
                                    )

                                    val isSwiping = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled ||
                                            dismissState.targetValue != SwipeToDismissBoxValue.Settled


                                    val noteCardColors = listOf(
                                        Color(0xFFE8F5E9),
                                        Color(0xFFFFF8E1),
                                        Color(0xFFE3F2FD),
                                        Color(0xFFFCE4EC),
                                        Color(0xFFEDE7F6),
                                        Color(0xFFE0F7FA),
                                        Color(0xFFFFF3E0),
                                        Color(0xFFF3E5F5),
                                    )
                                    val noteCardColorsDark = listOf(
                                        Color(0xFF1B2E1E),
                                        Color(0xFF2E2A14),
                                        Color(0xFF152130),
                                        Color(0xFF2E1820),
                                        Color(0xFF1E1A2E),
                                        Color(0xFF122428),
                                        Color(0xFF2E2418),
                                        Color(0xFF271A2E),
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
                                        modifier = Modifier
                                            .animateItem()
                                            .zIndex(if (isSwiping) 1f else 0f),
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
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        text = note.title.ifEmpty { stringResource(R.string.untitled) },
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
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
                                                                        imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,                                                                        contentDescription = null,
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
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showFabMenu = false }
                )
            }
        }
    }
}