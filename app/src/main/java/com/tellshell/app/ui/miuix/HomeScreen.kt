package com.tellshell.app.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tellshell.app.data.AppInfo
import com.tellshell.app.shell.ShizukuExecutor
import com.tellshell.app.viewmodel.HomeUiState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixHomeScreen(
    uiState: HomeUiState,
    onToggleApp: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onExecute: () -> Unit,
    onRequestPermission: () -> Unit,
    onClearOutput: () -> Unit,
    onClearCommand: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val colorScheme = MiuixTheme.colorScheme
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var outputWeight by remember { mutableFloatStateOf(0.3f) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                // 顶部标题
                androidx.compose.material3.Text(
                    text = "Tell Shell",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                // 菜单项
                NavigationDrawerItem(
                    label = { Text("历史记录") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToHistory()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("设置") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    }
                )
                // 底部留空
                Spacer(Modifier.weight(1f))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
            TopAppBar(
                title = "Shell-AI",
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .clickable { scope.launch { drawerState.open() } }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "菜单"
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clickable { onRequestPermission() }
                            .background(
                                color = when (uiState.permissionState) {
                                    is ShizukuExecutor.PermissionState.Granted -> colorScheme.primary
                                    else -> colorScheme.error
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (val state = uiState.permissionState) {
                                is ShizukuExecutor.PermissionState.Idle -> "⚪ 授权"
                                is ShizukuExecutor.PermissionState.Requesting -> "..."
                                is ShizukuExecutor.PermissionState.Granted -> "✅ ${state.source}"
                                is ShizukuExecutor.PermissionState.Denied -> "❌ 拒绝"
                            },
                            color = colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    label = "搜索应用",
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )

                Text(
                    text = "已选 ${uiState.selectedPackages.size}/${uiState.filteredAppList.size} 个应用",
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.filteredAppList.size) { index ->
                        val app = uiState.filteredAppList[index]
                        MiuixAppCheckboxItem(
                            app = app,
                            isChecked = app.packageName in uiState.selectedPackages,
                            onToggle = { onToggleApp(app.packageName) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // TextField: Miuix 用 label 代替 placeholder
                TextField(
                    value = uiState.naturalInput,
                    onValueChange = onInputChange,
                    colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    minLines = 3,
                    label = "输入自然语言，如：禁用 kindle",
                    useLabelAsPlaceholder = true,
                    enabled = !uiState.isTranslating
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onTranslate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.naturalInput.isNotBlank() && !uiState.isTranslating
                ) {
                    if (uiState.isTranslating) {
                        LinearProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("翻译中...")
                    } else {
                        Text("生成命令")
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (uiState.generatedCommand.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = uiState.generatedCommand,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onExecute,
                                    enabled = !uiState.isExecuting,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (uiState.isExecuting) {
                                        LinearProgressIndicator(modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("执行中...")
                                    } else {
                                        Text("执行")
                                    }
                                }
                                Button(onClick = onClearCommand) {
                                    Text("清除")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                uiState.errorMessage?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorScheme.error, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = error, color = colorScheme.onError)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (uiState.commandOutput.isNotBlank()) {
                    // 拖拽调整大小的手柄
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(colorScheme.secondary, RoundedCornerShape(4.dp))
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    val delta = -dragAmount / 1200f
                                    outputWeight = (outputWeight + delta).coerceIn(0.1f, 0.7f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        top.yukonga.miuix.kmp.basic.Text(
                            text = "⋮",
                            fontSize = 14.sp,
                            color = colorScheme.onSecondary
                        )
                    }
                    val scrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(outputWeight)
                    ) {
                        Column {
                            Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    top.yukonga.miuix.kmp.basic.Text(
                                        text = "输出",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = onClearOutput,
                                        modifier = Modifier.heightIn(min = 28.dp)
                                    ) {
                                        top.yukonga.miuix.kmp.basic.Text("清除", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                                val clipboard = LocalClipboardManager.current
                                val context = LocalContext.current
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(scrollState)
                                ) {
                                    top.yukonga.miuix.kmp.basic.Text(
                                        text = uiState.commandOutput,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .clickable {
                                                clipboard.setText(AnnotatedString(uiState.commandOutput))
                                                android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixAppCheckboxItem(
    app: AppInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val colorScheme = MiuixTheme.colorScheme
    val toggleState = remember(isChecked) { if (isChecked) ToggleableState.On else ToggleableState.Off }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            state = toggleState,
            onClick = onToggle
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (app.isSystemApp) {
            Box(
                modifier = Modifier
                    .background(colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "系统",
                    fontSize = 10.sp,
                    color = colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
