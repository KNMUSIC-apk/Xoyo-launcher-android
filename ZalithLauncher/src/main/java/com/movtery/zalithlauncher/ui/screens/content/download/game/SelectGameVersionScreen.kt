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

package com.movtery.zalithlauncher.ui.screens.content.download.game

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.CheckChip
import com.movtery.zalithlauncher.ui.components.EdgeDirection
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.PullToRefreshLayout
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.classes.Quadruple
import com.movtery.zalithlauncher.utils.formatDate
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.toLocal
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private const val TAG = "SelectGameVersion"

/** 版本列表加载状态 */
private sealed interface VersionState {
    /** 加载中 */
    data object Loading : VersionState
    /** 加载完成 */
    data class None(val groups: List<VersionGroup>) : VersionState
    /** 加载出现异常 */
    data class Failure(val message: AndroidStringText) : VersionState
}

/**
 * Nhóm phiên bản theo dòng, ví dụ 1.21, 1.20, 1.19.
 * Danh sách bên trong đã sắp xếp mới nhất trước.
 */
private data class VersionGroup(
    val key: String,
    val versions: List<MinecraftVersion>
) {
    val latest: MinecraftVersion get() = versions.first()
}

/** Loại phiên bản đang chọn: mỗi lần chỉ xem một loại, giống thanh danh mục trong ảnh tham khảo */
private enum class VersionCategory(val labelRes: Int) {
    RELEASE(R.string.download_game_type_release),
    SNAPSHOT(R.string.download_game_type_snapshot),
    APRIL_FOOLS(R.string.download_game_type_april_fools),
    OLD(R.string.download_game_type_old)
}

/**
 * 版本过滤条件
 * @param category 版本类型
 * @param id 搜索并过滤版本ID
 */
private data class VersionFilter(
    val category: VersionCategory = VersionCategory.RELEASE,
    val id: String = ""
)

private class VersionsViewModel: ViewModel() {
    var versionState by mutableStateOf<VersionState>(VersionState.Loading)
        private set

    //简易版本类型过滤器
    var versionFilter by mutableStateOf(VersionFilter())
        private set

    fun filterWith(filter: VersionFilter) {
        versionFilter = filter
        viewModelScope.launch {
            val allVersions = MinecraftVersions.allVersions.value
            versionState = VersionState.None(
                groups = allVersions.groupVersions(versionFilter)
            )
        }
    }

    fun refresh(forceReload: Boolean = false) {
        viewModelScope.launch {
            versionState = VersionState.Loading
            versionState = runCatching {
                MinecraftVersions.refreshVersions(forceReload)
                val allVersions = MinecraftVersions.allVersions.value
                VersionState.None(allVersions.groupVersions(versionFilter))
            }.getOrElse { e ->
                Logger.warning(TAG, "Failed to get version manifest!", e)
                val message: AndroidStringText = when(e) {
                    is HttpRequestTimeoutException -> androidText(R.string.error_timeout)
                    is UnknownHostException, is UnresolvedAddressException -> androidText(R.string.error_network_unreachable)
                    is ConnectException -> androidText(R.string.error_connection_failed)
                    is ResponseException -> e.toLocal()
                    else -> {
                        Logger.error(TAG, "An unknown exception was caught!", e)
                        androidText(e.localizedMessage ?: e.message ?: e::class.qualifiedName ?: "Unknown error")
                    }
                }
                VersionState.Failure(message)
            }
        }
    }

    init {
        //初始化后，刷新版本列表
        refresh()
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

/**
 * 选择 Minecraft 版本屏幕的宿主信息，提供可见性判定所需的导航层级
 */
sealed interface SelectGameVersionHost {
    /** 下载游戏流程 */
    data class Download(
        val mainScreenKey: TitledNavKey?,
        val downloadScreenKey: TitledNavKey?,
        val downloadGameScreenKey: TitledNavKey?
    ) : SelectGameVersionHost

    /** 版本设置流程（修改版本） */
    data class VersionSettings(
        val mainScreenKey: TitledNavKey?,
        val versionSettingsScreenKey: TitledNavKey?
    ) : SelectGameVersionHost
}

@Composable
fun SelectGameVersionScreen(
    host: SelectGameVersionHost,
    eventViewModel: EventViewModel,
    onVersionSelect: (MinecraftVersion) -> Unit = {}
) {
    val viewModel = viewModel(
        key = NormalNavKey.DownloadGame.SelectGameVersion.toString()
    ) {
        VersionsViewModel()
    }

    when (host) {
        is SelectGameVersionHost.Download -> BaseScreen(
            levels1 = listOf(
                Pair(NestedNavKey.Download::class.java, host.mainScreenKey),
                Pair(NestedNavKey.DownloadGame::class.java, host.downloadScreenKey)
            ),
            Triple(NormalNavKey.DownloadGame.SelectGameVersion, host.downloadGameScreenKey, false)
        ) { isVisible ->
            SelectGameVersionContent(
                isVisible = isVisible,
                viewModel = viewModel,
                eventViewModel = eventViewModel,
                onVersionSelect = onVersionSelect
            )
        }

        is SelectGameVersionHost.VersionSettings -> BaseScreen(
            levels1 = listOf(
                Pair(NestedNavKey.VersionSettings::class.java, host.mainScreenKey)
            ),
            Triple(NormalNavKey.Versions.ModifyVersion, host.versionSettingsScreenKey, false)
        ) { isVisible ->
            SelectGameVersionContent(
                isVisible = isVisible,
                viewModel = viewModel,
                eventViewModel = eventViewModel,
                onVersionSelect = onVersionSelect
            )
        }
    }
}

@Composable
private fun SelectGameVersionContent(
    isVisible: Boolean,
    viewModel: VersionsViewModel,
    eventViewModel: EventViewModel,
    onVersionSelect: (MinecraftVersion) -> Unit
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .animated(ctx)
    ) {
        PullToRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = false,
            enabled = viewModel.versionState !is VersionState.Loading,
            onRefresh = { viewModel.refresh(true) }
        ) {
            when (val state = viewModel.versionState) {
                is VersionState.Loading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier.width(168.dp),
                            wavelength = 32.dp
                        )
                    }
                }

                is VersionState.Failure -> {
                    Box(Modifier.fillMaxSize()) {
                        ScalingLabel(
                            modifier = Modifier.align(Alignment.Center),
                            text = {
                                AndroidStringText(
                                    text = androidText(
                                        R.string.download_game_failed_to_get_versions,
                                        state.message
                                    )
                                )
                            },
                            onClick = {
                                viewModel.refresh(true)
                            }
                        )
                    }
                }

                is VersionState.None -> {
                    Column {
                        VersionHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            versionFilter = viewModel.versionFilter,
                            onVersionFilterChange = { viewModel.filterWith(it) },
                            itemContainerColor = cardColor(),
                            itemContentColor = onCardColor(),
                            onRefreshClick = {
                                viewModel.refresh(true)
                            }
                        )

                        VersionGrid(
                            modifier = Modifier.weight(1f),
                            groups = state.groups,
                            category = viewModel.versionFilter.category,
                            onVersionSelect = onVersionSelect,
                            openLink = { url ->
                                eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                            }
                        )
                    }
                }
            }
        }
    }
}

private val VERSION_NUMBER_REGEX = Regex("^(\\d+)\\.(\\d+)")

private const val GROUP_APRIL_FOOLS = "april_fools"
private const val GROUP_OLD_BETA = "old_beta"
private const val GROUP_OLD_ALPHA = "old_alpha"

/**
 * Lọc theo loại đang chọn và tên cần tìm, sau đó gom thành từng nhóm dòng phiên bản.
 * Giữ nguyên thứ tự mới nhất trước để nhóm mới nhất nằm ở đầu lưới.
 */
private fun List<MinecraftVersion>.groupVersions(
    versionFilter: VersionFilter
): List<VersionGroup> {
    val keyword = versionFilter.id.trim()
    return this.asSequence()
        .filter { version ->
            when (versionFilter.category) {
                VersionCategory.RELEASE -> version.type == MinecraftVersion.Type.Release
                VersionCategory.SNAPSHOT -> version.type == MinecraftVersion.Type.Snapshot
                VersionCategory.APRIL_FOOLS -> version.type == MinecraftVersion.Type.AprilFools
                VersionCategory.OLD -> version.type == MinecraftVersion.Type.OldBeta ||
                        version.type == MinecraftVersion.Type.OldAlpha
            }
        }
        .filter { version ->
            keyword.isEmpty() || version.version.id.contains(keyword, ignoreCase = true)
        }
        .sortedDescending()
        .groupBy { version -> version.groupKey() }
        .map { (key, versions) -> VersionGroup(key = key, versions = versions) }
}

/** Snapshot kiểu cũ đặt tên theo tuần, ví dụ 21w13a */
private val SNAPSHOT_YEAR_REGEX = Regex("^(\\d{2})w")

/** Khoá nhóm: dòng phiên bản (1.21), năm của snapshot cũ (2021), hoặc Beta/Alpha/Cá tháng tư */
private fun MinecraftVersion.groupKey(): String = when (type) {
    MinecraftVersion.Type.AprilFools -> GROUP_APRIL_FOOLS
    MinecraftVersion.Type.OldBeta -> GROUP_OLD_BETA
    MinecraftVersion.Type.OldAlpha -> GROUP_OLD_ALPHA
    else -> {
        val id = version.id
        VERSION_NUMBER_REGEX.find(id)?.value
            ?: SNAPSHOT_YEAR_REGEX.find(id)?.groupValues?.getOrNull(1)?.let { year -> "20$year" }
            ?: id.substringBefore('-', id)
    }
}

/** Tên nhóm hiển thị trên thẻ */
@Composable
private fun versionGroupTitle(key: String): String = when (key) {
    GROUP_APRIL_FOOLS -> stringResource(R.string.download_game_type_april_fools)
    GROUP_OLD_BETA -> stringResource(R.string.download_game_type_old_beta)
    GROUP_OLD_ALPHA -> stringResource(R.string.download_game_type_old_alpha)
    else -> key
}

@Composable
private fun VersionHeader(
    modifier: Modifier = Modifier,
    versionFilter: VersionFilter,
    onVersionFilterChange: (VersionFilter) -> Unit,
    itemContainerColor: Color,
    itemContentColor: Color,
    onRefreshClick: () -> Unit = {}
) {
    Column(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fadeEdge(
                            state = scrollState,
                            direction = EdgeDirection.Horizontal
                        )
                        .widthIn(max = this@BoxWithConstraints.maxWidth / 5 * 3) //3/5
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //Mỗi lần chỉ chọn một loại phiên bản
                    VersionCategory.entries.forEach { category ->
                        VersionTypeItem(
                            selected = versionFilter.category == category,
                            onClick = {
                                onVersionFilterChange(versionFilter.copy(category = category))
                            },
                            text = stringResource(category.labelRes)
                        )
                    }
                }

                //搜索、刷新
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimpleTextInputField(
                        modifier = Modifier.weight(1f),
                        value = versionFilter.id,
                        onValueChange = { onVersionFilterChange(versionFilter.copy(id = it)) },
                        color = itemContainerColor,
                        contentColor = itemContentColor,
                        singleLine = true,
                        hint = {
                            Text(
                                text = stringResource(R.string.generic_search),
                                style = TextStyle(color = itemContentColor).copy(fontSize = 12.sp)
                            )
                        }
                    )

                    IconButton(
                        onClick = onRefreshClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.generic_refresh)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun VersionTypeItem(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CheckChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = {
            Text(text)
        },
    )
}

/**
 * Lưới thẻ phiên bản theo dòng (1.21, 1.20, ...).
 * Bấm vào một thẻ để mở danh sách phiên bản của dòng đó ngay trong thẻ.
 */
@Composable
private fun VersionGrid(
    modifier: Modifier = Modifier,
    groups: List<VersionGroup>,
    category: VersionCategory,
    onVersionSelect: (MinecraftVersion) -> Unit,
    openLink: (url: String) -> Unit
) {
    if (groups.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.download_game_no_result),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    //Đổi loại phiên bản thì đóng thẻ đang mở
    var expandedKey by remember(category) { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 170.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = groups,
            key = { group -> group.key },
            span = { group ->
                GridItemSpan(if (group.key == expandedKey) maxLineSpan else 1)
            }
        ) { group ->
            VersionGroupCard(
                group = group,
                expanded = group.key == expandedKey,
                onToggle = {
                    expandedKey = if (expandedKey == group.key) null else group.key
                },
                onVersionSelect = onVersionSelect,
                openLink = openLink
            )
        }
    }
}

@Composable
private fun VersionGroupCard(
    group: VersionGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onVersionSelect: (MinecraftVersion) -> Unit,
    openLink: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape: Shape = MaterialTheme.shapes.extraLarge
    val banner: Painter = versionBanner(group.latest)
    val title = versionGroupTitle(group.key)

    Surface(
        modifier = modifier,
        shape = shape,
        color = cardColor(),
        contentColor = onCardColor()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (expanded) 3.2f else 1.9f)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onToggle)
            ) {
                Image(
                    modifier = Modifier.matchParentSize(),
                    painter = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                //Lớp phủ tối để chữ luôn đọc được trên ảnh nền
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(
                            R.string.download_game_group_version_count,
                            group.versions.size
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Icon(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                    painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    group.versions.forEach { version ->
                        VersionItemLayout(
                            modifier = Modifier.fillMaxWidth(),
                            version = version,
                            onClick = { onVersionSelect(version) },
                            onAccessWiki = openLink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionItemLayout(
    modifier: Modifier = Modifier,
    version: MinecraftVersion,
    onClick: () -> Unit = {},
    onAccessWiki: (String) -> Unit = {},
    shape: Shape = MaterialTheme.shapes.large,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    blur: Int = LauncherSettings.backgroundBlur.state,
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    val (icon, versionType, wikiUrl, summary) = getVersionComponents(version)
    val banner = versionBanner(version)
    //Có ảnh nền thì chữ luôn dùng màu sáng để đọc được trên ảnh
    val textColor = Color.White

    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        onClick = onClick,
        shape = shape,
        color = color,
        contentColor = textColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = shape)
                .backgroundGlass(blur, color, influencedByBackground)
        ) {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = banner,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            //Lớp phủ tối dần từ trái sang phải để chữ vẫn rõ
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.82f),
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.2f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(all = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let { versionIcon ->
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = versionIcon,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = version.version.id,
                            style = MaterialTheme.typography.labelLarge
                        )

                        LittleTextLabel(
                            text = versionType
                        )
                    }

                    summary?.let { text ->
                        Text(
                            modifier = Modifier.alpha(0.7f),
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Text(
                        modifier = Modifier.alpha(0.7f),
                        text = formatDate(
                            input = version.version.releaseTime,
                            pattern = stringResource(R.string.date_format)
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                wikiUrl?.let { url ->
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onAccessWiki(url) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_link),
                            contentDescription = "Wiki"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ảnh nền minh hoạ theo thế hệ phiên bản, giúp danh sách phiên bản không còn trống trải
 */
@Composable
private fun versionBanner(version: MinecraftVersion): Painter {
    val numbers = VERSION_NUMBER_REGEX.find(version.version.id)
    val major = numbers?.groupValues?.getOrNull(1)?.toIntOrNull()
    val minor = numbers?.groupValues?.getOrNull(2)?.toIntOrNull()
    val drawableId = when (version.type) {
        MinecraftVersion.Type.Release -> when {
            major == null || minor == null -> R.drawable.img_ver_classic
            //XOYO: dòng phiên bản theo năm (26, 27...) dùng ảnh riêng, khác với 1.21
            major >= 2 -> R.drawable.img_ver_year
            minor >= 21 -> R.drawable.img_ver_trial
            minor == 20 -> R.drawable.img_ver_cherry
            minor == 19 -> R.drawable.img_ver_wild
            minor == 18 -> R.drawable.img_ver_caves
            //XOYO: 1.17 (Caves & Cliffs Part I) tách riêng khỏi 1.18
            minor == 17 -> R.drawable.img_ver_mountains
            minor == 16 -> R.drawable.img_ver_nether
            minor == 15 -> R.drawable.img_ver_bees
            minor == 14 -> R.drawable.img_ver_village
            minor == 13 -> R.drawable.img_ver_aquatic
            minor == 12 -> R.drawable.img_ver_color
            minor == 11 -> R.drawable.img_ver_exploration
            else -> R.drawable.img_ver_classic
        }
        MinecraftVersion.Type.Snapshot -> R.drawable.img_ver_trial
        else -> R.drawable.img_ver_classic
    }
    return painterResource(drawableId)
}

@Composable
private fun getVersionComponents(
    version: MinecraftVersion
): Quadruple<Painter?, String, String?, String?> {
    val vmVer = version.version
    val summary = version.summary?.let { stringResource(it) }
    val urlSuffix = version.urlSuffix ?: vmVer.id

    return when (version.type) {
        MinecraftVersion.Type.Release -> {
            Quadruple(
                painterResource(R.drawable.img_minecraft),
                stringResource(R.string.download_game_type_release),
                stringResource(R.string.url_wiki_minecraft_game_release, urlSuffix),
                summary
            )
        }
        MinecraftVersion.Type.Snapshot -> {
            Quadruple(
                painterResource(R.drawable.img_command_block),
                stringResource(R.string.download_game_type_snapshot),
                stringResource(R.string.url_wiki_minecraft_game_snapshot, urlSuffix),
                summary
            )
        }
        MinecraftVersion.Type.AprilFools -> {
            Quadruple(
                painterResource(R.drawable.img_diamond_block),
                stringResource(R.string.download_game_type_april_fools),
                stringResource(R.string.url_wiki_minecraft_game_snapshot, urlSuffix),
                summary
            )
        }
        MinecraftVersion.Type.OldBeta -> {
            Quadruple(
                painterResource(R.drawable.img_old_cobblestone),
                stringResource(R.string.download_game_type_old_beta),
                null,
                summary
            )
        }
        MinecraftVersion.Type.OldAlpha -> {
            Quadruple(
                painterResource(R.drawable.img_old_grass_block),
                stringResource(R.string.download_game_type_old_alpha),
                null,
                summary
            )
        }
        else -> {
            Quadruple(
                null,
                stringResource(R.string.generic_unknown),
                null,
                version.summary?.let { stringResource(it) }
            )
        }
    }
}
