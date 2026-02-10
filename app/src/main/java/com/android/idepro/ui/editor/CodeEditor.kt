package com.android.idepro.ui.editor

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.amrdeveloper.codeview.CodeView
import com.android.idepro.ui.theme.ThemeManager
import com.android.idepro.ui.theme.toComposeColor
import java.util.regex.Pattern

@Composable
fun CodeEditorView(
    code: String,
    onCodeChanged: (String) -> Unit,
    extension: String = "kt",
    modifier: Modifier = Modifier
) {
    val theme = ThemeManager.currentTheme
    val language = LanguageManager.getLanguageForExtension(extension) ?: LanguageManager.getKotlinLanguage()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            CodeView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                isEnabled = true
                inputType = InputType.TYPE_CLASS_TEXT or 
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE or 
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_ACTION_NONE
                
                setTypeface(Typeface.MONOSPACE)
                setEnableLineNumber(true)
                setLineNumberTextSize(25f)
                
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        onCodeChanged(s.toString())
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.requestFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                    false
                }
            }
        },
        update = { view ->
            // Update Theme Colors
            view.setBackgroundColor(android.graphics.Color.parseColor(theme.colors.ui.background))
            view.setTextColor(android.graphics.Color.parseColor(theme.colors.ui.text))
            view.setLineNumberTextColor(android.graphics.Color.parseColor("#858585"))
            
            // Clear existing patterns and re-apply based on language and theme
            view.resetSyntaxPatternList()
            
            language.rules.forEach { rule ->
                val colorHex = when (rule.token) {
                    "keyword" -> theme.colors.syntax.keyword
                    "type" -> theme.colors.syntax.type
                    "string" -> theme.colors.syntax.string
                    "comment" -> theme.colors.syntax.comment
                    "number" -> theme.colors.syntax.number
                    "annotation" -> theme.colors.syntax.annotation
                    "literal" -> theme.colors.syntax.literal
                    "tag" -> theme.colors.syntax.tag
                    "attribute" -> theme.colors.syntax.attribute
                    else -> theme.colors.ui.text
                }
                view.addSyntaxPattern(Pattern.compile(rule.pattern), android.graphics.Color.parseColor(colorHex))
            }

            if (view.text.toString() != code) {
                view.setText(code)
            }
        }
    )
}

