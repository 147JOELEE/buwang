package com.buwang.app.presentation.settings

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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/**
 * API Key 管理页面
 *
 * 支持：
 * - 内置模式（DeepSeek V3 免费 Key，开箱即用）
 * - 自定义模式（Base URL + API Key 双输入框，兼容 OpenAI 格式）
 * - 模型切换（DeepSeek V3 / 通义千问 等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyScreen(
    onBack: () -> Unit,
    viewModel: ApiKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var baseUrl by remember { mutableStateOf(uiState.customBaseUrl.ifEmpty { "https://tokens-box.com/v1/" }) }
    var apiKey by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key 管理") },
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
            // 模式选择
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用模式", style = MaterialTheme.typography.titleMedium)
                    // 内置模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.apiKeyMode == "builtin",
                                onClick = { viewModel.switchMode("builtin") }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.apiKeyMode == "builtin",
                            onClick = { viewModel.switchMode("builtin") }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("内置 Tokens-Box（MiniMax）", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "联调网关，开箱即用，无需配置",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // 自定义模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.apiKeyMode == "custom",
                                onClick = { viewModel.switchMode("custom") }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.apiKeyMode == "custom",
                            onClick = { viewModel.switchMode("custom") }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("自定义 API", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "配置 Base URL + Key，支持任意 OpenAI 兼容接口",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 自定义配置区
            if (uiState.apiKeyMode == "custom") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text(" API 配置", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Base URL") },
                            placeholder = { Text("https://tokens-box.com/v1/") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.saveCustomConfig(baseUrl, apiKey) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSaving) {
                                Text("验证并保存中…")
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Text(" 保存并验证", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        if (uiState.hasCustomKey) {
                            Text(
                                "✅ 自定义 Key 已加密存储",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 模型切换
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前模型", style = MaterialTheme.typography.titleMedium)
                    val models = viewModel.availableModels
                    models.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = uiState.selectedModel == key,
                                    onClick = { viewModel.updateModel(key) }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.selectedModel == key,
                                onClick = { viewModel.updateModel(key) }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }

    // 错误提示
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("配置失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("知道了")
                }
            }
        )
    }

    // 成功提示
    uiState.success?.let { success ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSuccess() },
            title = { Text("配置成功") },
            text = { Text(success) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSuccess() }) {
                    Text("好的")
                }
            }
        )
    }
}
