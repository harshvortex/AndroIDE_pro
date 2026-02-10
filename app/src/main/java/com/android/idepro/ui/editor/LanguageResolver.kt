package com.android.idepro.ui.editor

import com.android.idepro.model.ProjectFile

/**
 * Central service for resolving programming languages based on file metadata.
 */
object LanguageResolver {
    
    fun resolve(file: ProjectFile): LanguageConfig {
        val extension = file.extension.lowercase()
        return LanguageManager.getLanguageForExtension(extension) 
            ?: LanguageManager.getKotlinLanguage() // Default fallback
    }

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "kt", "kts" -> "text/x-kotlin"
            "java" -> "text/x-java"
            "py" -> "text/x-python"
            "js", "jsx" -> "text/javascript"
            "ts", "tsx" -> "text/typescript"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "sh", "bash" -> "text/x-sh"
            "md" -> "text/markdown"
            else -> "text/plain"
        }
    }
}
