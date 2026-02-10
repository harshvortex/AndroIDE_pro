package com.android.idepro.execution

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class Task(
    val name: String,
    val command: String,
    val description: String = ""
)

object ExecutionEngine {
    private var activeProcess: Process? = null

    suspend fun runTask(task: Task, workingDir: File, onOutput: (String) -> Unit): Int = withContext(Dispatchers.IO) {
        try {
            activeProcess?.destroy()
            
            val builder = ProcessBuilder(*task.command.split(" ").toTypedArray())
                .directory(workingDir)
                .redirectErrorStream(true)

            val process = builder.start()
            activeProcess = process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                onOutput(line + "\n")
            }

            val exitCode = process.waitFor()
            activeProcess = null
            exitCode
        } catch (e: Exception) {
            onOutput("Execution Error: ${e.localizedMessage}\n")
            -1
        }
    }

    fun stopActiveTask() {
        activeProcess?.destroy()
        activeProcess = null
    }
}
