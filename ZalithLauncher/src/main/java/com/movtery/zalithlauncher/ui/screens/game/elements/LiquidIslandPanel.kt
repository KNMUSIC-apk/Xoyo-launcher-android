/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.music.LiquidIsland
import com.movtery.zalithlauncher.music.LiquidLibrary
import com.movtery.zalithlauncher.music.LiquidSong
import com.movtery.zalithlauncher.music.rememberLiquidPlayback
import com.movtery.zalithlauncher.setting.IslandAnchor
import com.movtery.zalithlauncher.setting.LiquidIslandSettings
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidArtwork
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidIslandSurface
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidProgress
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidSongRow
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidTransport
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidVolume
import com.movtery.zalithlauncher.ui.screens.content.liquid.LiquidWaveform

private const val PANEL_TAB_SONGS = 0
private const val PANEL_TAB_FAVORITES = 1
private const val PANEL_TAB_RECENT = 2

private fun IslandAnchor.toAlignment(): Alignment = when (this) {
    IslandAnchor.TopStart -> Alignment.TopStart
    IslandAnchor.TopCenter -> Alignment.TopCenter
    IslandAnchor.TopEnd -> Alignment.TopEnd
    IslandAnchor.BottomStart -> Alignment.BottomStart
    IslandAnchor.BottomCenter -> Alignment.BottomCenter
    IslandAnchor.BottomEnd -> Alignment.BottomEnd
}

/**
 * Bảng Liquid Island trong game.
 *
 * Vẽ trong cùng lớp Compose với các lớp điều khiển của [com.movtery.zalithlauncher.ui.screens.game.GameScreen],
 * nên không mở Activity và không cần quyền cửa sổ nổi. Chỉ vùng bảng bắt sự kiện chạm,
 * phần còn lại vẫn rơi xuống lớp điều khiển của Minecraft như cũ.
 *
 * - Chạm ảnh bìa: mở/thu gọn thư viện
 * - Vuốt ngang ảnh bìa: bài trước / bài sau
 * - Giữ ảnh bìa: mở thư viện ở danh sách bài hát
 */
@Composable
fun LiquidIslandPanel(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    if (!LiquidIslandSettings.enabled.state) return

    val context = LocalContext.current
    val masterEnabled = LiquidIslandSettings.enabled.state
    val playerEnabled = LiquidIslandSettings.playerEnabled.state
    val animations = LiquidIslandSettings.animations.state && LiquidIslandSettings.enabled.state
    val playback = rememberLiquidPlayback(enabled = masterEnabled && playerEnabled)

    var libraryOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(PANEL_TAB_SONGS) }
    var songs by remember { mutableStateOf<List<LiquidSong>>(emptyList()) }
    var favorites by remember { mutableStateOf(emptySet<Long>()) }
    var recentIds by remember { mutableStateOf(emptyList<Long>()) }

    LaunchedEffect(Unit) {
        favorites = LiquidLibrary.favorites()
        recentIds = LiquidLibrary.recentIds()
        songs = LiquidLibrary.scan(context)
    }
    LaunchedEffect(playback.song?.id) {
        favorites = LiquidLibrary.favorites()
        recentIds = LiquidLibrary.recentIds()
    }

    val scalePercent = LiquidIslandSettings.panelScale.state / 100f
    val panelWidth = 330.dp * scalePercent
    val libraryWidth = 300.dp * scalePercent

    val openProgress by animateFloatAsState(
        targetValue = if (libraryOpen) 1f else 0f,
        animationSpec = if (animations) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
        label = "liquidIslandLibrary"
    )

    val shownSongs = when (tab) {
        PANEL_TAB_FAVORITES -> LiquidLibrary.pick(songs, favorites)
        PANEL_TAB_RECENT -> LiquidLibrary.pick(songs, recentIds)
        else -> LiquidLibrary.search(songs, query)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.align(LiquidIslandSettings.panelAnchor.state.toAlignment()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (openProgress > 0.01f) {
                LiquidIslandSurface(
                    modifier = Modifier
                        .width(libraryWidth)
                        .fillMaxHeight(0.78f)
                        .alpha(openProgress),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.liquid_search_hint)) },
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = null
                                )
                            }
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tabs = listOf(
                                R.string.liquid_tab_songs,
                                R.string.liquid_tab_favorites,
                                R.string.liquid_tab_recent
                            )
                            tabs.forEachIndexed { index, titleRes ->
                                FilterChip(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    label = {
                                        Text(
                                            text = stringResource(titleRes),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                        if (shownSongs.isEmpty()) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = stringResource(
                                    if (LiquidLibrary.canReadAudio(context)) {
                                        R.string.liquid_no_songs
                                    } else {
                                        R.string.liquid_no_permission
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
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
                                        onToggleFavorite = {
                                            favorites = LiquidLibrary.toggleFavorite(song.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LiquidIslandSurface(
                modifier = Modifier.width(panelWidth),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .pointerInput(playback.song?.id) {
                                        var dragged = 0f
                                        val threshold = 48.dp.toPx()
                                        detectHorizontalDragGestures(
                                            onDragStart = { dragged = 0f },
                                            onDragEnd = {
                                                when {
                                                    dragged > threshold -> LiquidIsland.previous(context)
                                                    dragged < -threshold -> LiquidIsland.next(context)
                                                }
                                            }
                                        ) { _, amount -> dragged += amount }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { libraryOpen = !libraryOpen },
                                            onLongPress = {
                                                tab = PANEL_TAB_SONGS
                                                libraryOpen = true
                                            }
                                        )
                                    }
                            ) {
                                LiquidArtwork(
                                    song = playback.song,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = playback.song?.title
                                        ?: stringResource(R.string.liquid_nothing_playing),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playback.song?.artist?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.liquid_pick_song),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LiquidWaveform(
                                    isPlaying = playback.isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .padding(top = 2.dp)
                                )
                            }
                        }

                        IconButton(onClick = onClose) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.liquid_close_panel)
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
                        compact = true,
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
    }
}
