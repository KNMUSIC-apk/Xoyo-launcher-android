/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.theme.XoyoMotion
import com.movtery.zalithlauncher.ui.theme.pressScale

/**
 * Các loại tải hiện trên rail trái của trang Tải về, theo kiểu launcher PC.
 * Trùng với các mục Tải về trên sidebar.
 */
enum class DownloadRailTarget(val titleRes: Int, val iconRes: Int) {
    MOD(R.string.nav_download_mods, R.drawable.ic_extension_outlined),
    MODPACK(R.string.nav_download_modpacks, R.drawable.ic_archive_filled),
    RESOURCE_PACK(R.string.nav_download_resource_packs, R.drawable.ic_photo_library_outlined),
    SHADER(R.string.nav_download_shaders, R.drawable.ic_lightbulb),
    WORLD(R.string.nav_download_worlds, R.drawable.ic_public)
}

/**
 * Hàm chuyển loại tải, do màn Tải về cung cấp.
 * Null nghĩa là không ở trong màn Tải về, khi đó rail không hiện.
 */
val LocalDownloadRailSwitch = compositionLocalOf<((DownloadRailTarget) -> Unit)?> { null }

/**
 * Rail trái: danh sách loại tải dạng viên thuốc. Mục đang chọn tô màu nhấn.
 * @param selected loại đang mở, null nếu không khớp loại nào
 * @param compact màn hình hẹp: chỉ hiện icon để nhường chỗ cho danh sách kết quả
 */
@Composable
fun DownloadTypeRail(
    selected: DownloadRailTarget?,
    onSelect: (DownloadRailTarget) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(if (compact) 60.dp else 160.dp)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DownloadRailTarget.entries.forEach { target ->
            DownloadRailItem(
                target = target,
                selected = target == selected,
                compact = compact,
                onClick = { onSelect(target) }
            )
        }
    }
}

@Composable
private fun DownloadRailItem(
    target: DownloadRailTarget,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val title = stringResource(target.titleRes)
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val reducedMotion = LauncherSettings.launcherReducedMotion.state

    val background by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            hovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
            else -> Color.Transparent
        },
        animationSpec = if (reducedMotion) snap() else XoyoMotion.soft(),
        label = "downloadRailBackground"
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
            .pressScale(source, enabled = !reducedMotion)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .hoverable(source)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = if (compact) 0.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(target.iconRes),
            contentDescription = if (compact) title else null,
            tint = contentColor
        )
        if (!compact) {
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
