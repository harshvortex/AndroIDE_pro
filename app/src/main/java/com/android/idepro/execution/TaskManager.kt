package com.android.idepro.execution

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

object TaskManager {
    private val gson = Gson()

    fun loadTasks(context: Context, projectDir: DocumentFile): List<Task> {
        val tasksFile = projectDir.findFile("tasks.json") ?: return getDefaultTasks()
        return try {
            context.contentResolver.openInputStream(tasksFile.uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val type = object : TypeToken<List<Task>>() {}.type
                gson.fromJson(reader, type)
            } ?: getDefaultTasks()
        } catch (e: Exception) {
            getDefaultTasks()
        }
    }

    private fun getDefaultTasks(): List<Task> {
        return listOf(
            Task("Build", "./gradlew assembleDebug", "Build the current Android project"),
            Task("Run Python", "python main.py", "Execute the main Python script"),
            Task("Run Node", "node index.js", "Execute the main JavaScript file"),
            Task("List Files", "ls -R", "List all files in project")
        )
    }
}
