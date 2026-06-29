package com.tellshell.app.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tellshell.app.data.ThemeMode
import com.tellshell.app.viewmodel.SettingsUiState
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixSettingsScreen(
    uiState: SettingsUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onModelChange: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MiuixTheme.colorScheme
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showSaved = true
            delay(2000)
            showSaved = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = "设置",
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "API 配置",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TextField(
                value = uiState.baseUrl,
                onValueChange = onBaseUrlChange,
                label = "BaseURL",
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                value = uiState.apiKey,
                onValueChange = onApiKeyChange,
                label = "API Key",
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true
            )

            Spacer(Modifier.height(24.dp))

            // === 模型选择 ===
            Text(
                text = "模型",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.isLoadingModels) {
                LinearProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (uiState.availableModels.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        uiState.availableModels.forEach { model ->
                            val isSelected = uiState.selectedModel == model
                            Button(
                                onClick = { onModelChange(model) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isSelected) "✓ $model" else model
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = uiState.modelError ?: "点击刷新获取模型列表"
                )
                Button(
                    onClick = onRefreshModels,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("刷新模型列表")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "主题",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = uiState.themeMode == mode
                        Button(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSelected) "✓ ${mode.displayName}" else mode.displayName
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "系统提示词（只读）",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.systemPrompt,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "分析提示词（只读）",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.analysisPrompt,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存设置")
            }

            if (showSaved) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(text = "设置已保存", color = colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
