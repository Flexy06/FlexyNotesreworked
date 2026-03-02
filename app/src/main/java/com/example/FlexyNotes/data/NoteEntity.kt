package com.example.FlexyNotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val createdAt: Long,
    val modifiedAt: Long,
    val colorArgb: Int? = null,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false
)