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
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.world.backupWorldToZip
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.viewmodel.sendToast
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.version.saves.SaveData
import com.movtery.zalithlauncher.game.version.saves.parseLevelDatFile
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.home.formatPlayDuration
import com.movtery.zalithlauncher.ui.screens.content.home.relativeTime
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Một thế giới đã đọc từ level.dat, kèm phiên bản chứa nó */
private data class WorldEntry(
    val versionName: String,
    val data: SaveData,
    /** Thư mục game (.minecraft) của phiên bản, nơi đặt thư mục sao lưu */
    val gameDir: File,
)

/**
 * Quản lý thế giới của mọi phiên bản.
 * Đọc level.dat thật. Xoá thế giới luôn cần xác nhận. Chưa có sao lưu và nhập thế giới.
 */
@Composable
fun WorldManagerScreen(
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
) {
    BaseScreen(
        screenKey = NormalNavKey.WorldManager,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { _ ->
        val versions by VersionsManager.versions.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        //XOYO: sao lưu thế giới thành zip trong thư mục world_backups của phiên bản
        val backupWorld: (WorldEntry) -> Unit = { entry ->
            scope.launch {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        backupWorldToZip(
                            worldDir = entry.data.saveFile,
                            targetDir = File(entry.gameDir, "world_backups"),
                            timestamp = timestamp
                        )
                    }
                }
                val message = result.fold(
                    onSuccess = { context.getString(R.string.world_backup_success, it.absolutePath) },
                    onFailure = { context.getString(R.string.world_backup_failed) }
                )
                eventViewModel.sendToast(androidText(message), Toast.LENGTH_LONG)
            }
        }

        var worlds by remember { mutableStateOf<List<WorldEntry>?>(null) }
        var reloadKey by remember { mutableStateOf(0) }
        var query by rememberSaveable { mutableStateOf("") }
        var pendingDelete by remember { mutableStateOf<WorldEntry?>(null) }

        LaunchedEffect(versions, reloadKey) {
            worlds = null
            worlds = withContext(Dispatchers.IO) { scanWorlds(versions) }
        }

        val loaded = worlds
        val shown = remember(loaded, query) {
            val needle = query.trim()
            loaded?.filter {
                needle.isEmpty() || worldTitle(it).contains(needle, ignoreCase = true)
            }.orEmpty()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.world_search_hint)) }
                )
            }

            when {
                loaded == null -> item {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(R.string.world_loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                loaded.isEmpty() -> item {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(R.string.world_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                shown.isEmpty() -> item {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(R.string.world_no_match),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> items(shown, key = { it.data.saveFile.absolutePath }) { entry ->
                    WorldCard(
                        entry = entry,
                        onOpenFolder = {
                            eventViewModel.sendEvent(
                                EventViewModel.Event.OpenFileManager(
                                    rootPath = entry.data.saveFile.absolutePath
                                )
                            )
                        },
                        onDelete = { pendingDelete = entry },
                        onBackup = { backupWorld(entry) }
                    )
                }
            }
        }

        pendingDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.world_delete_title)) },
                text = {
                    Text(stringResource(R.string.world_delete_message, worldTitle(entry)))
                },
                confirmButton = {
                    TextButton(onClick = {
                        val target = entry
                        pendingDelete = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                target.data.saveFile.deleteRecursively()
                            }
                            reloadKey++
                        }
                    }) {
                        Text(stringResource(R.string.world_delete))
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

private fun worldTitle(entry: WorldEntry): String =
    entry.data.levelName?.takeIf { it.isNotBlank() } ?: entry.data.saveFile.name

/** Quét mọi thư mục saves của các phiên bản, trên luồng IO */
private suspend fun scanWorlds(versions: List<Version>): List<WorldEntry> {
    val result = mutableListOf<WorldEntry>()
    for (version in versions) {
        val savesDir = File(version.getGameDir(), VersionFolders.SAVES.folderName)
        val dirs = savesDir.listFiles { file -> file.isDirectory }.orEmpty()
        for (dir in dirs) {
            val levelDat = File(dir, "level.dat")
            if (!levelDat.isFile) continue
            val data = parseLevelDatFile(
                saveFile = dir,
                levelDatFile = levelDat,
                worldGenDatFile = File(dir, "data/minecraft/world_gen_settings.dat")
            )
            result += WorldEntry(versionName = version.getVersionName(), data = data, gameDir = version.getGameDir())
        }
    }
    return result.sortedByDescending { it.data.lastPlayed ?: 0L }
}

@Composable
private fun WorldCard(
    entry: WorldEntry,
    onOpenFolder: () -> Unit,
    onDelete: () -> Unit,
    onBackup: () -> Unit,
) {
    val data = entry.data
    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = worldTitle(entry),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.world_version, entry.versionName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!data.isValid) {
                    Text(
                        text = stringResource(R.string.world_invalid),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    val lastPlayed = data.lastPlayed?.takeIf { it > 0L }
                    Text(
                        text = listOfNotNull(
                            lastPlayed?.let { stringResource(R.string.dash_last_played, relativeTime(it)) },
                            data.levelMCVersion?.let { stringResource(R.string.world_mc_version, it) }
                        ).joinToString("  ·  ").ifEmpty { stringResource(R.string.world_no_details) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    data.playTime?.takeIf { it > 0L }?.let { playMs ->
                        Text(
                            text = stringResource(R.string.world_playtime, formatPlayDuration(playMs * 50L)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onOpenFolder) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder_outlined),
                    contentDescription = stringResource(R.string.world_open_folder)
                )
            }
            IconButton(onClick = onBackup) {
                Icon(
                    painter = painterResource(R.drawable.ic_archive_filled),
                    contentDescription = stringResource(R.string.world_backup_action)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.world_delete)
                )
            }
        }
    }
}
