/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import com.movtery.zalithlauncher.ui.components.Button
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardPingStatus
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardState
import com.movtery.zalithlauncher.ui.screens.main.NavDestination
import com.movtery.zalithlauncher.ui.screens.main.navigateToDestination
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast

/**
 * Trung tâm máy chủ.
 * Chỉ hiển thị các máy chủ người dùng đã thêm. Trạng thái là kết quả ping thật tới máy chủ,
 * không có dữ liệu mẫu.
 */
@Composable
fun ServerHubScreen(
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
) {
    BaseScreen(
        screenKey = NormalNavKey.ServerHub,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val cards by ServerCardManager.cards.collectAsStateWithLifecycle()
        val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()

        var showAdd by remember { mutableStateOf(false) }
        var pendingDelete by remember { mutableStateOf<ServerCardState?>(null) }

        // Chỉ ping khi màn hình đang hiển thị
        LaunchedEffect(isVisible, cards) {
            ServerCardManager.onVisibleCardsChanged(
                if (isVisible) cards.map { it.record.cardId }.toSet() else emptySet()
            )
        }
        DisposableEffect(Unit) {
            onDispose { ServerCardManager.onVisibleCardsChanged(emptySet()) }
        }

        val play: (String) -> Unit = { address ->
            val target = currentVersion?.takeIf { it.isValid() }
            if (target != null) {
                eventViewModel.sendEvent(EventViewModel.Event.Launch.PlayServer(target, address))
            } else {
                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                backStackViewModel.navigateToDestination(NavDestination.VERSIONS)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                BackgroundCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hub_server_intro),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = { showAdd = true }) {
                            Text(stringResource(R.string.hub_server_add))
                        }
                    }
                }
            }

            if (cards.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(R.string.hub_server_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(cards, key = { it.record.cardId }) { state ->
                ServerRow(
                    state = state,
                    onQuickLaunchChange = { enabled ->
                        ServerCardManager.setQuickLaunchEnabled(state.record.cardId, enabled)
                    },
                    onRefresh = {
                        ServerCardManager.refresh(state.record.cardId, manual = true)
                    },
                    onPlay = { play(state.record.cardId) },
                    onDelete = { pendingDelete = state }
                )
            }
        }

        if (showAdd) {
            AddServerDialog(
                onDismiss = { showAdd = false },
                onAdd = { name, address ->
                    val created = ServerCardManager.addCard(name, address)
                    if (created) {
                        showAdd = false
                    } else {
                        eventViewModel.sendToast(androidText(R.string.hub_server_add_failed))
                    }
                }
            )
        }

        pendingDelete?.let { state ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.hub_server_delete_title)) },
                text = {
                    Text(stringResource(R.string.hub_server_delete_message, state.record.name))
                },
                confirmButton = {
                    TextButton(onClick = {
                        ServerCardManager.removeCard(state.record.cardId)
                        pendingDelete = null
                    }) {
                        Text(stringResource(R.string.hub_server_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.generic_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ServerRow(
    state: ServerCardState,
    onQuickLaunchChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val pingText = when (val ping = state.ping) {
        ServerCardPingStatus.Loading -> stringResource(R.string.hub_server_ping_loading)
        ServerCardPingStatus.Failed -> stringResource(R.string.hub_server_ping_failed)
        is ServerCardPingStatus.Loaded -> stringResource(R.string.hub_server_ping_ms, ping.result.pingMs)
    }

    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.record.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.record.cardId,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.hub_server_refresh)
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow_filled),
                        contentDescription = stringResource(R.string.hub_server_play),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outlined),
                        contentDescription = stringResource(R.string.hub_server_delete)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.hub_server_quick_launch),
                    style = MaterialTheme.typography.bodySmall
                )
                Switch(
                    checked = state.record.quickLaunchEnabled,
                    onCheckedChange = onQuickLaunchChange
                )
            }
        }
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, address: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val valid = address.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hub_server_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.hub_server_name)) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.hub_server_address)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val trimmed = address.trim()
                    onAdd(name.trim().ifEmpty { trimmed }, trimmed)
                }
            ) {
                Text(stringResource(R.string.generic_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_cancel))
            }
        }
    )
}
