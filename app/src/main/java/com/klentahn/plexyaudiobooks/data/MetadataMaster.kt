package com.klentahn.plexyaudiobooks.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.klentahn.plexyaudiobooks.ui.screens.player.Chapter

class MetadataMaster(private val context: Context) {

    fun getChapters(uri: Uri): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            // Note: MediaMetadataRetriever doesn't support chapter extraction for all formats.
            // This is a placeholder for where you'd use a library like FFmpeg or an MP4 parser.
            // For now, let's keep it empty or implement a basic check.
        } catch (e: Exception) {
            Log.e("MetadataMaster", "Error extracting chapters from $uri", e)
        } finally {
            retriever.release()
        }
        return chapters
    }

    fun getMetadata(uri: Uri): Map<String, String> {
        val retriever = MediaMetadataRetriever()
        val metadata = mutableMapOf<String, String>()
        try {
            retriever.setDataSource(context, uri)
            metadata["title"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            metadata["artist"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            metadata["album"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            metadata["duration"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: ""
        } catch (e: Exception) {
            Log.e("MetadataMaster", "Error extracting metadata from $uri", e)
        } finally {
            retriever.release()
        }
        return metadata
    }

    fun getEmbeddedArt(uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            return retriever.embeddedPicture
        } catch (e: Exception) {
            Log.e("MetadataMaster", "Error extracting embedded art from $uri", e)
        } finally {
            retriever.release()
        }
        return null
    }
}
