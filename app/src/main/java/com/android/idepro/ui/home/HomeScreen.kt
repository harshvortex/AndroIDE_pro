package com.android.idepro.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.documentfile.provider.DocumentFile
import com.android.idepro.ui.editor.CodeEditorView
import com.android.idepro.ui.filemanager.FileExplorer
import com.android.idepro.ui.terminal.TerminalView
import com.android.idepro.ui.theme.*
import com.android.idepro.data.compiler.CompilerService
import com.android.idepro.learning.*
import com.android.idepro.ui.editor.LanguageManager
import com.android.idepro.execution.ExecutionEngine
import com.android.idepro.execution.TaskManager
import com.android.idepro.execution.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
fun HomeScreen() {
    var isLoggedIn by remember { mutableStateOf(false) }
    
    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    } else {
        IDE_MainContent()
    }
}

@Composable
fun IDE_MainContent() {
    var openFiles by remember { mutableStateOf(listOf<DocumentFile>()) }
    var activeFile by remember { mutableStateOf<DocumentFile?>(null) }
    var activeFileContent by remember { mutableStateOf("") }
    var showTerminal by remember { mutableStateOf(false) }
    var terminalTab by remember { mutableStateOf("TERMINAL") }
    var selectedActivity by remember { mutableStateOf("explorer") }
    var showSidebar by remember { mutableStateOf(true) }
    
    val currentTheme = ThemeManager.currentTheme
    
    // Learning Mode State
    var isLearningMode by remember { mutableStateOf(false) }
    var activeExplanation by remember { mutableStateOf<ErrorMatchResult?>(null) }
    
    // Persistent state for different tabs
    var terminalOutput by remember { mutableStateOf("Ready to build...") }
    var problemsOutput by remember { mutableStateOf("No problems detected.") }
    var buildOutput by remember { mutableStateOf("Build history empty.") }
    
    var showCommandPalette by remember { mutableStateOf(false) }
    var showTaskPalette by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Optimized Saving: Debounced background save
    LaunchedEffect(activeFileContent, activeFile) {
        if (activeFile != null) {
            delay(1000) // Wait for 1 second of inactivity before saving
            withContext(Dispatchers.IO) {
                saveFileContent(context, activeFile!!, activeFileContent)
            }
        }
    }

    // Effect to detect errors when output changes in Learning Mode
    LaunchedEffect(terminalOutput) {
        if (isLearningMode) {
            activeExplanation = ErrorRuleRegistry.match(terminalOutput)
        }
    }

    if (showCommandPalette) {
        CommandPalette(onDismiss = { showCommandPalette = false })
    }

    if (showTaskPalette) {
        TaskPalette(
            onDismiss = { showTaskPalette = false },
            onTaskSelected = { task ->
                showTerminal = true
                terminalTab = "TERMINAL"
                terminalOutput += "\nRunning task: ${task.name}...\n"
                scope.launch {
                    val projectDir = context.getExternalFilesDir(null) // Simplified for demo
                    if (projectDir != null) {
                        ExecutionEngine.runTask(task, projectDir) { output ->
                            terminalOutput += output
                        }
                    } else {
                        terminalOutput += "Error: Could not determine project directory.\n"
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(currentTheme.colors.ui.background.toComposeColor())) {
        TopToolbar(
            onRun = { showTaskPalette = true },
            onTogglePanel = { showTerminal = !showTerminal },
            onCommandPalette = { showCommandPalette = true }
        )

        Row(modifier = Modifier.weight(1f)) {
            ActivityBar(
                selectedActivity = selectedActivity,
                onActivitySelected = { activity ->
                    if (selectedActivity == activity) {
                        showSidebar = !showSidebar
                    } else {
                        selectedActivity = activity
                        showSidebar = true
                    }
                }
            )

            AnimatedVisibility(visible = showSidebar) {
                Box(modifier = Modifier.width(250.dp).fillMaxHeight().background(currentTheme.colors.ui.sidebar.toComposeColor())) {
                    when (selectedActivity) {
                        "explorer" -> FileExplorer(
                            modifier = Modifier.fillMaxSize(),
                            onFileSelected = { file ->
                                if (!openFiles.contains(file)) {
                                    openFiles = openFiles + file
                                }
                                activeFile = file
                                activeFileContent = readFileContent(context, file)
                            }
                        )
                        "search" -> SearchPanel()
                        "git" -> PlaceholderPanel("SOURCE CONTROL")
                        "debug" -> PlaceholderPanel("RUN AND DEBUG")
                        "extensions" -> ExtensionsPanel()
                        "settings" -> SettingsPanel(
                            isLearningMode = isLearningMode,
                            onLearningModeToggle = { isLearningMode = it }
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (openFiles.isNotEmpty()) {
                    EditorTabs(
                        openFiles = openFiles,
                        activeFile = activeFile,
                        onFileSelected = { file ->
                            activeFile = file
                            activeFileContent = readFileContent(context, file)
                        },
                        onFileClosed = { file ->
                            openFiles = openFiles.filter { it != file }
                            if (activeFile == file) {
                                activeFile = openFiles.lastOrNull()
                                activeFileContent = activeFile?.let { readFileContent(context, it) } ?: ""
                            }
                        }
                    )
                    Breadcrumbs(activeFile)
                }

                Box(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (activeFile != null) {
                                CodeEditorView(
                                    code = activeFileContent,
                                    onCodeChanged = { activeFileContent = it },
                                    extension = activeFile?.name?.substringAfterLast(".", "") ?: "kt",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                WelcomeScreen(
                                    onNewFile = { /* ... */ },
                                    onOpenFolder = { /* ... */ },
                                    onSearch = { selectedActivity = "search"; showSidebar = true }
                                )
                            }
                            
                            if (isLearningMode && activeExplanation != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    ErrorExplanationCard(
                                        result = activeExplanation!!,
                                        onDismiss = { activeExplanation = null }
                                    )
                                }
                            }
                        }
                        if (activeFile != null) {
                            KeyBar(onKeyClick = { key ->
                                activeFileContent += if (key == "TAB") "    " else key
                            })
                        }
                    }
                }

                if (showTerminal) {
                    TerminalView(
                        modifier = Modifier.height(250.dp),
                        terminalOutput = terminalOutput,
                        problemsOutput = problemsOutput,
                        buildOutput = buildOutput,
                        selectedTab = terminalTab,
                        onTabSelected = { terminalTab = it },
                        onCommand = { cmd ->
                            terminalOutput += "\nuser@androidIDE:~$ $cmd\n"
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    CompilerService.executeCommand(cmd, context.getExternalFilesDir(null))
                                }
                                terminalOutput += result
                            }
                        },
                        onClear = { 
                            when(terminalTab) {
                                "TERMINAL" -> terminalOutput = ""
                                "PROBLEMS" -> problemsOutput = ""
                                "OUTPUT" -> buildOutput = ""
                            }
                        },
                        onClose = { showTerminal = false }
                    )
                }
            }
        }

        StatusBar(
            showTerminal = showTerminal,
            onTerminalToggle = { showTerminal = !showTerminal },
            activeFile = activeFile,
            isLearningMode = isLearningMode
        )
    }
}

@Composable
fun TaskPalette(onDismiss: () -> Unit, onTaskSelected: (Task) -> Unit) {
    val context = LocalContext.current
    val theme = ThemeManager.currentTheme
    // For demo purposes, using a generic tasks list
    val tasks = TaskManager.loadTasks(context, DocumentFile.fromFile(context.getExternalFilesDir(null)!!))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = theme.colors.ui.sidebar.toComposeColor(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Task", color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskSelected(task); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(task.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(task.description, color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorExplanationCard(result: ErrorMatchResult, onDismiss: () -> Unit) {
    val theme = ThemeManager.currentTheme
    Surface(
        modifier = Modifier.width(280.dp),
        color = theme.colors.ui.sidebar.toComposeColor(),
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, theme.colors.ui.accent.toComposeColor())
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = theme.colors.ui.accent.toComposeColor(), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(result.title, color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(16.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(result.explanation, color = theme.colors.ui.text.toComposeColor(), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tip: ${result.suggestion}", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SettingsPanel(isLearningMode: Boolean, onLearningModeToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val theme = ThemeManager.currentTheme
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).background(theme.colors.ui.sidebar.toComposeColor())) {
        Text("SETTINGS", style = MaterialTheme.typography.labelMedium, color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsGroup("Theme") {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.colors.ui.accent.toComposeColor())
                ) {
                    Text("Current Theme: ${theme.name}")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(theme.colors.ui.sidebar.toComposeColor())
                ) {
                    ThemeManager.getAvailableThemes(context).forEach { themeName ->
                        DropdownMenuItem(
                            text = { Text(themeName, color = theme.colors.ui.text.toComposeColor()) },
                            onClick = {
                                ThemeManager.loadTheme(context, themeName)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsGroup("Learning") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Learning Mode", color = theme.colors.ui.text.toComposeColor(), style = MaterialTheme.typography.bodySmall)
                    Text("Explains errors in plain English", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.5f), fontSize = 10.sp)
                }
                Switch(
                    checked = isLearningMode, 
                    onCheckedChange = onLearningModeToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.colors.ui.accent.toComposeColor())
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroup("Editor") {
            SettingsToggle("Auto Save", true)
            SettingsToggle("Line Numbers", true)
            SettingsSlider("Font Size", 14f)
        }
    }
}

@Composable
fun StatusBar(showTerminal: Boolean, onTerminalToggle: () -> Unit, activeFile: DocumentFile?, isLearningMode: Boolean) {
    val theme = ThemeManager.currentTheme
    val accentColor = theme.colors.ui.accent.toComposeColor()
    val textColor = if (theme.isDark) Color.White else Color.Black
    
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp).background(if (isLearningMode) Color(0xFF68217A) else accentColor).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.clickable { onTerminalToggle() }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(12.dp), tint = textColor)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Terminal", color = textColor, style = MaterialTheme.typography.labelSmall)
        }
        
        if (isLearningMode) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.School, null, modifier = Modifier.size(12.dp), tint = Color.White)
            Text(" Learning Mode ON", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.weight(1f))
        
        if (activeFile != null) {
            Text("Ln 1, Col 1", color = textColor, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Text(activeFile.name?.substringAfterLast(".", "")?.uppercase() ?: "PLAIN TEXT", color = textColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val theme = ThemeManager.currentTheme
    Box(modifier = Modifier.fillMaxSize().background(theme.colors.ui.background.toComposeColor()), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(300.dp).background(theme.colors.ui.sidebar.toComposeColor()).padding(24.dp)
        ) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(64.dp), tint = theme.colors.ui.accent.toComposeColor())
            Spacer(modifier = Modifier.height(16.dp))
            Text("Login to AndroidIDE Pro", color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = theme.colors.ui.background.toComposeColor(),
                    unfocusedContainerColor = theme.colors.ui.background.toComposeColor(),
                    focusedTextColor = theme.colors.ui.text.toComposeColor(),
                    unfocusedTextColor = theme.colors.ui.text.toComposeColor()
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = theme.colors.ui.background.toComposeColor(),
                    unfocusedContainerColor = theme.colors.ui.background.toComposeColor(),
                    focusedTextColor = theme.colors.ui.text.toComposeColor(),
                    unfocusedTextColor = theme.colors.ui.text.toComposeColor()
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginSuccess,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.colors.ui.accent.toComposeColor())
            ) {
                Text("Login")
            }
        }
    }
}

@Composable
fun KeyBar(onKeyClick: (String) -> Unit) {
    val theme = ThemeManager.currentTheme
    val keys = listOf("{", "}", "[", "]", "(", ")", ";", "<", ">", "/", "\\", "|", "&", "!", "_", "=", "\"", "'", ":", "TAB")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(theme.colors.ui.activityBar.toComposeColor())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(keys) { key ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(44.dp)
                    .fillMaxHeight(0.8f)
                    .background(theme.colors.ui.sidebar.toComposeColor(), shape = MaterialTheme.shapes.extraSmall)
                    .clickable { onKeyClick(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(key, color = theme.colors.ui.text.toComposeColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TopToolbar(
    onRun: () -> Unit,
    onTogglePanel: () -> Unit,
    onCommandPalette: () -> Unit
) {
    val theme = ThemeManager.currentTheme
    var showFileMenu by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showGoMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(theme.colors.ui.activityBar.toComposeColor())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IDE_Menu("File", showFileMenu, { showFileMenu = it }) {
            DropdownMenuItem(text = { Text("New File", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showFileMenu = false })
            DropdownMenuItem(text = { Text("Open Folder", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showFileMenu = false })
            DropdownMenuItem(text = { Text("Save", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showFileMenu = false })
        }
        IDE_Menu("Edit", showEditMenu, { showEditMenu = it }) {
            DropdownMenuItem(text = { Text("Undo", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showEditMenu = false })
            DropdownMenuItem(text = { Text("Redo", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showEditMenu = false })
            Divider(color = theme.colors.ui.background.toComposeColor())
            DropdownMenuItem(text = { Text("Format Document", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showEditMenu = false })
        }
        IDE_Menu("View", showViewMenu, { showViewMenu = it }) {
            DropdownMenuItem(text = { Text("Appearance: Toggle Panel", color = theme.colors.ui.text.toComposeColor()) }, onClick = { onTogglePanel(); showViewMenu = false })
            DropdownMenuItem(text = { Text("Reset View Locations", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showViewMenu = false })
        }
        IDE_Menu("Go", showGoMenu, { showGoMenu = it }) {
            DropdownMenuItem(text = { Text("Go to File...", color = theme.colors.ui.text.toComposeColor()) }, onClick = { onCommandPalette(); showGoMenu = false })
            DropdownMenuItem(text = { Text("Go to Line...", color = theme.colors.ui.text.toComposeColor()) }, onClick = { showGoMenu = false })
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onCommandPalette) {
            Icon(Icons.Default.Keyboard, "Command Palette", tint = theme.colors.ui.text.toComposeColor(), modifier = Modifier.size(18.dp))
        }
        
        IconButton(onClick = onRun) {
            Icon(Icons.Default.PlayArrow, "Run", tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
        }
        
        IconButton(onClick = { /* Notifications */ }) {
            Icon(Icons.Default.Notifications, "Notifications", tint = theme.colors.ui.text.toComposeColor(), modifier = Modifier.size(18.dp))
        }
        
        IconButton(onClick = { /* Account */ }) {
            Icon(Icons.Default.AccountCircle, "Accounts", tint = theme.colors.ui.text.toComposeColor(), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun IDE_Menu(label: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val theme = ThemeManager.currentTheme
    Box {
        TextButton(onClick = { onExpandedChange(true) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(label, color = theme.colors.ui.text.toComposeColor(), fontSize = 12.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(theme.colors.ui.sidebar.toComposeColor())
        ) {
            content()
        }
    }
}

@Composable
fun CommandPalette(onDismiss: () -> Unit) {
    val theme = ThemeManager.currentTheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = theme.colors.ui.sidebar.toComposeColor(),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                var query by remember { mutableStateOf(">") }
                TextField(
                    value = query,
                    onValueChange = { if (it.startsWith(">")) query = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = theme.colors.ui.background.toComposeColor(),
                        unfocusedContainerColor = theme.colors.ui.background.toComposeColor(),
                        focusedTextColor = theme.colors.ui.text.toComposeColor(),
                        unfocusedTextColor = theme.colors.ui.text.toComposeColor()
                    ),
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val commands = listOf("Go to File", "Run Build", "Toggle Panel", "Settings")
                commands.forEach { cmd ->
                    Text(
                        text = cmd,
                        color = theme.colors.ui.text.toComposeColor(),
                        modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(8.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityBar(
    selectedActivity: String,
    onActivitySelected: (String) -> Unit
) {
    val theme = ThemeManager.currentTheme
    Column(
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(theme.colors.ui.activityBar.toComposeColor()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActivityIcon(Icons.Default.FileCopy, selectedActivity == "explorer") { onActivitySelected("explorer") }
        ActivityIcon(Icons.Default.Search, selectedActivity == "search") { onActivitySelected("search") }
        ActivityIcon(Icons.Default.Source, selectedActivity == "git") { onActivitySelected("git") }
        ActivityIcon(Icons.Default.BugReport, selectedActivity == "debug") { onActivitySelected("debug") }
        ActivityIcon(Icons.Default.Extension, selectedActivity == "extensions") { onActivitySelected("extensions") }
        
        Spacer(modifier = Modifier.weight(1f))
        
        ActivityIcon(Icons.Default.Settings, selectedActivity == "settings") { onActivitySelected("settings") }
    }
}

@Composable
fun ActivityIcon(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val theme = ThemeManager.currentTheme
    Box(modifier = Modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (isSelected) {
            Box(modifier = Modifier.align(Alignment.CenterStart).width(2.dp).fillMaxHeight(0.5f).background(theme.colors.ui.accent.toComposeColor()))
        }
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = if (isSelected) theme.colors.ui.text.toComposeColor() else theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f))
    }
}

@Composable
fun SearchPanel() {
    val theme = ThemeManager.currentTheme
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SEARCH", style = MaterialTheme.typography.labelMedium, color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.colors.ui.background.toComposeColor(),
                unfocusedContainerColor = theme.colors.ui.background.toComposeColor(),
                focusedTextColor = theme.colors.ui.text.toComposeColor(),
                unfocusedTextColor = theme.colors.ui.text.toComposeColor()
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Replace", style = MaterialTheme.typography.labelSmall, color = theme.colors.ui.text.toComposeColor())
        TextField(
            value = "",
            onValueChange = { },
            placeholder = { Text("Replace", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.colors.ui.background.toComposeColor(),
                unfocusedContainerColor = theme.colors.ui.background.toComposeColor(),
                focusedTextColor = theme.colors.ui.text.toComposeColor(),
                unfocusedTextColor = theme.colors.ui.text.toComposeColor()
            )
        )
    }
}

@Composable
fun ExtensionsPanel() {
    val theme = ThemeManager.currentTheme
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("EXTENSIONS", style = MaterialTheme.typography.labelMedium, color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        ExtensionItem("Kotlin", "JetBrains", "v1.9.0")
        ExtensionItem("Android Support", "Google", "v1.0.0")
        ExtensionItem("Material Theme", "Material", "v2.1.0")
    }
}

@Composable
fun ExtensionItem(name: String, author: String, version: String) {
    val theme = ThemeManager.currentTheme
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(theme.colors.ui.activityBar.toComposeColor()), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Extension, null, tint = theme.colors.ui.accent.toComposeColor())
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, color = theme.colors.ui.text.toComposeColor(), style = MaterialTheme.typography.bodyMedium)
            Text("$author • $version", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun Breadcrumbs(activeFile: DocumentFile?) {
    val theme = ThemeManager.currentTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(theme.colors.ui.background.toComposeColor())
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("app", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f), fontSize = 11.sp)
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f))
        Text("src", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f), fontSize = 11.sp)
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f))
        Text("main", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f), fontSize = 11.sp)
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f))
        Text(activeFile?.name ?: "", color = theme.colors.ui.text.toComposeColor(), fontSize = 11.sp)
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val theme = ThemeManager.currentTheme
    Column {
        Text(title, color = theme.colors.ui.accent.toComposeColor(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SettingsToggle(label: String, initialValue: Boolean) {
    val theme = ThemeManager.currentTheme
    var checked by remember { mutableStateOf(initialValue) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.colors.ui.text.toComposeColor(), style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = { checked = it }, colors = SwitchDefaults.colors(checkedThumbColor = theme.colors.ui.accent.toComposeColor(), checkedTrackColor = theme.colors.ui.accent.toComposeColor().copy(alpha = 0.5f)))
    }
}

@Composable
fun PlaceholderPanel(title: String) {
    val theme = ThemeManager.currentTheme
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Coming soon...", color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f))
    }
}

@Composable
fun WelcomeScreen(onNewFile: () -> Unit, onOpenFolder: () -> Unit, onSearch: () -> Unit) {
    val theme = ThemeManager.currentTheme
    Box(modifier = Modifier.fillMaxSize().background(theme.colors.ui.background.toComposeColor()), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(80.dp), tint = theme.colors.ui.activityBar.toComposeColor())
            Text("AndroidIDE Pro", style = MaterialTheme.typography.headlineMedium, color = theme.colors.ui.text.toComposeColor(), fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(24.dp))
            
            WelcomeActionItem(Icons.Default.NoteAdd, "New File", "Ctrl+N", onNewFile)
            WelcomeActionItem(Icons.Default.FolderOpen, "Open Folder", "Ctrl+O", onOpenFolder)
            WelcomeActionItem(Icons.Default.Search, "Search", "Ctrl+Shift+F", onSearch)
        }
    }
}

@Composable
fun WelcomeActionItem(icon: ImageVector, label: String, shortcut: String, onClick: () -> Unit) {
    val theme = ThemeManager.currentTheme
    Row(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = theme.colors.ui.accent.toComposeColor(), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = theme.colors.ui.text.toComposeColor(), style = MaterialTheme.typography.bodyMedium)
            Text(shortcut, color = theme.colors.ui.text.toComposeColor().copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun EditorTabs(
    openFiles: List<DocumentFile>,
    activeFile: DocumentFile?,
    onFileSelected: (DocumentFile) -> Unit,
    onFileClosed: (DocumentFile) -> Unit
) {
    val theme = ThemeManager.currentTheme
    LazyRow(modifier = Modifier.fillMaxWidth().height(35.dp).background(theme.colors.ui.sidebar.toComposeColor())) {
        items(openFiles) { file ->
            val isSelected = file == activeFile
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(if (isSelected) theme.colors.ui.background.toComposeColor() else Color.Transparent)
                    .clickable { onFileSelected(file) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.InsertDriveFile, null, modifier = Modifier.size(14.dp), tint = Color(0xFF519ABA))
                Spacer(modifier = Modifier.width(6.dp))
                Text(file.name ?: "Unknown", color = if (isSelected) theme.colors.ui.text.toComposeColor() else theme.colors.ui.text.toComposeColor().copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(14.dp).clickable { onFileClosed(file) }, tint = if (isSelected) theme.colors.ui.text.toComposeColor() else Color.Transparent)
            }
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = theme.colors.ui.background.toComposeColor())
        }
    }
}

fun readFileContent(context: Context, file: DocumentFile): String {
    return try {
        context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: ""
    } catch (e: Exception) {
        "Error reading file: ${e.localizedMessage}"
    }
}

fun saveFileContent(context: Context, file: DocumentFile, content: String) {
    try {
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                writer.write(content)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun SettingsSlider(label: String, value: Float) {
    val theme = ThemeManager.currentTheme
    var sliderValue by remember { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = theme.colors.ui.text.toComposeColor(), style = MaterialTheme.typography.bodySmall)
            Text("${sliderValue.toInt()}px", color = theme.colors.ui.accent.toComposeColor(), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 8f..24f,
            colors = SliderDefaults.colors(thumbColor = theme.colors.ui.accent.toComposeColor(), activeTrackColor = theme.colors.ui.accent.toComposeColor())
        )
    }
}
