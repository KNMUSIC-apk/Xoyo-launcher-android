/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.legal.AvailableAgreement
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.SplashBackStackViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * @param startAllTask 开启全部的解压任务
 * @param onWelcomeFinished 完成欢迎页
 * @param onExitClick 架构不匹配屏幕的退出按钮
 * @param availableAgreement 完整的可用协议数据
 * @param unpackItems 解压任务列表
 * @param loadingState 启动初始化的真实进度与错误状态
 * @param onRetryLoading 启动失败后的重试回调
 */
@Composable
fun SplashScreen(
    startAllTask: () -> Unit,
    onWelcomeFinished: () -> Unit,
    onExitClick: () -> Unit = {},
    availableAgreement: StateFlow<AvailableAgreement?>,
    unpackItems: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel,
    loadingState: StateFlow<SplashLoadingState>,
    onRetryLoading: () -> Unit = {}
) {
    NavStateHost(rootKey = screenViewModel.splashScreen) {
        NavigationUI(
            modifier = Modifier.fillMaxSize(),
            startAllTask = startAllTask,
            onWelcomeFinished = onWelcomeFinished,
            onExitClick = onExitClick,
            availableAgreement = availableAgreement,
            unpackItems = unpackItems,
            screenViewModel = screenViewModel,
            loadingState = loadingState,
            onRetryLoading = onRetryLoading
        )
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    startAllTask: () -> Unit,
    onWelcomeFinished: () -> Unit,
    onExitClick: () -> Unit = {},
    availableAgreement: StateFlow<AvailableAgreement?>,
    unpackItems: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel,
    loadingState: StateFlow<SplashLoadingState>,
    onRetryLoading: () -> Unit = {}
) {
    val backStack = screenViewModel.splashScreen.backStack

    val currentKey = backStack.lastOrNull()
    LaunchedEffect(currentKey) {
        screenViewModel.splashScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = screenViewModel.splashScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {},
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<SplashOperation.Loading> {
                    LoadingScreen(
                        loadingState = loadingState,
                        onRetryLoading = onRetryLoading
                    )
                }
                entry<SplashOperation.Welcome> {
                    WelcomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onFinished = onWelcomeFinished,
                        availableAgreement = availableAgreement,
                        screenViewModel = screenViewModel
                    )
                }
                entry<SplashOperation.Unpacking> {
                    UnpackScreen(
                        items = unpackItems,
                        screenViewModel = screenViewModel,
                        onAgreeClick = startAllTask
                    )
                }
                entry<SplashOperation.ArchMismatch> {
                    ArchMismatchScreen(
                        screenViewModel = screenViewModel,
                        onExitClick = onExitClick,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

/**
 * XOYO: màn loading hiện ngay, không bọc trong BaseScreen và không trượt/hiện dần,
 * nên không còn khoảng chờ giữa logo và thanh tiến trình.
 */
@Composable
private fun LoadingScreen(
    loadingState: StateFlow<SplashLoadingState>,
    onRetryLoading: () -> Unit
) {
    val state by loadingState.collectAsStateWithLifecycle()

    SplashLoadingScreen(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onRetry = onRetryLoading
    )
}