package com.tellshell.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tellshell.app.data.SettingsStore
import com.tellshell.app.data.ThemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主题路由 — 根据 ThemeMode 分发到 Material3 或 Miuix 主题
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    when (themeMode) {
        ThemeMode.MATERIAL3 -> {
            val colorScheme = if (isSystemInDarkTheme()) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                content()
            }
        }
        ThemeMode.MIUIX -> {
            val controller = remember { top.yukonga.miuix.kmp.theme.ThemeController() }
            MiuixTheme(controller = controller) {
                content()
            }
        }
    }
}
