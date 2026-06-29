package com.tellshell.app.ui.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tellshell.app.data.HistoryItem
import com.tellshell.app.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var analysisResultHeight by remember { mutableFloatStateOf(200f) }
    val filteredItems = viewModel.filteredItems

    // 删除确认对话框
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${uiState.selectedIds.size} 条历史记录吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSelected() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            // === 搜索框 ===
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("搜索历史记录...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                singleLine = true
            )

            // === 全选 + 条目计数 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filteredItems.isNotEmpty() &&
                                filteredItems.all { it.id in uiState.selectedIds },
                        onCheckedChange = { viewModel.toggleSelectAll() }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "全选 (${filteredItems.size} 条)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (uiState.selectedIds.isNotEmpty()) {
                    Text(
                        text = "已选 ${uiState.selectedIds.size} 条",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
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
                    HistoryItemCard(
                        item = item,
                        isSelected = item.id in uiState.selectedIds,
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            HorizontalDivider()

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
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("删除选中 (${uiState.selectedIds.size})")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 分析输入框 + 按钮
                OutlinedTextField(
                    value = uiState.analysisInput,
                    onValueChange = viewModel::updateAnalysisInput,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入分析要求...") },
                    singleLine = true,
                    enabled = !uiState.isAnalyzing
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { viewModel.analyze(filteredItems) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.analysisInput.isNotBlank() &&
                            filteredItems.isNotEmpty() &&
                            !uiState.isAnalyzing
                ) {
                    if (uiState.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("分析中...")
                    } else {
                        Text("分析")
                    }
                }

                // 分析错误
                uiState.errorMessage?.let { error ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
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
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    val delta = -dragAmount / 5f
                                    analysisResultHeight = (analysisResultHeight + delta).coerceIn(80f, 500f)
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
                    val resultScrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
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
                                    Text(
                                        text = "分析结果",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.clearAnalysisResult() },
                                        modifier = Modifier.heightIn(min = 28.dp)
                                    ) {
                                        Text("清除", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
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
private fun HistoryItemCard(
    item: HistoryItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 时间戳
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                // 自然语言输入
                Text(
                    text = item.naturalInput,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                // 生成的命令
                Text(
                    text = item.generatedCommand,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 输出预览
                if (item.commandOutput.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.commandOutput.take(80).replace("\n", " "),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
