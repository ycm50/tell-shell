package com.tellshell.app.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tellshell.app.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
fun MiuixHistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredItems = viewModel.filteredItems
    val colorScheme = MiuixTheme.colorScheme
    var analysisResultHeight by remember { mutableFloatStateOf(200f) }

    // 删除确认对话框 (Miuix 风格)
    if (uiState.showDeleteConfirmDialog) {
        MiuixDeleteConfirmDialog(
            count = uiState.selectedIds.size,
            onConfirm = { viewModel.deleteSelected() },
            onDismiss = { viewModel.hideDeleteConfirm() }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = "历史记录",
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
        ) {
            // === 搜索框 ===
            TextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = "搜索历史记录...",
                useLabelAsPlaceholder = true
            )

            // === 全选 + 条目计数 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allVisibleSelected = filteredItems.isNotEmpty() &&
                        filteredItems.all { it.id in uiState.selectedIds }
                val toggleState = remember(allVisibleSelected) {
                    if (allVisibleSelected) ToggleableState.On else ToggleableState.Off
                }
                Checkbox(
                    state = toggleState,
                    onClick = { viewModel.toggleSelectAll() }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "全选 (${filteredItems.size} 条)",
                    modifier = Modifier.weight(1f)
                )
                if (uiState.selectedIds.isNotEmpty()) {
                    Text(
                        text = "已选 ${uiState.selectedIds.size} 条"
                    )
                }
            }

            // === 历史列表 ===
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val isSelected = item.id in uiState.selectedIds
                    val toggleState = remember(isSelected) {
                        if (isSelected) ToggleableState.On else ToggleableState.Off
                    }
                    MiuixHistoryItemCard(
                        item = item,
                        toggleState = toggleState,
                        onToggleSelection = { viewModel.toggleSelection(item.id) }
                    )
                }

                if (filteredItems.isEmpty()) {
                    item {
                        Spacer(Modifier.height(48.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "未找到匹配的历史记录"
                                   else "暂无历史记录",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // === 底部操作区域 ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 删除按钮
                AnimatedVisibility(visible = uiState.selectedIds.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.showDeleteConfirm() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("删除选中 (${uiState.selectedIds.size})")
                    }
                }

                // 分析输入框
                TextField(
                    value = uiState.analysisInput,
                    onValueChange = viewModel::updateAnalysisInput,
                    colors = TextFieldDefaults.textFieldColors(borderColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    label = "输入分析要求...",
                    useLabelAsPlaceholder = true,
                    enabled = !uiState.isAnalyzing
                )

                Spacer(Modifier.height(4.dp))

                // 分析按钮
                Button(
                    onClick = { viewModel.analyze(filteredItems) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.analysisInput.isNotBlank() &&
                            filteredItems.isNotEmpty() &&
                            !uiState.isAnalyzing
                ) {
                    if (uiState.isAnalyzing) {
                        LinearProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("分析中...")
                    } else {
                        Text("分析")
                    }
                }

                // 错误信息
                uiState.errorMessage?.let { error ->
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.error, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = error, color = colorScheme.onError)
                    }
                }

                // 分析结果
                uiState.analysisResult?.let { result ->
                    Spacer(Modifier.height(4.dp))
                    // 拖拽手柄
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(colorScheme.secondary, RoundedCornerShape(4.dp))
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    val delta = -dragAmount / 5f
                                    analysisResultHeight = (analysisResultHeight + delta).coerceIn(80f, 500f)
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
                    val resultScrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = analysisResultHeight.dp)
                    ) {
                        Row {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(resultScrollState)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    top.yukonga.miuix.kmp.basic.Text(
                                        text = "分析结果",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { viewModel.clearAnalysisResult() },
                                        modifier = Modifier.heightIn(min = 28.dp)
                                    ) {
                                        top.yukonga.miuix.kmp.basic.Text("清除", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                top.yukonga.miuix.kmp.basic.Text(
                                    text = result,
                                    fontSize = 14.sp
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
private fun MiuixHistoryItemCard(
    item: com.tellshell.app.data.HistoryItem,
    toggleState: ToggleableState,
    onToggleSelection: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val colorScheme = MiuixTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (toggleState == ToggleableState.On)
                        colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    state = toggleState,
                    onClick = onToggleSelection,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 时间
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    // 自然语言
                    Text(
                        text = item.naturalInput,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    // 命令
                    Text(
                        text = item.generatedCommand,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 输出预览
                    if (item.commandOutput.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.commandOutput.take(80).replace("\n", " "),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixDeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除选中的 $count 条历史记录吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
