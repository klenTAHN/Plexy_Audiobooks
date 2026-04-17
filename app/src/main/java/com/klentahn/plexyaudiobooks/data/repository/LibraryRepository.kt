package com.klentahn.plexyaudiobooks.data.repository

import android.util.Log
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.local.db.BookDao
import com.klentahn.plexyaudiobooks.data.local.db.BookEntity
import com.klentahn.plexyaudiobooks.data.remote.PlexApi
import com.klentahn.plexyaudiobooks.data.MetadataMaster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LibraryRepository(
    private val plexApi: PlexApi,
    private val bookDao: BookDao,
    private val settingsManager: SettingsManager,
    private val metadataMaster: MetadataMaster
) {
    private val TAG = "LibraryRepository"

    private suspend fun resolveThumbRecursive(
        metadata: com.klentahn.plexyaudiobooks.data.model.PlexMetadata,
        serverUri: String,
        token: String,
        depth: Int = 0
    ): String? {
        if (depth > 3) return null
        
        // Priority: 
        // 1. Specific item thumb (if it's an album/book)
        // 2. Parent thumb (usually the album/book cover if item is a track)
        // 3. ONLY THEN grandparent thumb (author photo) - we want to avoid this if possible
        
        val thumb = if (metadata.type == "album") {
            metadata.thumb.takeIf { !it.isNullOrBlank() }
                ?: metadata.parentThumb.takeIf { !it.isNullOrBlank() }
        } else if (metadata.type == "track") {
            metadata.parentThumb.takeIf { !it.isNullOrBlank() }
                ?: metadata.thumb.takeIf { !it.isNullOrBlank() }
        } else {
            metadata.thumb.takeIf { !it.isNullOrBlank() }
                ?: metadata.parentThumb.takeIf { !it.isNullOrBlank() }
        }

        if (thumb != null) return thumb
        
        // If we still don't have a thumb, and we have a grandparent thumb, 
        // use it as a last resort before recursion.
        metadata.grandparentThumb.takeIf { !it.isNullOrBlank() }?.let { return it }
        
        if (metadata.parentRatingKey != null) {
            try {
                val url = "$serverUri/library/metadata/${metadata.parentRatingKey}?includeExternalMedia=1&includeExtras=1"
                val response = plexApi.getMetadata(url, token)
                if (response.isSuccessful) {
                    val parentMetadata = response.body()?.mediaContainer?.metadata?.firstOrNull()
                    if (parentMetadata != null) {
                        return resolveThumbRecursive(parentMetadata, serverUri, token, depth + 1)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in resolveThumbRecursive at depth $depth", e)
            }
        }
        return null
    }

    fun getBooksByAuthor(libraryKey: String): Flow<List<BookEntity>> = 
        bookDao.getBooksByAuthor(libraryKey)

    fun getBooksByTitle(libraryKey: String): Flow<List<BookEntity>> = 
        bookDao.getBooksByTitle(libraryKey)

    fun getAuthors(libraryKey: String): Flow<List<String>> =
        bookDao.getAuthors(libraryKey)

    fun getBooksByAuthorFiltered(author: String, libraryKey: String): Flow<List<BookEntity>> = 
        bookDao.getBooksByAuthorFiltered(author, libraryKey)

    suspend fun refreshLibrary() {
        Log.d(TAG, "Starting library refresh")
        val token = settingsManager.authToken.first() ?: return
        val serverUri = settingsManager.serverUri.first() ?: return
        val libraryKey = settingsManager.libraryKey.first() ?: return

        var offset = 0
        val pageSize = 100
        var totalSize = -1
        val allBookEntities = mutableListOf<BookEntity>()

        try {
            val localBooks = bookDao.getBooksList(libraryKey).associateBy { it.ratingKey }
            
            while (totalSize == -1 || offset < totalSize) {
                // Added includeExternalMedia and includeExtras to get better thumb discovery
                val url = "$serverUri/library/sections/$libraryKey/all?includeExternalMedia=1&includeExtras=1"
                Log.d(TAG, "Fetching library contents (offset: $offset, size: $pageSize)")
                
                val response = plexApi.getLibraryContents(
                    url = url,
                    token = token,
                    type = 9, // Fetch albums (books)
                    start = offset,
                    size = pageSize
                )

                if (!response.isSuccessful) {
                    Log.e(TAG, "Error fetching library page: ${response.code()} ${response.message()}")
                    break
                }

                val mediaContainer = response.body()?.mediaContainer ?: run {
                    Log.w(TAG, "Empty body in response")
                    break
                }

                if (totalSize == -1) {
                    // Try to find total size in various possible fields
                    totalSize = mediaContainer.totalSize ?: mediaContainer.size
                    Log.d(TAG, "Library reports size: ${mediaContainer.size}, totalSize: ${mediaContainer.totalSize}")
                }

                val metadataList = mediaContainer.metadata ?: emptyList()
                val directoryList = mediaContainer.directories ?: emptyList()
                
                Log.d(TAG, "Received ${metadataList.size} metadata items and ${directoryList.size} directories")
                
                if (metadataList.isEmpty() && directoryList.isEmpty()) {
                    Log.d(TAG, "No more items found")
                    break
                }

                // Log all types we see to help debug missing items
                metadataList.forEach { 
                    Log.v(TAG, "Item: title='${it.title}', type='${it.type}', ratingKey='${it.ratingKey}'")
                }

                val pageEntities = metadataList.filter { 
                    // Accept albums, but also tracks or other types if they have necessary info
                    it.type == "album" || it.type == "track" || it.type == "audio"
                }.map { metadata ->
                    val existingBook = localBooks[metadata.ratingKey]
                    val resolvedThumb = resolveThumbRecursive(metadata, serverUri, token) ?: existingBook?.thumb
                    
                    val mediaPart = metadata.media?.firstOrNull()?.parts?.firstOrNull()
                    val streamUrl = mediaPart?.let { "$serverUri${it.key}?X-Plex-Token=$token" }

                    Log.d(TAG, "Library item: title='${metadata.title}', type='${metadata.type}', resolvedThumb='$resolvedThumb', streamUrl='$streamUrl'")
                    if (resolvedThumb == null) {
                        Log.w(TAG, "  -> No thumb found for '${metadata.title}'.")
                    }

                    BookEntity(
                        ratingKey = metadata.ratingKey,
                        title = metadata.title,
                        titleSort = metadata.titleSort ?: metadata.title,
                        author = metadata.grandparentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" } 
                            ?: metadata.parentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" } 
                            ?: metadata.grandparentTitle.takeIf { !it.isNullOrBlank() }
                            ?: metadata.parentTitle.takeIf { !it.isNullOrBlank() }
                            ?: existingBook?.author
                            ?: "Unknown Author",
                        summary = metadata.summary ?: existingBook?.summary,
                        thumb = resolvedThumb,
                        art = metadata.art ?: existingBook?.art,
                        duration = metadata.duration ?: existingBook?.duration,
                        year = metadata.year ?: existingBook?.year,
                        addedAt = metadata.addedAt ?: existingBook?.addedAt,
                        updatedAt = metadata.updatedAt ?: existingBook?.updatedAt,
                        libraryKey = libraryKey,
                        streamUrl = streamUrl ?: existingBook?.streamUrl
                    )
                }
                
                allBookEntities.addAll(pageEntities)
                offset += metadataList.size
                
                // If we got fewer than requested, we've likely reached the end
                if (metadataList.size < pageSize) {
                    break
                }
                
                if (totalSize != -1 && offset >= totalSize) {
                    break
                }
            }

            if (allBookEntities.isNotEmpty()) {
                bookDao.insertBooks(allBookEntities)
                Log.d(TAG, "Successfully updated ${allBookEntities.size} books in database")
            } else {
                Log.w(TAG, "No books found during refresh")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during library refresh", e)
        }
    }

    suspend fun refreshMetadata() {
        Log.d(TAG, "Starting deep metadata refresh")
        val token = settingsManager.authToken.first() ?: return
        val serverUri = settingsManager.serverUri.first() ?: return
        val libraryKey = settingsManager.libraryKey.first() ?: return

        try {
            val localBooks = bookDao.getBooksList(libraryKey)
            Log.d(TAG, "Refreshing metadata for ${localBooks.size} local books")

            localBooks.forEach { book ->
                val url = "$serverUri/library/metadata/${book.ratingKey}?includeExternalMedia=1&includeExtras=1"
                val response = plexApi.getMetadata(url, token)

                if (response.isSuccessful) {
                    val metadata = response.body()?.mediaContainer?.metadata?.firstOrNull()
                    if (metadata != null) {
                        val resolvedThumb = resolveThumbRecursive(metadata, serverUri, token)
                        
                        val mediaPart = metadata.media?.firstOrNull()?.parts?.firstOrNull()
                        val streamUrl = mediaPart?.let { "$serverUri${it.key}?X-Plex-Token=$token" }

                        Log.d(TAG, "Metadata update for '${metadata.title}': resolvedThumb='$resolvedThumb', streamUrl='$streamUrl'")
                        if (resolvedThumb == null) {
                            Log.w(TAG, "  -> Still no thumb after metadata refresh and recursive fallback for '${metadata.title}'")
                        }
                        
                        val updatedBook = book.copy(
                            title = metadata.title,
                            titleSort = metadata.titleSort ?: metadata.title,
                            author = metadata.grandparentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" } 
                                ?: metadata.parentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" }
                                ?: metadata.grandparentTitle.takeIf { !it.isNullOrBlank() }
                                ?: metadata.parentTitle.takeIf { !it.isNullOrBlank() }
                                ?: book.author,
                            summary = metadata.summary ?: book.summary,
                            thumb = resolvedThumb ?: book.thumb,
                            art = metadata.art ?: book.art,
                            duration = metadata.duration ?: book.duration,
                            year = metadata.year ?: book.year,
                            updatedAt = metadata.updatedAt ?: book.updatedAt,
                            streamUrl = streamUrl ?: book.streamUrl
                        )
                        bookDao.insertBooks(listOf(updatedBook))
                    }
                }
else {
                    Log.e(TAG, "Failed to fetch metadata for ${book.title}: ${response.code()}")
                }
            }
            Log.d(TAG, "Deep metadata refresh complete")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during metadata refresh", e)
        }
    }
}
