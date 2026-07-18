package com.tellshell.app.data

/**
 * 桌面应用信息
 * 注意：不要存放 Drawable 等 Android 图形对象，以免在 Compose 重组时崩溃
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false
)
