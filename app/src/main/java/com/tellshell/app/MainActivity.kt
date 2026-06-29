package com.tellshell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tellshell.app.data.SettingsStore
import com.tellshell.app.data.ThemeMode
import com.tellshell.app.ui.material3.Material3HistoryScreen
import com.tellshell.app.ui.material3.Material3HomeScreen
import com.tellshell.app.ui.material3.Material3SettingsScreen
import com.tellshell.app.ui.miuix.MiuixHistoryScreen
import com.tellshell.app.ui.miuix.MiuixHomeScreen
import com.tellshell.app.ui.miuix.MiuixSettingsScreen
import com.tellshell.app.ui.theme.AppTheme
import com.tellshell.app.viewmodel.HistoryViewModel
import com.tellshell.app.viewmodel.HomeViewModel
import com.tellshell.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val settingsStore = remember { SettingsStore(context) }
            val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.MATERIAL3)
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            AppTheme(themeMode = themeMode) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        val viewModel: HomeViewModel = viewModel()

                        val uiState by viewModel.uiState.collectAsState()

                        when (themeMode) {
                            ThemeMode.MATERIAL3 -> {
                                Material3HomeScreen(
                                    uiState = uiState,
                                    onToggleApp = viewModel::toggleAppSelection,
                                    onInputChange = viewModel::updateNaturalInput,
                                    onTranslate = viewModel::translateToCommand,
                                    onExecute = viewModel::executeCommand,
                                    onRequestPermission = viewModel::requestPermission,
                                    onClearOutput = viewModel::clearOutput,
                                    onClearCommand = viewModel::clearCommand,
                                    onNavigateToHistory = {
                                        navController.navigate("history")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            ThemeMode.MIUIX -> {
                                MiuixHomeScreen(
                                    uiState = uiState,
                                    onToggleApp = viewModel::toggleAppSelection,
                                    onInputChange = viewModel::updateNaturalInput,
                                    onTranslate = viewModel::translateToCommand,
                                    onExecute = viewModel::executeCommand,
                                    onRequestPermission = viewModel::requestPermission,
                                    onClearOutput = viewModel::clearOutput,
                                    onClearCommand = viewModel::clearCommand,
                                    onNavigateToHistory = {
                                        navController.navigate("history")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                        }
                    }

                    composable("history") {
                        val historyViewModel: HistoryViewModel = viewModel()

                        when (themeMode) {
                            ThemeMode.MATERIAL3 -> {
                                Material3HistoryScreen(
                                    viewModel = historyViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            ThemeMode.MIUIX -> {
                                MiuixHistoryScreen(
                                    viewModel = historyViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    composable("settings") {
                        val viewModel: SettingsViewModel = viewModel()

                        val uiState by viewModel.uiState.collectAsState()

                        when (themeMode) {
                            ThemeMode.MATERIAL3 -> {
                                Material3SettingsScreen(
                                    uiState = uiState,
                                    onBaseUrlChange = viewModel::updateBaseUrl,
                                    onApiKeyChange = viewModel::updateApiKey,
                                    onThemeModeChange = { mode ->
                                        viewModel.updateThemeMode(mode)
                                        scope.launch {
                                            settingsStore.saveThemeMode(mode)
                                        }
                                    },
                                    onModelChange = viewModel::updateSelectedModel,
                                    onRefreshModels = viewModel::loadModels,
                                    onSave = {
                                        viewModel.saveSettings()
                                    },
                                    onBack = {
                                        // 返回主页时刷新 API 配置
                                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                                        navController.popBackStack()
                                    }
                                )
                            }
                            ThemeMode.MIUIX -> {
                                MiuixSettingsScreen(
                                    uiState = uiState,
                                    onBaseUrlChange = viewModel::updateBaseUrl,
                                    onApiKeyChange = viewModel::updateApiKey,
                                    onThemeModeChange = { mode ->
                                        viewModel.updateThemeMode(mode)
                                        scope.launch {
                                            settingsStore.saveThemeMode(mode)
                                        }
                                    },
                                    onModelChange = viewModel::updateSelectedModel,
                                    onRefreshModels = viewModel::loadModels,
                                    onSave = {
                                        viewModel.saveSettings()
                                    },
                                    onBack = {
                                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
