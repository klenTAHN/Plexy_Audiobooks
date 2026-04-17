package com.klentahn.plexyaudiobooks.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE libraryKey = :libraryKey ORDER BY author ASC, titleSort ASC")
    fun getBooksByAuthor(libraryKey: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE libraryKey = :libraryKey ORDER BY titleSort ASC")
    fun getBooksByTitle(libraryKey: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE author = :author AND libraryKey = :libraryKey ORDER BY year ASC, titleSort ASC")
    fun getBooksByAuthorFiltered(author: String, libraryKey: String): Flow<List<BookEntity>>

    @Query("SELECT DISTINCT author FROM books WHERE libraryKey = :libraryKey ORDER BY author ASC")
    fun getAuthors(libraryKey: String): Flow<List<String>>

    @Query("SELECT * FROM books WHERE libraryKey = :libraryKey")
    suspend fun getBooksList(libraryKey: String): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE libraryKey = :libraryKey")
    suspend fun deleteBooksByLibrary(libraryKey: String)
}
