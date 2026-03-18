package com.flexynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    // Generates a unique string ID automatically for every new note
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val createdAt: Long,
    val modifiedAt: Long,
    val colorArgb: Int? = null,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
    val isChecklist: Boolean = false,
    val reminderTime: Long? = null,
    val colorIndex: Int? = null

)