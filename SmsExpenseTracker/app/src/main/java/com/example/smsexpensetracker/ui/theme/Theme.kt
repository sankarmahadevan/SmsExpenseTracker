package com.example.smsexpensetracker.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1565C0),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001C45),
    secondary        = Color(0xFF455A64),
    onSecondary      = Color.White,
    error            = Color(0xFFC62828),
    errorContainer   = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface          = Color(0xFFF8F9FA),
    background       = Color(0xFFF1F3F5),
)

@Composable
fun SmsExpenseTheme(content: @Composable () -> Unit) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(LocalContext.current)
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
