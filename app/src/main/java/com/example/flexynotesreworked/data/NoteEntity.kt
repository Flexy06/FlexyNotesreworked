package com.example.flexynotesreworked.data

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
    // Soft delete flag for the trash bin functionality
    val isDeleted: Boolean = false
)