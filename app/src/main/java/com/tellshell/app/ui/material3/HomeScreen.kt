package com.tellshell.app.ui.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tellshell.app.data.AppInfo
import com.tellshell.app.shell.ShizukuExecutor
import com.tellshell.app.viewmodel.HomeUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3HomeScreen(
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
                Text(
                    text = "Tell Shell",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                // 菜单项
                NavigationDrawerItem(
                    label = { Text("历史记录") },
                    selected = false,
                    onClick = { onNavigateToHistory() }
                )
                NavigationDrawerItem(
                    label = { Text("设置") },
                    selected = false,
                    onClick = { onNavigateToSettings() }
                )
                // 底部留空
                Spacer(Modifier.weight(1f))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Shell-AI") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "菜单")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        Surface(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(8.dp),
                            color = when (uiState.permissionState) {
                                is ShizukuExecutor.PermissionState.Granted ->
                                    MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Text(
                                text = when (val state = uiState.permissionState) {
                                    is ShizukuExecutor.PermissionState.Idle -> "⚪ 授权"
                                    is ShizukuExecutor.PermissionState.Requesting -> "..."
                                    is ShizukuExecutor.PermissionState.Granted -> "✅ ${state.source}"
                                    is ShizukuExecutor.PermissionState.Denied -> "❌ ${state.reason}"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp)
            ) {
                // === 应用搜索 ===
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索应用") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                )

                // === 应用列表（带复选框） ===
                Text(
                    text = "已选 ${uiState.selectedPackages.size}/${uiState.filteredAppList.size} 个应用",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        AppCheckboxItem(
                            app = app,
                            isChecked = app.packageName in uiState.selectedPackages,
                            onToggle = { onToggleApp(app.packageName) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // === 自然语言输入 ===
                OutlinedTextField(
                    value = uiState.naturalInput,
                    onValueChange = onInputChange,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 200.dp),
                    placeholder = { Text("输入自然语言，如：禁用 kindle") },
                    enabled = !uiState.isTranslating
                )

                Spacer(Modifier.height(8.dp))

                // === 翻译按钮 ===
                Button(
                    onClick = onTranslate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.naturalInput.isNotBlank() && !uiState.isTranslating
                ) {
                    if (uiState.isTranslating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("翻译中...")
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("生成命令")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // === 生成的命令 ===
                if (uiState.generatedCommand.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = uiState.generatedCommand,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("执行中...")
                                    } else {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("执行")
                                    }
                                }
                                OutlinedButton(onClick = onClearCommand) {
                                    Text("清除")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // === 错误信息 ===
                uiState.errorMessage?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // === 执行输出 ===
                if (uiState.commandOutput.isNotBlank()) {
                    // 拖拽调整大小的手柄
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    val delta = -dragAmount / 1200f
                                    outputWeight = (outputWeight + delta).coerceIn(0.1f, 0.7f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⋮",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val scrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                                    Text(
                                        text = "输出",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = onClearOutput,
                                        modifier = Modifier.heightIn(min = 28.dp)
                                    ) {
                                        Text("清除", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                                val clipboard = LocalClipboardManager.current
                                val context = LocalContext.current
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(scrollState)
                                ) {
                                    Text(
                                        text = uiState.commandOutput,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .clickable {
                                                clipboard.setText(AnnotatedString(uiState.commandOutput))
                                                android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun AppCheckboxItem(
    app: AppInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (app.isSystemApp) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "系统",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}
