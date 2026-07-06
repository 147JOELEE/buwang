package com.buwang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.buwang.app.presentation.chat.ChatScreen
import com.buwang.app.presentation.persona.CreatePersonaScreen
import com.buwang.app.presentation.persona.PersonaListDrawer
import com.buwang.app.presentation.settings.ApiKeyScreen
import com.buwang.app.presentation.settings.ExportImportScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation()
                }
            }
        }
    }
}

/**
 * 主导航：串联 角色列表(Drawer) + 聊天 + 创建角色 + 设置页
 *
 * 页面路由：
 * - chat: 聊天主界面（默认）
 * - create: 创建角色
 * - exportImport: 数据导入导出
 * - apiKey: API Key 管理
 */
@Composable
private fun MainNavigation() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentPersonaId by remember { mutableStateOf<String?>("preset_default") }
    var route by remember { mutableStateOf("chat") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            PersonaListDrawer(
                onPersonaSelected = { id ->
                    currentPersonaId = id
                    route = "chat"
                },
                onCreateClicked = { route = "create" },
                onDismiss = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        when (route) {
            "create" -> {
                CreatePersonaScreen(
                    onBack = { route = "chat" },
                    onCreated = { personaId ->
                        route = "chat"
                        currentPersonaId = personaId
                    }
                )
            }
            "exportImport" -> {
                ExportImportScreen(onBack = { route = "chat" })
            }
            "apiKey" -> {
                ApiKeyScreen(onBack = { route = "chat" })
            }
            else -> {
                if (currentPersonaId != null) {
                    ChatScreen(
                        personaId = currentPersonaId!!,
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        }
                    )
                } else {
                    CreatePersonaScreen(
                        onBack = { },
                        onCreated = { personaId -> currentPersonaId = personaId }
                    )
                }
            }
        }
    }
}
