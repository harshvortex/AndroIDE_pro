package com.android.idepro.ui.theme

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import java.io.InputStreamReader

data class ThemeConfig(
    val name: String,
    val isDark: Boolean,
    val colors: ColorConfig
)

data class ColorConfig(
    val ui: UIColors,
    val syntax: SyntaxColors
)

data class UIColors(
    val background: String,
    val sidebar: String,
    val activityBar: String,
    val accent: String,
    val text: String,
    val selection: String
)

data class SyntaxColors(
    val keyword: String,
    val type: String,
    val string: String,
    val comment: String,
    val number: String,
    val annotation: String,
    val literal: String,
    val tag: String,
    val attribute: String,
    val error: String
)

object ThemeManager {
    private val gson = Gson()
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "current_theme"

    var currentTheme by mutableStateOf(getDefaultTheme())
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getString(KEY_THEME, "VS Code Dark") ?: "VS Code Dark"
        loadTheme(context, savedTheme)
    }

    fun loadTheme(context: Context, themeName: String) {
        try {
            val fileName = themeName.replace(" ", "") + ".json"
            val assetManager = context.assets
            val inputStream = assetManager.open("themes/$fileName")
            val reader = InputStreamReader(inputStream)
            currentTheme = gson.fromJson(reader, ThemeConfig::class.java)
            
            // Persist choice
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, themeName)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            if (themeName != "VS Code Dark") {
                loadTheme(context, "VS Code Dark")
            }
        }
    }

    fun getAvailableThemes(context: Context): List<String> {
        return listOf("VS Code Dark", "Light Clean", "Solarized Dark", "High Contrast")
    }

    private fun getDefaultTheme(): ThemeConfig {
        return ThemeConfig(
            name = "VS Code Dark",
            isDark = true,
            colors = ColorConfig(
                ui = UIColors(
                    background = "#1E1E1E",
                    sidebar = "#252526",
                    activityBar = "#333333",
                    accent = "#007ACC",
                    text = "#CCCCCC",
                    selection = "#264F78"
                ),
                syntax = SyntaxColors(
                    keyword = "#569CD6",
                    type = "#4EC9B0",
                    string = "#CE9178",
                    comment = "#6A9955",
                    number = "#B5CEA8",
                    annotation = "#DCDCAA",
                    literal = "#569CD6",
                    tag = "#569CD6",
                    attribute = "#9CDCFE",
                    error = "#F44336"
                )
            )
        )
    }
}

fun String.toComposeColor(): Color = Color(android.graphics.Color.parseColor(this))
