package com.android.idepro.model

import androidx.documentfile.provider.DocumentFile

/**
 * Foundation model for file handling in AndroidIDE Pro.
 * Decouples the IDE from Android's DocumentFile and MIME types.
 */
data class ProjectFile(
    val document: DocumentFile,
    val name: String = document.name ?: "unknown",
    val extension: String = name.substringAfterLast(".", "")
) {
    val isDirectory: Boolean get() = document.isDirectory
}
