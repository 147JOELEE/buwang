package com.buwang.app.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buwang.app.core.export.ConflictStrategy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据导入/导出页面
 *
 * 支持：
 * - 全量导出：AES密码加密 → .bwbackup → 系统分享
 * - 全量导入：选文件 → 输密码（最多5次）→ 冲突处理 → 恢复
 * - 单角色ST导出（入口预留）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    onBack: () -> Unit,
    viewModel: ExportImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // 文件选择器（导入）
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { showImportDialog = true }
        // 实际导入逻辑在对话框内触发，需保存 uri
        if (uri != null) pendingImportUri = uri
    }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // 文件分享（导出）
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        // 实际导出由 viewModel 完成，此处仅触发分享
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据导入 / 导出") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 导出卡片
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(" 导出全部数据", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "将所有角色、会话、消息加密导出为 .bwbackup 文件，可迁移到新设备。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showExportDialog = true },
                        enabled = !uiState.isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isExporting) {
                            Text("导出中…")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Text(" 开始导出", modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    uiState.lastExportResult?.let { r ->
                        Text(
                            "✅ 导出成功：角色${r.personaCount}个，会话${r.conversationCount}个，消息${r.messageCount}条",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 导入卡片
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(" 导入数据", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "从 .bwbackup 文件恢复数据，需输入导出时设置的密码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { importLauncher.launch("application/octet-stream") },
                        enabled = !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isImporting) {
                            Text("导入中…")
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Text(" 选择备份文件", modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    uiState.lastImportResult?.let { r ->
                        Column {
                            Text(
                                "✅ 导入成功：角色${r.personaCount}个，会话${r.conversationCount}个，消息${r.messageCount}条",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (r.skippedPersonas > 0 || r.skippedConversations > 0) {
                                Text(
                                    "⏭️ 跳过：角色${r.skippedPersonas}个，会话${r.skippedConversations}个",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (r.conflicts.isNotEmpty()) {
                                Text(
                                    "⚠️ ${r.conflicts.joinToString("；")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 导出密码对话框
    if (showExportDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("设置导出密码") },
            text = {
                Column {
                    Text("此密码用于加密备份文件，导入时需输入。请牢记，遗忘无法恢复。")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码（至少6位）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.exportAll(password)
                        showExportDialog = false
                    },
                    enabled = password.length >= 6
                ) {
                    Text("确认导出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 导入密码对话框
    if (showImportDialog && pendingImportUri != null) {
        var password by remember { mutableStateOf("") }
        var strategy by remember { mutableStateOf(ConflictStrategy.KEEP_BOTH) }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("输入导入密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("备份密码") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("冲突处理策略：", style = MaterialTheme.typography.bodySmall)
                    ConflictStrategy.values().forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = strategy == s,
                                    onClick = { strategy = s }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = strategy == s, onClick = { strategy = s })
                            Text(
                                when (s) {
                                    ConflictStrategy.OVERWRITE -> "覆盖已有数据"
                                    ConflictStrategy.KEEP_BOTH -> "保留两者（推荐）"
                                    ConflictStrategy.SKIP -> "跳过冲突项"
                                },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importAll(pendingImportUri!!, password, strategy)
                        showImportDialog = false
                    }
                ) {
                    Text("开始导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 错误提示
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("操作失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("知道了")
                }
            }
        )
    }
}
