package com.android.idepro

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.android.idepro.ui.editor.LanguageManager
import com.android.idepro.ui.home.HomeScreen
import com.android.idepro.ui.theme.AndroidIDEProTheme
import com.android.idepro.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize IDE Systems
        ThemeManager.loadTheme(this, "VS Code Dark") // Initial theme
        LanguageManager.loadLanguages(this)
        
        // Ensure keyboard doesn't overlap the layout but resizes it
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        setContent {
            AndroidIDEProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}
