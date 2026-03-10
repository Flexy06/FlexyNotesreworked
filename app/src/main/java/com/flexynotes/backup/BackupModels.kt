package com.flexynotes.data.backup

import kotlinx.serialization.Serializable

// Wrapper to hold all notes and metadata for version control
@Serializable
data class BackupContainer(
    val version: Int,
    val timestamp: Long,
    val notes: List<BackupNote>
)

@Serializable
data class BackupNote(
    val title: String,
    val content: String,
    val isChecklist: Boolean,
    val reminderTime: Long?,
    val createdAt: Long,
    val modifiedAt: Long
)