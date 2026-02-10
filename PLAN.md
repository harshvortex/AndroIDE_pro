# AndroidIDE Pro - Implementation Plan

## 1. Project Overview
**Name**: AndroidIDE Pro
**Target OS**: Android 14+
**Tech Stack**: 
-   **Language**: Kotlin (Required for Jetpack Compose) / Java (Interop where possible)
    -   *Note: Jetpack Compose APIs are exclusively available in Kotlin. The project will use Kotlin for UI and logic to ensure compatibility.*
-   **UI Framework**: Jetpack Compose + Material 3
-   **Architecture**: MVVM (Model-View-ViewModel)

## 2. Core Features & Implementation Strategy

### A. Code Editor (CodeView)
-   **Component**: `CodeEditor` (Composable wrapper around a syntax highlighting view or a custom Compose implementation).
-   **Features**: Syntax highlighting (Java, Python, JS), Line numbers, Auto-indentation.
-   **Lib**: `com.github.AmrDeveloper:CodeView` (wrapped in `AndroidView` for Compose) or a native Compose editor.

### B. Runtime/Compiler (Runtime.exec)
-   **Java**: Use `javax.tools.JavaCompiler` (if bundled) or `Runtime.getRuntime().exec("javac ...")` if a bootstrap compiler is present (e.g., via Termux). *Note: On stock Android, `javac` is not present globally. We will implement a basic "Script" runner for Python/JS via embedded interpreters or assume Termux environment is available.*
-   **Python**: Embed `Chaquopy` or similar, or execute via Termux intents.
-   **Implementation**: `CompilerRepository` handling `ProcessBuilder`.

### C. File System (SAF)
-   **Storage Access Framework**: Use `Intent.ACTION_OPEN_DOCUMENT_TREE` to get permissions.
-   **Explorer UI**: Tree view in Compose showing file hierarchy.

### D. Terminal Emulator (Jackpal/Termux)
-   **Integration**: Embed `Jackpal Android Terminal Emulator` source or use `Termux` Intent API to send commands.
-   **UI**: A bottom sheet or dedicated tab showing the shell output.

### E. GitHub Integration
-   **API**: GitHub REST API via Retrofit.
-   **Auth**: Personal Access Token (PAT).
-   **Features**: Clone repo, Commit, Push (via JGit or API).

## 3. Project Structure
```
AndroidIDE/
├── build.gradle
├── settings.gradle
├── app/
│   ├── build.gradle
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/android/idepro/
│   │   │   │   ├── MainActivity.kt          // Entry point
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/               // Material 3 Theme
│   │   │   │   │   ├── editor/              // Editor Screen
│   │   │   │   │   ├── terminal/            // Terminal Screen
│   │   │   │   │   ├── filemanager/         // File Explorer
│   │   │   │   │   └── home/                // Main Home Screen
│   │   │   │   ├── data/
│   │   │   │   │   ├── file/                // File Operations (SAF)
│   │   │   │   │   ├── compiler/            // Runtime.exec logic
│   │   │   │   │   └── github/              // Retrofit Service
│   │   │   │   └── di/                      // Dependency Injection (Hilt/Manual)
│   │   │   └── res/                         // Resources (Drawables, Strings)
```

## 4. Dependencies
-   `androidx.compose.material3:material3`
-   `androidx.activity:activity-compose`
-   `com.squareup.retrofit2:retrofit`
-   `com.google.gson:gson`
-   `io.coil-kt:coil-compose` (for loading images/avatars)
-   `com.github.AmrDeveloper:CodeView` (or equivalent)
-   `org.eclipse.jgit:org.eclipse.jgit` (for Git operations)

## 5. Next Steps
1.  Initialize Project Structure (Gradle files).
2.  Implement `MainActivity` + `AppTheme`.
3.  Build `FileExplorer` with SAF.
4.  Build `CodeEditor` view.
5.  Integrate Terminal/Compiler logic.
