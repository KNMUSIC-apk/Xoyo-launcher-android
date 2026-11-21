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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.ContentKind
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.home.relativeTime
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Một mục đã cài trong thư mục nội dung của phiên bản */
private data class InstalledItem(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedMs: Long,
)

/** Thư mục trong game tương ứng với loại nội dung. Modpack không có thư mục riêng. */
private fun ContentKind.folder(): VersionFolders? = when (this) {
    ContentKind.MODS -> VersionFolders.MOD
    ContentKind.MODPACKS -> null
    ContentKind.RESOURCE_PACKS -> VersionFolders.RESOURCE_PACK
    ContentKind.SHADERS -> VersionFolders.SHADERS
}

/** Liệt kê thư mục nội dung của phiên bản, trên luồng IO. Không đệ quy để giữ nhanh. */
private fun scanInstalled(version: Version, kind: ContentKind): List<InstalledItem> {
    val folder = kind.folder() ?: return emptyList()
    val dir = File(version.getGameDir(), folder.folderName)
    val children = dir.listFiles().orEmpty()
        .filter { !it.name.startsWith(".") }
        .filter { kind != ContentKind.MODS || it.isFile }
    return children.map { file ->
        InstalledItem(
            name = file.name,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isFile) file.length() else 0L,
            modifiedMs = file.lastModified()
        )
    }.sortedBy { it.name.lowercase() }
}

/**
 * Trung tâm nội dung: mod, modpack, resource pack, shader.
 * Liệt kê những gì đã cài trong phiên bản đang chọn. Nút tải đưa đến trang tải tương ứng.
 */
@Composable
fun ContentCenterScreen(
    key: NormalNavKey.ContentCenter,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
) {
    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { _ ->
        val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
        val version = currentVersion?.takeIf { it.isValid() }

        var installed by remember { mutableStateOf<List<InstalledItem>?>(null) }

        LaunchedEffect(version, key.kind) {
            installed = if (version == null) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) { scanInstalled(version, key.kind) }
            }
        }

        val folder = key.kind.folder()
        val installTarget = when (key.kind) {
            ContentKind.MODS -> backScreenViewModel.downloadModScreen
            ContentKind.MODPACKS -> backScreenViewModel.downloadModPackScreen
            ContentKind.RESOURCE_PACKS -> backScreenViewModel.downloadResourcePackScreen
            ContentKind.SHADERS -> backScreenViewModel.downloadShadersScreen
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (version == null) {
                            Text(
                                text = stringResource(R.string.content_select_version),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.content_for_version, version.getVersionName()),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val count = installed?.size
                            if (count != null && folder != null) {
                                Text(
                                    text = stringResource(R.string.content_items_count, count),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { backScreenViewModel.navigateToDownload(installTarget) }) {
                                Text(stringResource(R.string.content_install))
                            }
                            if (version != null && folder != null) {
                                Button(onClick = {
                                    val dir = File(version.getGameDir(), folder.folderName)
                                    val path = if (dir.exists()) dir.absolutePath else version.getGameDir().absolutePath
                                    eventViewModel.sendEvent(EventViewModel.Event.OpenFileManager(rootPath = path))
                                }) {
                                    Text(stringResource(R.string.content_open_folder))
                                }
                            }
                        }
                    }
                }
            }

            if (folder == null) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(R.string.content_modpack_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                val list = installed
                when {
                    version == null -> Unit
                    list == null -> Unit
                    list.isEmpty() -> item {
                        Text(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            text = stringResource(R.string.content_empty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> items(list, key = { it.name }) { item ->
                        InstalledRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledRow(item: InstalledItem) {
    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(
                    if (item.isDirectory) R.drawable.ic_folder_outlined else R.drawable.ic_package_2_outlined
                ),
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        if (item.isDirectory) stringResource(R.string.content_folder) else formatFileSize(item.sizeBytes),
                        relativeTime(item.modifiedMs)
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
