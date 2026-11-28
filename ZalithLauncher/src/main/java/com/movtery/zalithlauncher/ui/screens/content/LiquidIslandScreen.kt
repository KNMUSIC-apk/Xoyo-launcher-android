/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.music.LiquidIsland
import com.movtery.zalithlauncher.music.LiquidLibrary
import com.movtery.zalithlauncher.music.LiquidPlaylist
import com.movtery.zalithlauncher.music.LiquidSong
import com.movtery.zalithlauncher.music.rememberLiquidPlayback
import com.movtery.zalithlauncher.setting.IslandAnchor
import com.movtery.zalithlauncher.setting.LiquidIslandSettings
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidArtwork
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidIslandSurface
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidProgress
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidSectionTitle
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidSongRow
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidTransport
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidVolume
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidWaveform
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.EnumSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/** Tab thư viện trong màn Liquid Island */
private const val TAB_SONGS = 0
private const val TAB_FAVORITES = 1
private const val TAB_RECENT = 2
private const val TAB_PLAYLISTS = 3

private fun anchorSymbol(anchor: IslandAnchor): String = when (anchor) {
    IslandAnchor.TopStart -> "↖"
    IslandAnchor.TopCenter -> "↑"
    IslandAnchor.TopEnd -> "↗"
    IslandAnchor.BottomStart -> "↙"
    IslandAnchor.BottomCenter -> "↓"
    IslandAnchor.BottomEnd -> "↘"
}

@Composable
fun LiquidIslandScreen(
    backStackViewModel: ScreenBackStackViewModel,
) {
    BaseScreen(
        screenKey = NormalNavKey.LiquidIsland,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val context = LocalContext.current

        val masterEnabled = LiquidIslandSettings.enabled.state
        val playerEnabled = LiquidIslandSettings.playerEnabled.state
        val overlayEnabled = LiquidIslandSettings.inGameOverlay.state
        val animations = LiquidIslandSettings.animations.state
        val playback = rememberLiquidPlayback(enabled = isVisible && masterEnabled && playerEnabled)

        var songs by remember { mutableStateOf<List<LiquidSong>>(emptyList()) }
        var hasAudioAccess by remember { mutableStateOf(false) }
        var reloadKey by remember { mutableIntStateOf(0) }
        var query by remember { mutableStateOf("") }
        var tab by remember { mutableIntStateOf(TAB_SONGS) }
        var favorites by remember { mutableStateOf(emptySet<Long>()) }
        var recentIds by remember { mutableStateOf(emptyList<Long>()) }
        var playlists by remember { mutableStateOf(emptyList<LiquidPlaylist>()) }

        val refreshUserData: () -> Unit = {
            favorites = LiquidLibrary.favorites()
            recentIds = LiquidLibrary.recentIds()
            playlists = LiquidLibrary.playlists()
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            hasAudioAccess = LiquidLibrary.canReadAudio(context)
            reloadKey++
        }
        val allFilesLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            hasAudioAccess = LiquidLibrary.canReadAudio(context)
            reloadKey++
        }
        val folderLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                LiquidIslandSettings.libraryFolder.save(uri.toString())
                reloadKey++
            }
        }

        val requestAccess: () -> Unit = {
            when {
                Build.VERSION.SDK_INT >= 33 -> permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager() -> {
                    runCatching {
                        allFilesLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
                else -> permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        LaunchedEffect(isVisible, reloadKey) {
            if (!isVisible) return@LaunchedEffect
            hasAudioAccess = LiquidLibrary.canReadAudio(context)
            refreshUserData()
            songs = if (hasAudioAccess) LiquidLibrary.scan(context) else emptyList()
        }

        // Bài hát đổi thì "vừa phát" cũng đổi theo
        LaunchedEffect(playback.song?.id) { refreshUserData() }

        val filteredSongs = remember(songs, query) { LiquidLibrary.search(songs, query) }
        val shownSongs = when (tab) {
            TAB_FAVORITES -> LiquidLibrary.pick(songs, favorites)
            TAB_RECENT -> LiquidLibrary.pick(songs, recentIds)
            else -> filteredSongs
        }
        val defaultPlaylistName = stringResource(R.string.liquid_playlist_default_name, playlists.size + 1)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            //Đang phát
            item {
                LiquidIslandSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiquidArtwork(
                                song = playback.song,
                                modifier = Modifier.size(84.dp),
                                shape = RoundedCornerShape(22.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp)
                            ) {
                                Text(
                                    text = playback.song?.title
                                        ?: stringResource(R.string.liquid_nothing_playing),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playback.song?.artist?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.liquid_pick_song),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LiquidWaveform(
                                    isPlaying = playback.isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .padding(top = 8.dp)
                                )
                            }
                        }

                        LiquidProgress(
                            playback = playback,
                            modifier = Modifier.fillMaxWidth(),
                            onSeek = { position -> LiquidIsland.seekTo(context, position) }
                        )
                        LiquidTransport(
                            playback = playback,
                            modifier = Modifier.fillMaxWidth(),
                            onShuffle = { LiquidIsland.toggleShuffle(context) },
                            onPrevious = { LiquidIsland.previous(context) },
                            onPlayPause = { LiquidIsland.playOrToggle(context) },
                            onNext = { LiquidIsland.next(context) },
                            onRepeat = { LiquidIsland.cycleRepeat(context) }
                        )
                        LiquidVolume(
                            volume = playback.volume,
                            modifier = Modifier.fillMaxWidth(),
                            onVolumeChange = { LiquidIsland.applyVolume(context, it) },
                            onVolumeCommit = { LiquidIsland.saveVolume(it) }
                        )
                    }
                }
            }

            //Thư viện nhạc
            item {
                BackgroundCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiquidSectionTitle(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.liquid_library)
                            )
                            IconButton(onClick = { reloadKey++ }) {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    painter = painterResource(R.drawable.ic_refresh),
                                    contentDescription = stringResource(R.string.liquid_refresh_library)
                                )
                            }
                            TextButton(onClick = {
                                folderLauncher.launch(null)
                            }) {
                                Text(stringResource(R.string.liquid_choose_folder))
                            }
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.liquid_search_hint)) },
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = null
                                )
                            }
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabs = listOf(
                                R.string.liquid_tab_songs,
                                R.string.liquid_tab_favorites,
                                R.string.liquid_tab_recent,
                                R.string.liquid_tab_playlists
                            )
                            tabs.forEachIndexed { index, titleRes ->
                                FilterChip(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    label = { Text(stringResource(titleRes)) }
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = when {
                                    !hasAudioAccess -> stringResource(R.string.liquid_no_permission)
                                    tab == TAB_PLAYLISTS -> stringResource(
                                        R.string.liquid_playlist_count,
                                        playlists.size
                                    )
                                    shownSongs.isEmpty() -> stringResource(R.string.liquid_no_songs)
                                    else -> stringResource(R.string.liquid_song_count, shownSongs.size)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tab != TAB_PLAYLISTS && shownSongs.isNotEmpty()) {
                                TextButton(onClick = { LiquidIsland.play(context, shownSongs, 0) }) {
                                    Text(stringResource(R.string.liquid_play_all))
                                }
                            }
                            if (!hasAudioAccess) {
                                TextButton(onClick = requestAccess) {
                                    Text(stringResource(R.string.liquid_grant_access))
                                }
                            }
                        }

                        if (LiquidIslandSettings.libraryFolder.state.isNotBlank()) {
                            Text(
                                text = stringResource(
                                    R.string.liquid_folder_current,
                                    LiquidIslandSettings.libraryFolder.state
                                        .substringAfterLast('/')
                                        .substringAfterLast(':')
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (tab == TAB_PLAYLISTS) {
                items(playlists, key = { it.name }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        songs = LiquidLibrary.pick(songs, playlist.songIds),
                        onPlay = { list -> LiquidIsland.play(context, list, 0) },
                        onDelete = {
                            LiquidLibrary.deletePlaylist(playlist.name)
                            playlists = LiquidLibrary.playlists()
                        }
                    )
                }
                item {
                    TextButton(
                        onClick = {
                            val queue = LiquidIsland.currentQueue()
                            if (queue.isNotEmpty()) {
                                LiquidLibrary.savePlaylist(defaultPlaylistName, queue.map { it.id })
                                playlists = LiquidLibrary.playlists()
                            }
                        },
                        enabled = playback.count > 0
                    ) {
                        Text(stringResource(R.string.liquid_save_queue))
                    }
                }
            } else {
                items(shownSongs, key = { it.id }) { song ->
                    LiquidSongRow(
                        song = song,
                        isCurrent = playback.song?.id == song.id,
                        isFavorite = favorites.contains(song.id),
                        onPlay = {
                            if (playback.song?.id == song.id) {
                                LiquidIsland.playOrToggle(context)
                            } else {
                                LiquidIsland.play(
                                    context,
                                    shownSongs,
                                    shownSongs.indexOf(song).coerceAtLeast(0)
                                )
                            }
                        },
                        onToggleFavorite = { favorites = LiquidLibrary.toggleFavorite(song.id) }
                    )
                }
            }

            //Cài đặt
            item {
                LiquidSectionTitle(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(R.string.liquid_settings)
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.enabled,
                    title = stringResource(R.string.liquid_master_switch),
                    summary = stringResource(R.string.liquid_master_switch_summary),
                    position = CardPosition.Single
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.playerEnabled,
                    title = stringResource(R.string.liquid_player_switch),
                    summary = stringResource(R.string.liquid_player_switch_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.inGameOverlay,
                    title = stringResource(R.string.liquid_overlay_switch),
                    summary = stringResource(R.string.liquid_overlay_switch_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.autoShowOnPlay,
                    title = stringResource(R.string.liquid_auto_show_switch),
                    summary = stringResource(R.string.liquid_auto_show_switch_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled && overlayEnabled
                )
            }

            item {
                EnumSettingsCard(
                    unit = LiquidIslandSettings.panelAnchor,
                    entries = IslandAnchor.entries,
                    title = stringResource(R.string.liquid_position),
                    summary = stringResource(R.string.liquid_position_summary),
                    position = CardPosition.Single,
                    getRadioText = { anchor -> anchorSymbol(anchor) },
                    getRadioEnable = { masterEnabled }
                )
            }

            item {
                IntSliderSettingsCard(
                    unit = LiquidIslandSettings.panelScale,
                    title = stringResource(R.string.liquid_size),
                    summary = stringResource(R.string.liquid_size_summary),
                    position = CardPosition.Single,
                    valueRange = 70f..130f,
                    suffix = "%",
                    enabled = masterEnabled
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.animations,
                    title = stringResource(R.string.liquid_animations),
                    summary = stringResource(R.string.liquid_animations_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.liquidGlass,
                    title = stringResource(R.string.liquid_glass),
                    summary = stringResource(R.string.liquid_glass_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.waveform,
                    title = stringResource(R.string.liquid_waveform),
                    summary = stringResource(R.string.liquid_waveform_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled && animations
                )
            }

            item {
                IntSliderSettingsCard(
                    unit = LiquidIslandSettings.opacity,
                    title = stringResource(R.string.liquid_opacity),
                    position = CardPosition.Single,
                    valueRange = 40f..100f,
                    suffix = "%",
                    enabled = masterEnabled
                )
            }

            item {
                IntSliderSettingsCard(
                    unit = LiquidIslandSettings.blur,
                    title = stringResource(R.string.liquid_blur),
                    summary = stringResource(R.string.liquid_blur_summary),
                    position = CardPosition.Single,
                    valueRange = 0f..40f,
                    enabled = masterEnabled
                )
            }

            item {
                IntSliderSettingsCard(
                    unit = LiquidIslandSettings.minDurationSeconds,
                    title = stringResource(R.string.liquid_min_duration),
                    summary = stringResource(R.string.liquid_min_duration_summary),
                    position = CardPosition.Single,
                    valueRange = 0f..300f,
                    suffix = "s",
                    enabled = masterEnabled
                )
            }

            item {
                SwitchSettingsCard(
                    unit = LiquidIslandSettings.backgroundPlayback,
                    title = stringResource(R.string.liquid_background_playback),
                    summary = stringResource(R.string.liquid_background_playback_summary),
                    position = CardPosition.Single,
                    enabled = masterEnabled
                )
            }

            item {
                BackgroundCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.liquid_spotify_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.liquid_spotify_body),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://open.spotify.com")
                                            )
                                        )
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.liquid_spotify_open))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: LiquidPlaylist,
    songs: List<LiquidSong>,
    onPlay: (List<LiquidSong>) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.liquid_song_count, songs.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { onPlay(songs) }, enabled = songs.isNotEmpty()) {
            Text(stringResource(R.string.liquid_play))
        }
        IconButton(onClick = onDelete) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.liquid_delete_playlist)
            )
        }
    }
}
