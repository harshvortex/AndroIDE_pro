package com.android.idepro.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.idepro.ui.theme.*

@Composable
fun TerminalView(
    modifier: Modifier = Modifier,
    terminalOutput: String = "",
    problemsOutput: String = "",
    buildOutput: String = "",
    selectedTab: String = "TERMINAL",
    onTabSelected: (String) -> Unit = {},
    onCommand: (String) -> Unit = {},
    onClear: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    var command by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val currentContent = when (selectedTab) {
        "TERMINAL" -> terminalOutput
        "PROBLEMS" -> problemsOutput
        "OUTPUT" -> buildOutput
        else -> ""
    }

    LaunchedEffect(currentContent) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = modifier.background(VSCodeDark).fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalTab("PROBLEMS", selectedTab == "PROBLEMS") { onTabSelected("PROBLEMS") }
            TerminalTab("OUTPUT", selectedTab == "OUTPUT") { onTabSelected("OUTPUT") }
            TerminalTab("DEBUG CONSOLE", selectedTab == "DEBUG CONSOLE") { onTabSelected("DEBUG CONSOLE") }
            TerminalTab("TERMINAL", selectedTab == "TERMINAL") { onTabSelected("TERMINAL") }
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ClearAll, "Clear", tint = VSCodeText, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Close", tint = VSCodeText, modifier = Modifier.size(16.dp))
            }
        }

        Divider(color = VSCodeActivityBar, thickness = 1.dp)

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = currentContent,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                
                if (selectedTab == "TERMINAL") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "user@androidIDE:~$ ",
                            color = Color(0xFF4CAF50),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        BasicTextField(
                            value = command,
                            onValueChange = { command = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (command.isNotBlank()) {
                                        onCommand(command)
                                        command = ""
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else VSCodeText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(VSCodeAccent)
            )
        }
    }
}
