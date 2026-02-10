package com.android.idepro.ui.editor

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

data class LanguageConfig(
    val name: String,
    val extensions: List<String>,
    val rules: List<SyntaxRule>
)

data class SyntaxRule(
    val pattern: String,
    val token: String
)

object LanguageManager {
    private val gson = Gson()
    private val languages = mutableListOf<LanguageConfig>()

    fun loadLanguages(context: Context) {
        try {
            val assetManager = context.assets
            val langFiles = assetManager.list("languages") ?: emptyArray()
            languages.clear()
            for (file in langFiles) {
                if (file.endsWith(".json")) {
                    val inputStream = assetManager.open("languages/$file")
                    val reader = InputStreamReader(inputStream)
                    val config = gson.fromJson(reader, LanguageConfig::class.java)
                    languages.add(config)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLanguageForExtension(extension: String): LanguageConfig? {
        return languages.find { it.extensions.contains(extension.lowercase()) }
    }

    fun getKotlinLanguage(): LanguageConfig {
        return languages.find { it.name == "Kotlin" } ?: LanguageConfig(
            name = "Kotlin",
            extensions = listOf("kt"),
            rules = emptyList()
        )
    }
}
