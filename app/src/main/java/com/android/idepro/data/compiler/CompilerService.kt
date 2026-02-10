package com.android.idepro.data.compiler

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CompilerService {

    fun executeCommand(command: String, workingDir: File? = null): String {
        return try {
            val process = Runtime.getRuntime().exec(command, null, workingDir)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append("Error: ").append(line).append("\n")
            }
            
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            "Execution failed: ${e.localizedMessage}"
        }
    }
}
