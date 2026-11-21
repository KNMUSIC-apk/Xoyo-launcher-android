/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.playhistory.PlayHistory
import com.movtery.zalithlauncher.game.playhistory.PlayRecord
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.ui.screens.content.home.formatPlayDuration
import com.movtery.zalithlauncher.ui.screens.content.home.relativeTime
import com.movtery.zalithlauncher.ui.screens.main.NavDestination
import com.movtery.zalithlauncher.ui.screens.main.navigateToDestination
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/** Cách sắp xếp Thư viện game */
enum class LibrarySort(val titleRes: Int) {
    LAST_PLAYED(R.string.library_sort_last_played),
    PLAYTIME(R.string.library_sort_playtime),
    NAME(R.string.library_sort_name)
}

/**
 * Thuần: lọc theo tên rồi sắp xếp.
 * Phiên bản chưa chơi được xếp sau khi sắp theo lần chơi gần nhất hoặc thời gian chơi.
 */
fun libraryOrder(
    versions: List<Version>,
    records: Map<String, PlayRecord>,
    query: String,
    sort: LibrarySort,
): List<Version> {
    val needle = query.trim()
    val filtered = if (needle.isEmpty()) {
        versions
    } else {
        versions.filter { it.getVersionName().contains(needle, ignoreCase = true) }
    }
    return when (sort) {
        LibrarySort.NAME -> filtered.sortedBy { it.getVersionName().lowercase() }
        LibrarySort.LAST_PLAYED -> filtered.sortedWith(
            compareByDescending<Version> { records[it.getVersionName()]?.lastPlayedAt ?: 0L }
                .thenBy { it.getVersionName().lowercase() }
        )
        LibrarySort.PLAYTIME -> filtered.sortedWith(
            compareByDescending<Version> { records[it.getVersionName()]?.totalPlayMs ?: 0L }
                .thenBy { it.getVersionName().lowercase() }
        )
    }
}

/**
 * Thư viện game: mọi phiên bản dưới dạng thẻ hoặc danh sách, có tìm kiếm, sắp xếp và thống kê.
 * Số liệu thời gian chơi lấy từ PlayHistory, ghi nhận khi game chạy từ launcher.
 */
@Composable
fun GameLibraryScreen(
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    navigateToVersions: (Version) -> Unit,
) {
    BaseScreen(
        screenKey = NormalNavKey.GameLibrary,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { _ ->
        val versions by VersionsManager.versions.collectAsStateWithLifecycle()
        val records by PlayHistory.records.collectAsStateWithLifecycle()

        var query by rememberSaveable { mutableStateOf("") }
        var sort by rememberSaveable { mutableStateOf(LibrarySort.LAST_PLAYED) }
        var asGrid by rememberSaveable { mutableStateOf(true) }

        val shown = remember(versions, records, query, sort) {
            libraryOrder(versions, records, query, sort)
        }
        val totalPlayMs = remember(records) { records.values.sumOf { it.totalPlayMs } }
        val totalSessions = remember(records) { records.values.sumOf { it.sessions } }

        val onPlay: (Version) -> Unit = { version ->
            eventViewModel.sendEvent(EventViewModel.Event.Launch.Game(version))
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackgroundCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBlock(
                        value = versions.size.toString(),
                        label = stringResource(R.string.dash_stats_profiles)
                    )
                    StatBlock(
                        value = if (totalPlayMs > 0L) formatPlayDuration(totalPlayMs) else "—",
                        label = stringResource(R.string.dash_stats_playtime)
                    )
                    StatBlock(
                        value = totalSessions.toString(),
                        label = stringResource(R.string.library_stat_sessions)
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.library_search_hint)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibrarySort.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(stringResource(option.titleRes)) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                FilterChip(
                    selected = asGrid,
                    onClick = { asGrid = true },
                    label = { Text(stringResource(R.string.library_view_grid)) }
                )
                FilterChip(
                    selected = !asGrid,
                    onClick = { asGrid = false },
                    label = { Text(stringResource(R.string.library_view_list)) }
                )
            }

            when {
                versions.isEmpty() -> EmptyLibrary(
                    text = stringResource(R.string.library_empty),
                    actionText = stringResource(R.string.library_open_versions),
                    onAction = { backStackViewModel.navigateToDestination(NavDestination.VERSIONS) }
                )
                shown.isEmpty() -> Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = stringResource(R.string.library_no_match),
                    style = MaterialTheme.typography.bodyMedium
                )
                asGrid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 170.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.getVersionName() }) { version ->
                        LibraryCard(
                            version = version,
                            record = records[version.getVersionName()],
                            onPlay = { onPlay(version) },
                            onOpen = { navigateToVersions(version) }
                        )
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shown, key = { it.getVersionName() }) { version ->
                        LibraryRow(
                            version = version,
                            record = records[version.getVersionName()],
                            onPlay = { onPlay(version) },
                            onOpen = { navigateToVersions(version) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun LibrarySummary(record: PlayRecord?): String {
    return if (record == null || record.sessions == 0) {
        stringResource(R.string.library_never_played)
    } else {
        listOf(
            stringResource(R.string.dash_last_played, relativeTime(record.lastPlayedAt)),
            formatPlayDuration(record.totalPlayMs)
        ).joinToString("  ·  ")
    }
}

@Composable
private fun LibraryCard(
    version: Version,
    record: PlayRecord?,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VersionIconImage(
                modifier = Modifier.size(56.dp),
                version = version
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = version.getVersionName(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = LibrarySummary(record),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onPlay) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = stringResource(R.string.main_launch_game),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LibraryRow(
    version: Version,
    record: PlayRecord?,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VersionIconImage(
                modifier = Modifier.size(44.dp),
                version = version
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.getVersionName(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = LibrarySummary(record),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = stringResource(R.string.main_launch_game),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(
    text: String,
    actionText: String,
    onAction: () -> Unit,
) {
    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAction) {
                Text(text = actionText)
            }
        }
    }
}
