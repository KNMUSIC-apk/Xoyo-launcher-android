/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.ContentKind
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/** Chiều rộng màn hình (dp) dưới ngưỡng này sẽ dùng thanh điều hướng dưới thay cho sidebar */
val CompactWidthThreshold = 720.dp

private val SidebarExpandedWidth = 232.dp
private val SidebarCollapsedWidth = 72.dp

/**
 * Nhóm chức năng trên sidebar.
 * Các nhóm trong [CollapsibleNavGroups] chỉ hiện một nút, bấm vào mới mở các mục bên trong.
 */
enum class NavGroup(val titleRes: Int, val iconRes: Int) {
    MAIN(R.string.nav_group_main, R.drawable.ic_home_filled),
    CONTENT(R.string.nav_group_content, R.drawable.ic_folder_filled),
    DOWNLOAD(R.string.nav_group_download, R.drawable.ic_download_2_filled),
    SOCIAL(R.string.nav_group_social, R.drawable.ic_group_filled),
    SYSTEM(R.string.nav_group_system, R.drawable.ic_build_filled)
}

/** Các nhóm thu gọn được trên sidebar và trong sheet "Thêm". Nhóm Chính luôn hiện đầy đủ. */
private val CollapsibleNavGroups = setOf(NavGroup.CONTENT, NavGroup.DOWNLOAD, NavGroup.SOCIAL, NavGroup.SYSTEM)

/** Các mục điều hướng của XOYO. Nhóm Nội dung gồm cả các mục tải xuống trước đây nằm trong thanh danh mục */
enum class NavDestination(val iconRes: Int, val titleRes: Int, val group: NavGroup) {
    HOME(R.drawable.ic_home_filled, R.string.nav_home, NavGroup.MAIN),
    PROFILES(R.drawable.ic_videogame_asset_outlined, R.string.nav_profiles, NavGroup.MAIN),
    VERSIONS(R.drawable.ic_box, R.string.nav_versions, NavGroup.MAIN),
    GAME_DOWNLOAD(R.drawable.ic_sports_esports_outlined, R.string.download_category_game, NavGroup.CONTENT),
    MODS(R.drawable.ic_extension_outlined, R.string.nav_mods, NavGroup.CONTENT),
    MODPACKS(R.drawable.ic_archive_filled, R.string.nav_modpacks, NavGroup.CONTENT),
    RESOURCE_PACKS(R.drawable.ic_photo_library_outlined, R.string.nav_resource_packs, NavGroup.CONTENT),
    SHADERS(R.drawable.ic_lightbulb, R.string.nav_shaders, NavGroup.CONTENT),
    WORLDS(R.drawable.ic_public, R.string.nav_worlds, NavGroup.CONTENT),
    SEARCH_ID(R.drawable.ic_card, R.string.download_category_by_id, NavGroup.CONTENT),
    FAVORITES(R.drawable.ic_favorite_outlined, R.string.download_category_favorites, NavGroup.CONTENT),
    DOWNLOAD_MODS(R.drawable.ic_extension_outlined, R.string.nav_download_mods, NavGroup.DOWNLOAD),
    DOWNLOAD_MODPACKS(R.drawable.ic_archive_filled, R.string.nav_download_modpacks, NavGroup.DOWNLOAD),
    DOWNLOAD_RESOURCE_PACKS(R.drawable.ic_photo_library_outlined, R.string.nav_download_resource_packs, NavGroup.DOWNLOAD),
    DOWNLOAD_SHADERS(R.drawable.ic_lightbulb, R.string.nav_download_shaders, NavGroup.DOWNLOAD),
    DOWNLOAD_WORLDS(R.drawable.ic_public, R.string.nav_download_worlds, NavGroup.DOWNLOAD),
    SERVERS(R.drawable.ic_dns_outlined, R.string.nav_servers, NavGroup.SOCIAL),
    ACCOUNTS(R.drawable.ic_person_outlined, R.string.nav_accounts, NavGroup.SOCIAL),
    PERFORMANCE(R.drawable.ic_dashboard_outlined, R.string.nav_performance, NavGroup.SYSTEM),
    SETTINGS(R.drawable.ic_settings_filled, R.string.nav_settings, NavGroup.SYSTEM);
}

/** Các mục hiện trực tiếp trên thanh dưới khi màn hình hẹp. Phần còn lại nằm trong "Thêm". */
val CompactPrimaryDestinations = listOf(
    NavDestination.HOME,
    NavDestination.PROFILES,
    NavDestination.MODS,
    NavDestination.PERFORMANCE
)

/** Xác định mục nào đang được chọn từ khoá màn hình hiện tại */
fun navDestinationOf(key: TitledNavKey?): NavDestination? = when (key) {
    null, NormalNavKey.LauncherMain -> NavDestination.HOME
    is NormalNavKey.GameLibrary -> NavDestination.PROFILES
    is NormalNavKey.VersionsManager -> NavDestination.VERSIONS
    is NormalNavKey.ContentCenter -> when (key.kind) {
        ContentKind.MODS -> NavDestination.MODS
        ContentKind.MODPACKS -> NavDestination.MODPACKS
        ContentKind.RESOURCE_PACKS -> NavDestination.RESOURCE_PACKS
        ContentKind.SHADERS -> NavDestination.SHADERS
    }
    is NormalNavKey.WorldManager -> NavDestination.WORLDS
    is NormalNavKey.ServerHub -> NavDestination.SERVERS
    is NormalNavKey.AccountManager -> NavDestination.ACCOUNTS
    is NormalNavKey.PerformanceCenter -> NavDestination.PERFORMANCE
    is NestedNavKey.Settings -> NavDestination.SETTINGS
    else -> null
}

/**
 * Chuyển đến một mục điều hướng.
 * Các mục cấp cao đặt lại ngăn xếp về đúng một màn hình, giống hành vi nút Home cũ.
 */
fun ScreenBackStackViewModel.navigateToDestination(destination: NavDestination) {
    when (destination) {
        NavDestination.HOME -> mainScreen.clearWith(NormalNavKey.LauncherMain)
        NavDestination.PROFILES -> mainScreen.clearWith(NormalNavKey.GameLibrary)
        NavDestination.VERSIONS -> mainScreen.clearWith(NormalNavKey.VersionsManager)
        NavDestination.MODS -> mainScreen.clearWith(NormalNavKey.ContentCenter(ContentKind.MODS))
        NavDestination.MODPACKS -> mainScreen.clearWith(NormalNavKey.ContentCenter(ContentKind.MODPACKS))
        NavDestination.RESOURCE_PACKS -> mainScreen.clearWith(NormalNavKey.ContentCenter(ContentKind.RESOURCE_PACKS))
        NavDestination.SHADERS -> mainScreen.clearWith(NormalNavKey.ContentCenter(ContentKind.SHADERS))
        NavDestination.WORLDS -> mainScreen.clearWith(NormalNavKey.WorldManager)
        NavDestination.SERVERS -> mainScreen.clearWith(NormalNavKey.ServerHub)
        NavDestination.ACCOUNTS -> mainScreen.clearWith(NormalNavKey.AccountManager())
        NavDestination.PERFORMANCE -> mainScreen.clearWith(NormalNavKey.PerformanceCenter)
        NavDestination.GAME_DOWNLOAD -> navigateToDownload(downloadGameScreen)
        NavDestination.SEARCH_ID -> navigateToDownload(NormalNavKey.SearchId)
        NavDestination.FAVORITES -> navigateToDownload(downloadFavoritesScreen)
        NavDestination.DOWNLOAD_MODS -> navigateToDownload(downloadModScreen)
        NavDestination.DOWNLOAD_MODPACKS -> navigateToDownload(downloadModPackScreen)
        NavDestination.DOWNLOAD_RESOURCE_PACKS -> navigateToDownload(downloadResourcePackScreen)
        NavDestination.DOWNLOAD_SHADERS -> navigateToDownload(downloadShadersScreen)
        NavDestination.DOWNLOAD_WORLDS -> navigateToDownload(downloadSavesScreen)
        NavDestination.SETTINGS -> mainScreen.removeAndNavigateTo(
            removes = clearBeforeNavKeys,
            screenKey = settingsScreen
        )
    }
}

/**
 * Mục đang được chọn khi đang ở màn Tải xuống: theo danh mục con đang mở.
 * Trường hợp không khớp danh mục nào thì coi là Trò chơi.
 */
fun ScreenBackStackViewModel.downloadDestination(): NavDestination = when (downloadScreen.currentKey) {
    downloadModScreen -> NavDestination.DOWNLOAD_MODS
    downloadModPackScreen -> NavDestination.DOWNLOAD_MODPACKS
    downloadResourcePackScreen -> NavDestination.DOWNLOAD_RESOURCE_PACKS
    downloadShadersScreen -> NavDestination.DOWNLOAD_SHADERS
    downloadSavesScreen -> NavDestination.DOWNLOAD_WORLDS
    downloadFavoritesScreen -> NavDestination.FAVORITES
    NormalNavKey.SearchId -> NavDestination.SEARCH_ID
    else -> NavDestination.GAME_DOWNLOAD
}

/**
 * Sidebar cho màn hình rộng. Thu gọn chỉ còn icon, mở rộng hiện tên.
 * Trạng thái mở rộng được lưu lại trong cài đặt.
 */
@Composable
fun AppSidebar(
    selected: NavDestination?,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = LauncherSettings.launcherSidebarExpanded.state
    val reducedMotion = LauncherSettings.launcherReducedMotion.state

    //Trạng thái mở của từng nhóm thu gọn. Mặc định đều đóng.
    val openGroups = remember { mutableStateListOf<NavGroup>() }

    val width by animateDpAsState(
        targetValue = if (expanded) SidebarExpandedWidth else SidebarCollapsedWidth,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 220),
        label = "sidebarWidth"
    )

    Surface(
        modifier = modifier
            .width(width)
            .fillMaxHeight(),
        color = cardColor(true),
        contentColor = onCardColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (expanded) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        text = "XOYO",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                IconButton(
                    onClick = {
                        LauncherSettings.launcherSidebarExpanded.save(!expanded)
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (expanded) R.drawable.ic_menu_open else R.drawable.ic_menu
                        ),
                        contentDescription = stringResource(
                            if (expanded) R.string.nav_collapse else R.string.nav_expand
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavDestination.entries.groupBy { it.group }.forEach { (group, items) ->
                    if (group in CollapsibleNavGroups) {
                        //Mỗi nhóm chỉ hiện một nút. Bấm vào để mở hoặc đóng các mục bên trong.
                        val open = group in openGroups
                        SidebarGroupHeader(
                            group = group,
                            containsSelected = items.any { it == selected },
                            expanded = expanded,
                            open = open,
                            reducedMotion = reducedMotion,
                            onClick = { if (open) openGroups.remove(group) else openGroups.add(group) }
                        )
                        AnimatedVisibility(
                            visible = open,
                            enter = if (reducedMotion) EnterTransition.None else expandVertically() + fadeIn(),
                            exit = if (reducedMotion) ExitTransition.None else shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(start = if (expanded) 12.dp else 0.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items.forEach { destination ->
                                    SidebarItem(
                                        destination = destination,
                                        selected = destination == selected,
                                        expanded = expanded,
                                        reducedMotion = reducedMotion,
                                        onClick = { onNavigate(destination) }
                                    )
                                }
                            }
                        }
                    } else {
                        if (expanded) {
                            Text(
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                                text = stringResource(group.titleRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items.forEach { destination ->
                            SidebarItem(
                                destination = destination,
                                selected = destination == selected,
                                expanded = expanded,
                                reducedMotion = reducedMotion,
                                onClick = { onNavigate(destination) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Nút đại diện cho một nhóm thu gọn trên sidebar */
@Composable
private fun SidebarGroupHeader(
    group: NavGroup,
    containsSelected: Boolean,
    expanded: Boolean,
    open: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(group.titleRes)
    val contentColor = if (containsSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 180),
        label = "sidebarGroupChevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(group.iconRes),
            contentDescription = if (expanded) null else title,
            tint = contentColor
        )
        if (expanded) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation),
                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                tint = contentColor
            )
        }
    }
}

@Composable
private fun SidebarItem(
    destination: NavDestination,
    selected: Boolean,
    expanded: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(destination.titleRes)
    val targetBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 160),
        label = "sidebarItemBackground"
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(destination.iconRes),
            contentDescription = if (expanded) null else title,
            tint = contentColor
        )
        if (expanded) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Thanh điều hướng dưới cho màn hình hẹp (điện thoại).
 */
@Composable
fun AppBottomBar(
    selected: NavDestination?,
    onNavigate: (NavDestination) -> Unit,
    onMore: () -> Unit,
    moreSelected: Boolean,
) {
    NavigationBar(containerColor = cardColor(true)) {
        CompactPrimaryDestinations.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
        NavigationBarItem(
            selected = moreSelected,
            onClick = onMore,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_more_horiz),
                    contentDescription = null
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_more),
                    maxLines = 1
                )
            }
        )
    }
}

/**
 * Danh sách các mục còn lại, mở từ nút "Thêm" trên thanh dưới.
 * Các nhóm thu gọn giống sidebar: mỗi nhóm một nút, bấm vào để mở.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreDestinationsSheet(
    selected: NavDestination?,
    onNavigate: (NavDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val reducedMotion = LauncherSettings.launcherReducedMotion.state
    val openGroups = remember { mutableStateListOf<NavGroup>() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavDestination.entries
                .filter { it !in CompactPrimaryDestinations }
                .groupBy { it.group }
                .forEach { (group, items) ->
                    if (group in CollapsibleNavGroups) {
                        val open = group in openGroups
                        SheetGroupHeader(
                            group = group,
                            containsSelected = items.any { it == selected },
                            open = open,
                            reducedMotion = reducedMotion,
                            onClick = { if (open) openGroups.remove(group) else openGroups.add(group) }
                        )
                        AnimatedVisibility(
                            visible = open,
                            enter = if (reducedMotion) EnterTransition.None else expandVertically() + fadeIn(),
                            exit = if (reducedMotion) ExitTransition.None else shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items.forEach { destination ->
                                    SheetDestinationRow(
                                        destination = destination,
                                        isSelected = destination == selected,
                                        onNavigate = onNavigate,
                                        onDismiss = onDismiss
                                    )
                                }
                            }
                        }
                    } else {
                        items.forEach { destination ->
                            SheetDestinationRow(
                                destination = destination,
                                isSelected = destination == selected,
                                onNavigate = onNavigate,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun SheetGroupHeader(
    group: NavGroup,
    containsSelected: Boolean,
    open: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (containsSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 180),
        label = "sheetGroupChevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(group.iconRes),
            contentDescription = null,
            tint = tint
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(group.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier
                .size(20.dp)
                .rotate(chevronRotation),
            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
            contentDescription = null,
            tint = tint
        )
    }
}

@Composable
private fun SheetDestinationRow(
    destination: NavDestination,
    isSelected: Boolean,
    onNavigate: (NavDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button) {
                onNavigate(destination)
                onDismiss()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = stringResource(destination.titleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
