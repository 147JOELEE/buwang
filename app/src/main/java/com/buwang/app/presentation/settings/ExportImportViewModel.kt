package com.buwang.app.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwang.app.core.export.ConflictStrategy
import com.buwang.app.core.export.ExportEngine
import com.buwang.app.core.export.ExportResult
import com.buwang.app.core.export.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 导入导出页面 ViewModel
 *
 * 封装 ExportEngine 的调用，管理导出/导入状态与进度。
 */
@HiltViewModel
class ExportImportViewModel @Inject constructor(
    private val exportEngine: ExportEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportImportUiState())
    val uiState: StateFlow<ExportImportUiState> = _uiState.asStateFlow()

    /**
     * 导出全量数据到 .bwbackup 文件
     */
    fun exportAll(password: String) {
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "密码至少6位")
            return
        }
        _uiState.value = _uiState.value.copy(isExporting = true, error = null)

        viewModelScope.launch {
            try {
                val file = File(context.cacheDir, "buwang_backup_${System.currentTimeMillis()}.bwbackup")
                val result = withContext(Dispatchers.IO) {
                    exportEngine.exportAll(file, password)
                }
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportResult = result,
                    exportFilePath = file.absolutePath
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = e.message ?: "导出失败"
                )
            }
        }
    }

    /**
     * 从 .bwbackup 文件导入数据
     */
    fun importAll(uri: Uri, password: String, strategy: ConflictStrategy) {
        _uiState.value = _uiState.value.copy(isImporting = true, error = null)

        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    File(context.cacheDir, "buwang_import_temp.bwbackup").also { temp ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                val result = withContext(Dispatchers.IO) {
                    exportEngine.importAll(file, password, strategy)
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    lastImportResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = e.message ?: "导入失败"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            lastExportResult = null,
            lastImportResult = null,
            error = null
        )
    }
}

/**
 * 导入导出 UI 状态
 */
data class ExportImportUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportFilePath: String? = null,
    val lastExportResult: ExportResult? = null,
    val lastImportResult: ImportResult? = null,
    val error: String? = null
)
