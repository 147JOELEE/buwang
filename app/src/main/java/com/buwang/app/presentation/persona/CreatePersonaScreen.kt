package com.buwang.app.presentation.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.buwang.app.core.personality.MbtiMapper
import com.buwang.app.core.personality.PresetCharacters
import kotlinx.coroutines.launch

/**
 * 创建角色页面
 *
 * 提供五种创建方式，全部接人格引擎生成 System Prompt：
 * 1. 预设角色（内置 4 个）
 * 2. 手动配置（名称 + 描述）
 * 3. MBTI 导入（16 型 → BigFive）
 * 4. 聊天文本导入（LLM 分析用户发言 → BigFive）
 * 5. ST 角色卡导入（解析中文字段）
 *
 * 创建成功后通过 [onCreated] 回传**角色真实 ID**，由主页据此进入对话。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonaScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreatePersonaViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("预设角色", "手动配置", "MBTI导入", "文本导入", "角色卡导入")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建角色") },
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
            // 创建方式 Tab（分段选择）
            SegmentedTabs(tabs, selectedTab) { selectedTab = it }

            when (selectedTab) {
                0 -> PresetPersonaSection(viewModel, onCreated)
                1 -> ManualCreateSection(viewModel, onCreated)
                2 -> MbtiImportSection(viewModel, onCreated)
                3 -> ChatTextImportSection(viewModel, onCreated)
                4 -> StCardImportSection(viewModel, onCreated)
            }
        }
    }
}

/**
 * 顶部分段 Tab
 */
@Composable
private fun SegmentedTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selected == index
            OutlinedButton(
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
                enabled = true
            ) {
                Text(
                    tab,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 预设角色区块
 */
@Composable
private fun PresetPersonaSection(
    viewModel: CreatePersonaViewModel,
    onCreated: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val presets = PresetCharacters.getAllPresets()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "选择一个预设角色开始对话：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        presets.forEach { preset ->
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val id = viewModel.createFromPreset(preset.id)
                        onCreated(id)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 手动配置区块
 */
@Composable
private fun ManualCreateSection(
    viewModel: CreatePersonaViewModel,
    onCreated: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 20) name = it },
            label = { Text("角色名称 (1-20字)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("角色描述（选填）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    val id = viewModel.createFromManual(name, description)
                    onCreated(id)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(" 创建并进入对话", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * MBTI 导入区块
 */
@Composable
private fun MbtiImportSection(
    viewModel: CreatePersonaViewModel,
    onCreated: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selectedMbti by remember { mutableStateOf<String?>(null) }
    val types = MbtiMapper.supportedTypes()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 20) name = it },
            label = { Text("角色名称 (1-20字)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("选择 MBTI 类型：", style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            types.chunked(4).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTypes.forEach { type ->
                        val isSelected = selectedMbti == type
                        OutlinedButton(
                            onClick = { selectedMbti = type },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    type,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                MbtiMapper.mbtiNameCn(type)?.let { cn ->
                                    Text(
                                        cn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    val id = viewModel.createFromMbti(name, selectedMbti ?: "")
                    onCreated(id)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && selectedMbti != null
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(" 生成并进入对话", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * 聊天文本导入区块（调用 LLM 分析）
 */
@Composable
private fun ChatTextImportSection(
    viewModel: CreatePersonaViewModel,
    onCreated: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var chatText by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 20) name = it },
            label = { Text("角色名称 (1-20字，选填)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = chatText,
            onValueChange = { chatText = it },
            label = { Text("粘贴一段聊天记录（仅用户发言效果更好）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5
        )
        if (analyzing) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("正在分析聊天风格…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    analyzing = true
                    error = null
                    try {
                        val id = viewModel.createFromChat(name, chatText)
                        onCreated(id)
                    } catch (e: Exception) {
                        error = "分析失败：${e.message ?: "未知错误"}"
                    } finally {
                        analyzing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !analyzing && chatText.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(" 分析并创建", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * ST 角色卡导入区块
 */
@Composable
private fun StCardImportSection(
    viewModel: CreatePersonaViewModel,
    onCreated: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "支持中文字段，如：\n姓名：小夜\n性格：温柔但倔强\n外貌：短发戴眼镜\n背景：来自海边的小镇",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("粘贴 ST 角色卡文本") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    val id = viewModel.createFromStCard(text)
                    onCreated(id)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(" 解析并创建", modifier = Modifier.padding(start = 4.dp))
        }
    }
}
