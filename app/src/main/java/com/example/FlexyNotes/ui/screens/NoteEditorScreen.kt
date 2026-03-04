package com.example.FlexyNotes.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.FlexyNotes.data.NoteEntity
import com.example.FlexyNotes.viewmodel.NotesViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChecklistItemState(
    initialText: String,
    initialChecked: Boolean
) {
    val id: String = UUID.randomUUID().toString()
    var text by mutableStateOf(initialText)
    var isChecked by mutableStateOf(initialChecked)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: Long?,
    isChecklist: Boolean,
    showTimestamp: Boolean,
    useHaptics: Boolean,
    onNavigateBack: () -> Unit
) {
    val titleState = rememberTextFieldState()
    val contentState = rememberTextFieldState()

    val checklistItems = remember { mutableStateListOf<ChecklistItemState>() }

    val checklistFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var itemToFocus by remember { mutableStateOf<String?>(null) }

    var existingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val contentFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val actualIsChecklist = existingNote?.isChecklist ?: isChecklist

    LaunchedEffect(itemToFocus) {
        itemToFocus?.let { id ->
            delay(50)
            checklistFocusRequesters[id]?.requestFocus()
            itemToFocus = null
        }
    }

    LaunchedEffect(noteId) {
        if (noteId == null) {
            delay(100)

            if (!actualIsChecklist) {
                try {
                    contentFocusRequester.requestFocus()
                } catch (_e: Exception) {}
            }

            if (isChecklist && checklistItems.isEmpty()) {
                val initialItem = ChecklistItemState("", false)
                checklistItems.add(initialItem)
                itemToFocus = initialItem.id
            }
        } else {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                existingNote = note
                titleState.setTextAndPlaceCursorAtEnd(note.title)

                if (note.isChecklist) {
                    checklistItems.clear()
                    if (note.content.isNotBlank()) {
                        val parsedItems = note.content.lines().map { line ->
                            if (line.startsWith("[x] ")) {
                                ChecklistItemState(line.removePrefix("[x] "), true)
                            } else if (line.startsWith("[ ] ")) {
                                ChecklistItemState(line.removePrefix("[ ] "), false)
                            } else {
                                ChecklistItemState(line, false)
                            }
                        }
                        checklistItems.addAll(parsedItems)
                    }
                } else {
                    contentState.setTextAndPlaceCursorAtEnd(note.content)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    if (existingNote != null) {
                        IconButton(onClick = {
                            if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                val shareText = buildString {
                                    if (existingNote!!.title.isNotBlank()) appendLine(existingNote!!.title)
                                    if (existingNote!!.title.isNotBlank() && existingNote!!.content.isNotBlank()) appendLine()

                                    // Replace brackets with nice symbols for sharing
                                    val formattedContent = if (existingNote!!.isChecklist) {
                                        existingNote!!.content.replace("[ ] ", "☐ ").replace("[x] ", "☑ ")
                                    } else {
                                        existingNote!!.content
                                    }

                                    if (formattedContent.isNotBlank()) append(formattedContent)
                                }
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share note via...")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share note")
                        }

                        IconButton(onClick = {
                            if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.archiveNote(existingNote!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive note")
                        }

                        IconButton(onClick = {
                            if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.moveToTrash(existingNote!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Move to trash")
                        }
                    }

                    Button(
                        onClick = {
                            if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            val titleText = titleState.text.toString()

                            val contentText = if (actualIsChecklist) {
                                checklistItems
                                    .filter { it.text.isNotBlank() || it.isChecked }
                                    .joinToString("\n") { if (it.isChecked) "[x] ${it.text}" else "[ ] ${it.text}" }
                            } else {
                                contentState.text.toString()
                            }

                            if (titleText.isNotBlank() || contentText.isNotBlank()) {
                                if (existingNote != null) {
                                    viewModel.updateNote(existingNote!!, titleText, contentText)
                                } else {
                                    viewModel.addNote(titleText, contentText, actualIsChecklist)
                                }
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (!actualIsChecklist) {
                        contentFocusRequester.requestFocus()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            BasicTextField(
                state = titleState,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine,
                decorator = { innerTextField ->
                    Box {
                        if (titleState.text.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (showTimestamp && existingNote != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
                val createdStr = remember(existingNote?.createdAt) { existingNote?.createdAt?.let { dateFormat.format(Date(it)) } ?: "" }
                val editedStr = remember(existingNote?.modifiedAt) { existingNote?.modifiedAt?.let { dateFormat.format(Date(it)) } ?: "" }

                Text(
                    text = "Created: $createdStr   Edited: $editedStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (actualIsChecklist) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    checklistItems.forEachIndexed { index, item ->
                        val itemFocusRequester = remember(item.id) { FocusRequester() }
                        checklistFocusRequesters[item.id] = itemFocusRequester

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = {
                                    item.isChecked = it
                                    if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )

                            BasicTextField(
                                value = item.text,
                                onValueChange = { newValue ->
                                    if (newValue.contains('\n')) {
                                        val parts = newValue.split('\n', limit = 2)
                                        item.text = parts[0]
                                        val newItem = ChecklistItemState(parts.getOrNull(1) ?: "", false)
                                        checklistItems.add(index + 1, newItem)
                                        itemToFocus = newItem.id
                                    } else {
                                        item.text = newValue
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .focusRequester(itemFocusRequester)
                            )

                            IconButton(onClick = { checklistItems.removeAt(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove item",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            val newItem = ChecklistItemState("", false)
                            checklistItems.add(newItem)
                            itemToFocus = newItem.id
                            if (useHaptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Item")
                    }
                }
            } else {
                BasicTextField(
                    state = contentState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 400.dp)
                        .focusRequester(contentFocusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorator = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (contentState.text.isEmpty()) {
                                Text(
                                    "Note",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}