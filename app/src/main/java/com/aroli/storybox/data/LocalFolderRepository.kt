package com.aroli.storybox.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "ogg", "m4a", "wav")
private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png")

/** One row of the batched SAF children query - avoids per-file ContentResolver round-trips. */
private data class DocEntry(val documentId: String, val name: String, val isDirectory: Boolean)

/**
 * Lists audio files from a single SAF folder tree picked once by the parent.
 * Supports recursive folder navigation: folders with audio/subfolders shown with isFolder=true.
 * Cover art: looks for same-name sibling image file (instant, no ID3 extraction which is slow).
 */
class LocalFolderRepository(
    private val context: Context,
    private val folderUri: Uri,
) : StoryRepository {

    /** Current position in the folder tree. Starts at root, updated by navigateInto/navigateBack. */
    private var currentFolderUri: Uri = folderUri

    /** Navigation history to support navigateBack. */
    private val navigationHistory = mutableListOf<Uri>()

    override suspend fun list(): List<StoryItem> = withContext(Dispatchers.IO) {
        // Single batched query for all children's name/mime-type/id - each DocumentFile property
        // access (isFile/name/isDirectory) is otherwise a separate IPC round-trip to the SAF
        // provider, which was the real bottleneck (100s of calls for a 23-file folder).
        val parentDocumentId = if (DocumentsContract.isDocumentUri(context, currentFolderUri)) {
            DocumentsContract.getDocumentId(currentFolderUri)
        } else {
            DocumentsContract.getTreeDocumentId(currentFolderUri)
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(currentFolderUri, parentDocumentId)

        val entries = mutableListOf<DocEntry>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null, null, null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                entries.add(
                    DocEntry(
                        documentId = cursor.getString(idIdx),
                        name = cursor.getString(nameIdx) ?: "",
                        isDirectory = cursor.getString(mimeIdx) == DocumentsContract.Document.MIME_TYPE_DIR,
                    )
                )
            }
        }

        fun uriFor(documentId: String) = DocumentsContract.buildDocumentUriUsingTree(currentFolderUri, documentId)
        fun extensionOf(name: String) = name.substringAfterLast('.', "").lowercase()

        val result = mutableListOf<StoryItem>()

        // List audio files (direct playables)
        val audioEntries = entries.filter { !it.isDirectory && extensionOf(it.name) in SUPPORTED_AUDIO_EXTENSIONS }
            .sortedBy { it.name }
        audioEntries.forEach { entry ->
            val baseName = entry.name.substringBeforeLast('.')
            val sibling = entries.firstOrNull {
                !it.isDirectory && extensionOf(it.name) in SUPPORTED_IMAGE_EXTENSIONS && it.name.substringBeforeLast('.') == baseName
            }
            result.add(
                StoryItem(
                    id = uriFor(entry.documentId).toString(),
                    title = baseName,
                    audioUri = uriFor(entry.documentId).toString(),
                    imageUri = sibling?.let { uriFor(it.documentId).toString() },
                    isFolder = false,
                )
            )
        }

        // List all folders (no recursive check - instant display for child)
        val folderEntries = entries.filter { it.isDirectory }.sortedBy { it.name }
        folderEntries.forEach { entry ->
            result.add(
                StoryItem(
                    id = uriFor(entry.documentId).toString(),
                    title = entry.name,
                    audioUri = "",  // Folders don't have audio
                    imageUri = null,
                    isFolder = true,
                )
            )
        }

        result
    }

    /** Enter a folder. Expects a StoryItem with isFolder=true. */
    suspend fun navigateInto(folder: StoryItem) {
        if (!folder.isFolder) return
        navigationHistory.add(currentFolderUri)
        currentFolderUri = Uri.parse(folder.id)
    }

    /** Exit current folder and go back to parent. */
    suspend fun navigateBack() {
        if (navigationHistory.isNotEmpty()) {
            currentFolderUri = navigationHistory.removeAt(navigationHistory.size - 1)
        }
    }

    /** True if we are inside a subfolder (not at root). */
    fun canNavigateBack(): Boolean = navigationHistory.isNotEmpty()

    override suspend fun resolvePlayableUri(item: StoryItem): String = item.audioUri
}
