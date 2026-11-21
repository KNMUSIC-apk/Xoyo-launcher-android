/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.playhistory.PlayHistory
import com.movtery.zalithlauncher.game.playhistory.PlayRecord
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.ui.screens.main.NavDestination

/** Các thao tác nhanh trên Tổng quan. Người dùng có thể bật, tắt và sắp xếp lại. */
enum class QuickAction(val iconRes: Int, val titleRes: Int) {
    CREATE_PROFILE(R.drawable.ic_add_box_filled, R.string.qa_create_profile),
    MANAGE_VERSIONS(R.drawable.ic_box, R.string.qa_manage_versions),
    INSTALL_MODS(R.drawable.ic_extension_outlined, R.string.qa_install_mods),
    INSTALL_SHADERS(R.drawable.ic_lightbulb, R.string.qa_install_shaders),
    INSTALL_RESOURCE_PACKS(R.drawable.ic_photo_library_outlined, R.string.qa_install_resource_packs),
    MANAGE_WORLDS(R.drawable.ic_public, R.string.qa_manage_worlds),
    PERFORMANCE(R.drawable.ic_dashboard_outlined, R.string.qa_performance),
    OPEN_GAME_FOLDER(R.drawable.ic_folder_filled, R.string.qa_open_folder)
}

/** Đọc danh sách thao tác đã bật. Tên không hợp lệ và trùng lặp sẽ bị bỏ qua. */
fun parseQuickActions(raw: String): List<QuickAction> = raw
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .mapNotNull { name -> runCatching { QuickAction.valueOf(name) }.getOrNull() }
    .distinct()

fun encodeQuickActions(actions: List<QuickAction>): String = actions.joinToString(",") { it.name }

/** Địa chỉ công khai dùng cho mục Khám phá. Đây là liên kết, không phải dữ liệu giả. */
const val MINECRAFT_NEWS_URL = "https://www.minecraft.net/en-us/article"
const val XOYO_RELEASES_URL = "https://github.com/KNMUSIC-apk/Xoyo-launcher-android/releases"

/**
 * Tổng quan trên trang chủ: thống kê, thao tác nhanh, lịch sử chơi gần đây, khám phá.
 * Mọi số liệu đọc từ VersionsManager và PlayHistory. Không có dữ liệu thì hiển thị trạng thái rỗng.
 */
@Composable
fun DashboardOverview(
    modifier: Modifier = Modifier,
    onLaunchGame: (Version) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val records by PlayHistory.records.collectAsStateWithLifecycle()
    val rawQuickActions = LauncherSettings.launcherQuickActions.state
    val quickActions = remember(rawQuickActions) { parseQuickActions(rawQuickActions) }

    val recent = remember(versions, records) {
        versions.mapNotNull { version ->
            records[version.getVersionName()]
                ?.takeIf { it.sessions > 0 }
                ?.let { version to it }
        }.sortedByDescending { it.second.lastPlayedAt }.take(5)
    }
    val totalPlayMs = remember(records) { records.values.sumOf { it.totalPlayMs } }
    val lastPlayedAt = remember(records) { records.values.maxOfOrNull { it.lastPlayedAt } }

    var showEdit by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            StatsCard(
                profileCount = versions.size,
                totalPlayMs = totalPlayMs,
                lastPlayedAt = lastPlayedAt
            )
        }

        item {
            SectionCard(
                title = stringResource(R.string.dash_quick_actions),
                trailing = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_outlined),
                            contentDescription = stringResource(R.string.dash_quick_actions_edit)
                        )
                    }
                }
            ) {
                if (quickActions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dash_quick_actions_empty),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    quickActions.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { action ->
                                QuickActionTile(
                                    modifier = Modifier.weight(1f),
                                    action = action,
                                    onClick = { onQuickAction(action) }
                                )
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.dash_recent)) {
                if (recent.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dash_recent_empty),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    recent.forEach { (version, record) ->
                        RecentItem(
                            version = version,
                            record = record,
                            onLaunch = { onLaunchGame(version) },
                            onSettings = { onOpenVersionSettings(version) }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.dash_discovery)) {
                DiscoveryRow(
                    text = stringResource(R.string.dash_discovery_content),
                    iconRes = R.drawable.ic_extension_outlined,
                    onClick = { onNavigate(NavDestination.MODS) }
                )
                DiscoveryRow(
                    text = stringResource(R.string.dash_discovery_news),
                    iconRes = R.drawable.ic_article_outlined,
                    onClick = { onOpenLink(MINECRAFT_NEWS_URL) }
                )
                DiscoveryRow(
                    text = stringResource(R.string.dash_discovery_updates),
                    iconRes = R.drawable.ic_update,
                    onClick = { onOpenLink(XOYO_RELEASES_URL) }
                )
                Text(
                    text = stringResource(R.string.dash_discovery_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showEdit) {
        QuickActionsEditDialog(
            enabled = quickActions,
            onSave = { selected ->
                LauncherSettings.launcherQuickActions.save(encodeQuickActions(selected))
                showEdit = false
            },
            onDismiss = { showEdit = false }
        )
    }
}

@Composable
private fun StatsCard(
    profileCount: Int,
    totalPlayMs: Long,
    lastPlayedAt: Long?,
) {
    SectionCard(title = stringResource(R.string.dash_stats_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBlock(
                modifier = Modifier.weight(1f),
                value = profileCount.toString(),
                label = stringResource(R.string.dash_stats_profiles)
            )
            StatBlock(
                modifier = Modifier.weight(1f),
                value = if (totalPlayMs > 0L) formatPlayDuration(totalPlayMs) else "—",
                label = stringResource(R.string.dash_stats_playtime)
            )
            StatBlock(
                modifier = Modifier.weight(1f),
                value = lastPlayedAt?.takeIf { it > 0L }?.let { relativeTime(it) } ?: "—",
                label = stringResource(R.string.dash_stats_last_played)
            )
        }
    }
}

@Composable
private fun StatBlock(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    action: QuickAction,
    onClick: () -> Unit,
) {
    val title = stringResource(action.titleRes)
    BackgroundCard(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(action.iconRes),
                contentDescription = null
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentItem(
    version: Version,
    record: PlayRecord,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VersionIconImage(
            modifier = Modifier.size(40.dp),
            version = version
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = version.getVersionName(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(
                    stringResource(R.string.dash_last_played, relativeTime(record.lastPlayedAt)),
                    stringResource(R.string.dash_play_time, formatPlayDuration(record.totalPlayMs)),
                    stringResource(R.string.dash_sessions, record.sessions)
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_filled),
                contentDescription = stringResource(R.string.dash_open_settings)
            )
        }
        IconButton(onClick = onLaunch) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow_filled),
                contentDescription = stringResource(R.string.main_launch_game),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DiscoveryRow(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = null
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_arrow_right_rounded),
            contentDescription = null
        )
    }
}

@Composable
private fun QuickActionsEditDialog(
    enabled: List<QuickAction>,
    onSave: (List<QuickAction>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dash_quick_actions_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (draft + QuickAction.entries.filter { it !in draft }).forEach { action ->
                    val index = draft.indexOf(action)
                    val isOn = index >= 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isOn,
                            onCheckedChange = { checked ->
                                draft = if (checked) draft + action else draft - action
                            }
                        )
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(action.iconRes),
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            text = stringResource(action.titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOn) {
                            IconButton(
                                enabled = index > 0,
                                onClick = { draft = draft.moved(index, index - 1) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                                    contentDescription = null
                                )
                            }
                            IconButton(
                                enabled = index < draft.lastIndex,
                                onClick = { draft = draft.moved(index, index + 1) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(text = stringResource(R.string.generic_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.generic_cancel))
            }
        }
    )
}

private fun List<QuickAction>.moved(from: Int, to: Int): List<QuickAction> =
    toMutableList().apply { add(to, removeAt(from)) }

/** Thời gian chơi dạng "2 h 15 min" hoặc "40 min" */
@Composable
fun formatPlayDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val hours = (totalMinutes / 60L).toInt()
    val minutes = (totalMinutes % 60L).toInt()
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

/** Thời điểm tương đối, ví dụ "2 hours ago", theo ngôn ngữ của hệ thống */
fun relativeTime(epochMs: Long): String = DateUtils.getRelativeTimeSpanString(
    epochMs,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
    DateUtils.FORMAT_ABBREV_RELATIVE
).toString()
