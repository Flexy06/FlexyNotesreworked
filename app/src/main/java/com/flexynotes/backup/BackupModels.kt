package com.flexynotes.data.backup

import kotlinx.serialization.Serializable
import java.util.UUID

// Wrapper to hold all notes, tombstones and metadata for version control
@Serializable
data class BackupContainer(
    val version: Int,
    val timestamp: Long,
    val notes: List<BackupNote>,
    val tombstones: List<BackupTombstone> = emptyList() // Default for backwards compatibility
)

@Serializable
data class BackupNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val createdAt: Long,
    val modifiedAt: Long,
    val colorArgb: Int? = null,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
    val isChecklist: Boolean,
    val reminderTime: Long?
)

@Serializable
data class BackupTombstone(
    val noteId: String,
    val deletedAt: Long
)