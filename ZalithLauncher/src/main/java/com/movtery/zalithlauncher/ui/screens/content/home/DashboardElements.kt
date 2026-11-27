/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.Button
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.playhistory.PlayHistory
import com.movtery.zalithlauncher.game.playhistory.PlayRecord
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.ui.screens.main.NavDestination

/** Địa chỉ công khai dùng cho mục Khám phá. Đây là liên kết, không phải dữ liệu giả. */
const val MINECRAFT_NEWS_URL = "https://www.minecraft.net/en-us/article"
const val XOYO_RELEASES_URL = "https://github.com/KNMUSIC-apk/Xoyo-launcher-android/releases"

/**
 * Tổng quan trên trang chủ: thống kê, chọn phiên bản Minecraft, lịch sử chơi gần đây, khám phá.
 * Mọi số liệu đọc từ VersionsManager và PlayHistory. Không có dữ liệu thì hiển thị trạng thái rỗng.
 */
@Composable
fun DashboardOverview(
    modifier: Modifier = Modifier,
    onLaunchGame: (Version) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val records by PlayHistory.records.collectAsStateWithLifecycle()

    val recent = remember(versions, records) {
        versions.mapNotNull { version ->
            records[version.getVersionName()]
                ?.takeIf { it.sessions > 0 }
                ?.let { version to it }
        }.sortedByDescending { it.second.lastPlayedAt }.take(5)
    }
    val totalPlayMs = remember(records) { records.values.sumOf { it.totalPlayMs } }
    val lastPlayedAt = remember(records) { records.values.maxOfOrNull { it.lastPlayedAt } }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            VersionLaunchCard(
                versions = versions,
                current = currentVersion,
                onSelect = { version -> VersionsManager.saveVersion(version) },
                onLaunch = onLaunchGame,
                onManageVersions = { onNavigate(NavDestination.VERSIONS) }
            )
        }

        item {
            StatsCard(
                profileCount = versions.size,
                totalPlayMs = totalPlayMs,
                lastPlayedAt = lastPlayedAt
            )
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
}

/**
 * Thẻ chọn phiên bản Minecraft và khởi chạy, thay cho khối thao tác nhanh cũ.
 * Hiện chỉ có Java Edition; bản Rust Edition chưa có trong launcher này nên không hiển thị.
 */
@Composable
private fun VersionLaunchCard(
    versions: List<Version>,
    current: Version?,
    onSelect: (Version) -> Unit,
    onLaunch: (Version) -> Unit,
    onManageVersions: () -> Unit,
) {
    var showList by remember { mutableStateOf(false) }

    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.home_version_card_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Java Edition: lựa chọn duy nhất, hiển thị như một tab đang chọn
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_coffee_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(R.string.home_edition_java),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            if (versions.isEmpty()) onManageVersions() else showList = true
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VersionIconImage(
                        modifier = Modifier.size(36.dp),
                        version = current
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = current?.getVersionName()
                                ?: stringResource(R.string.versions_manage_no_versions),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = current?.takeIf { it.isValid() }?.getVersionSummary()
                                ?: stringResource(R.string.home_version_change),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                        contentDescription = stringResource(R.string.home_version_change)
                    )
                }

                DropdownMenu(
                    expanded = showList && versions.isNotEmpty(),
                    onDismissRequest = { showList = false },
                    modifier = Modifier.width(280.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    versions.forEach { version ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    VersionIconImage(
                                        modifier = Modifier.size(28.dp),
                                        version = version
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = version.getVersionName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                onSelect(version)
                                showList = false
                            }
                        )
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = current != null,
                shape = MaterialTheme.shapes.extraLarge,
                onClick = { current?.let(onLaunch) }
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_launch),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
