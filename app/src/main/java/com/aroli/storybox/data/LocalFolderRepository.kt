package com.aroli.storybox.data

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "ogg", "m4a", "wav")
private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png")

/**
 * Lists audio files from a single SAF folder tree picked once by the parent.
 * Cover art priority: embedded ID3 picture, else same-name sibling image, else filename fallback (imageUri = null).
 */
class LocalFolderRepository(
    private val context: Context,
    private val folderUri: Uri,
) : StoryRepository {

    override suspend fun list(): List<StoryItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val files = root.listFiles().toList()
        val audioFiles = files.filter { it.isFile && it.extension() in SUPPORTED_AUDIO_EXTENSIONS }
            .sortedBy { it.name }

        audioFiles.map { file ->
            val embeddedArt = extractEmbeddedArt(file.uri)
            val imageUri = embeddedArt ?: findSiblingImage(files, file)?.uri
            StoryItem(
                id = file.uri.toString(),
                title = file.name?.substringBeforeLast('.') ?: "Untitled",
                audioUri = file.uri.toString(),
                imageUri = imageUri?.toString(),
            )
        }
    }

    override suspend fun resolvePlayableUri(item: StoryItem): String = item.audioUri

    private fun findSiblingImage(files: List<DocumentFile>, audioFile: DocumentFile): DocumentFile? {
        val baseName = audioFile.name?.substringBeforeLast('.') ?: return null
        return files.firstOrNull {
            it.isFile && it.extension() in SUPPORTED_IMAGE_EXTENSIONS && it.name?.substringBeforeLast('.') == baseName
        }
    }

    private fun extractEmbeddedArt(audioUri: Uri): Uri? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            val art = retriever.embeddedPicture ?: return null
            val cacheFile = File(context.cacheDir, "art_${audioUri.toString().hashCode()}.jpg")
            cacheFile.writeBytes(art)
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun DocumentFile.extension(): String = name?.substringAfterLast('.', "")?.lowercase() ?: ""
}
