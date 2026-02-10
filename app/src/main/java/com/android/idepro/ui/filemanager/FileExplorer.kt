package com.android.idepro.ui.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.documentfile.provider.DocumentFile
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.idepro.ui.theme.*

@Composable
fun FileExplorer(
    modifier: Modifier = Modifier,
    onFileSelected: (DocumentFile) -> Unit,
    onFolderChangeRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("ide_prefs", Context.MODE_PRIVATE) }
    
    var rootUri by remember { 
        mutableStateOf<Uri?>(sharedPrefs.getString("last_root_uri", null)?.let { Uri.parse(it) }) 
    }
    var currentDir by remember { mutableStateOf<DocumentFile?>(null) }
    var fileList by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf<Pair<Boolean, Boolean>>(Pair(false, false)) }
    var selectedFileForMenu by remember { mutableStateOf<DocumentFile?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val updateFileList = { dir: DocumentFile? ->
        fileList = dir?.listFiles()?.sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() }) ?: emptyList()
    }

    LaunchedEffect(rootUri) {
        rootUri?.let { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val docFile = DocumentFile.fromTreeUri(context, uri)
                currentDir = docFile
                updateFileList(docFile)
            } catch (e: Exception) {
                rootUri = null
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            sharedPrefs.edit().putString("last_root_uri", it.toString()).apply()
            rootUri = it
            val docFile = DocumentFile.fromTreeUri(context, it)
            currentDir = docFile
            updateFileList(docFile)
        }
    }

    Column(modifier = modifier.background(VSCodeSidebar)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EXPLORER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = VSCodeText
            )
            if (rootUri != null) {
                Row {
                    IconButton(onClick = { launcher.launch(null) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.FolderOpen, "Change Folder", modifier = Modifier.size(16.dp), tint = VSCodeText)
                    }
                    IconButton(onClick = { showCreateDialog = Pair(true, false) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.NoteAdd, "New File", modifier = Modifier.size(16.dp), tint = VSCodeText)
                    }
                    IconButton(onClick = { updateFileList(currentDir) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(16.dp), tint = VSCodeText)
                    }
                }
            }
        }

        if (rootUri == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { launcher.launch(null) }) {
                    Text("Open Folder")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VSCodeActivityBar.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentDir?.parentFile != null && currentDir?.uri != rootUri) {
                    IconButton(onClick = {
                        currentDir = currentDir?.parentFile
                        updateFileList(currentDir)
                    }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
                Text(
                    text = (currentDir?.name ?: "PROJECT").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                // Option to remove the full folder (clear workspace)
                IconButton(onClick = { 
                    sharedPrefs.edit().remove("last_root_uri").apply()
                    rootUri = null
                    currentDir = null
                    fileList = emptyList()
                }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, "Remove Folder", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(fileList) { file ->
                    FileItem(
                        file = file,
                        level = 1,
                        onLongClick = {
                            selectedFileForMenu = file
                            showContextMenu = true
                        },
                        onClick = {
                            if (file.isDirectory) {
                                currentDir = file
                                updateFileList(file)
                            } else {
                                onFileSelected(file)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog.first) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = Pair(false, false) },
            title = { Text(if (showCreateDialog.second) "New Folder" else "New File", color = Color.White) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VSCodeDark,
                        unfocusedContainerColor = VSCodeDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotEmpty()) {
                        if (showCreateDialog.second) {
                            currentDir?.createDirectory(name)
                        } else {
                            val mimeType = getMimeType(name)
                            currentDir?.createFile(mimeType, name)
                        }
                        updateFileList(currentDir)
                        showCreateDialog = Pair(false, false)
                    }
                }) { Text("Create", color = VSCodeAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = Pair(false, false) }) { Text("Cancel", color = VSCodeText) }
            },
            containerColor = VSCodeSidebar
        )
    }

    if (showContextMenu && selectedFileForMenu != null) {
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(VSCodeSidebar)
        ) {
            DropdownMenuItem(
                text = { Text(if (selectedFileForMenu?.isDirectory == true) "Delete Folder" else "Delete File", color = Color.Red) },
                onClick = {
                    selectedFileForMenu?.delete()
                    updateFileList(currentDir)
                    showContextMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            )
        }
    }
}

fun getMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
    if (extension.isEmpty()) return "text/plain"
    
    // Professional mapping for common dev extensions
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
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "text/plain"
    }
}

@Composable
fun FileItem(file: DocumentFile, level: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    val isDir = file.isDirectory
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(vertical = 2.dp)
            .padding(start = (level * 8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDir) {
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = VSCodeText)
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }

        val iconTint = when {
            isDir -> Color(0xFFEAA629)
            file.name?.endsWith(".kt") == true -> Color(0xFF519ABA)
            file.name?.endsWith(".java") == true -> Color(0xFFE44D26)
            file.name?.endsWith(".xml") == true -> Color(0xFF62B132)
            file.name?.endsWith(".py") == true -> Color(0xFF3776AB)
            file.name?.endsWith(".js") == true -> Color(0xFFF7DF1E)
            file.name?.endsWith(".json") == true -> Color(0xFFFBC02D)
            else -> Color.Gray
        }

        Icon(
            imageVector = if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = file.name ?: "Unknown",
            color = VSCodeText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
        )
    }
}
