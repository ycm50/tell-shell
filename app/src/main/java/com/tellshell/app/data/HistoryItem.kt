package com.tellshell.app.data

import java.util.UUID

/**
 * 历史记录条目
 */
data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val naturalInput: String,
    val generatedCommand: String,
    val commandOutput: String = "",
    val appContext: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val analyzedText: String? = null
)
