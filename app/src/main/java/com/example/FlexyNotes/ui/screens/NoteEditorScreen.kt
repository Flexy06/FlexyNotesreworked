package com.example.FlexyNotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.FlexyNotes.data.NoteEntity
import com.example.FlexyNotes.viewmodel.NotesViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: Long?,
    showTimestamp: Boolean,
    onNavigateBack: () -> Unit
) {
    // Utilize TextFieldState for robust typing and scrolling
    val titleState = rememberTextFieldState()
    val contentState = rememberTextFieldState()

    var existingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val contentFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()

    LaunchedEffect(noteId) {
        if (noteId == null) {
            delay(100)
            contentFocusRequester.requestFocus()
        } else {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                existingNote = note
                titleState.setTextAndPlaceCursorAtEnd(note.title)
                contentState.setTextAndPlaceCursorAtEnd(note.content)
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
                            viewModel.archiveNote(existingNote!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive note")
                        }

                        IconButton(onClick = {
                            viewModel.moveToTrash(existingNote!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Move to trash")
                        }
                    }

                    Button(
                        onClick = {
                            val titleText = titleState.text.toString()
                            val contentText = contentState.text.toString()

                            if (titleText.isNotBlank() || contentText.isNotBlank()) {
                                if (existingNote != null) {
                                    viewModel.updateNote(existingNote!!, titleText, contentText)
                                } else {
                                    viewModel.addNote(titleText, contentText)
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
        // Hand over keyboard management to Scaffold
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
                    contentFocusRequester.requestFocus()
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // Title Input
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

            // Dynamic Timestamp Layout based on user preference
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

            // Content Input
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

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}