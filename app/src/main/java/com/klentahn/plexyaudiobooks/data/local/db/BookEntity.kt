package com.klentahn.plexyaudiobooks.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val ratingKey: String,
    val title: String,
    val titleSort: String,
    val author: String,
    val summary: String?,
    val thumb: String?,
    val art: String?,
    val duration: Long?,
    val year: Int?,
    val addedAt: Long?,
    val updatedAt: Long?,
    val libraryKey: String,
    val streamUrl: String? = null
)
