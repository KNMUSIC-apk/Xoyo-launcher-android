/*
 * XOYO Launcher
 * Copyright (C) 2026 XOYO contributors
 *
 * Bảng màu riêng của XOYO (XOYO, MONOCHROME). Đây là ColorScheme viết tay,
 * không phụ thuộc vào các bảng màu có sẵn của ZalithLauncher.
 * Ý tưởng chế độ Monochrome (nền đen OLED) lấy từ Hyper Launcher 3 (LGPL-3.0);
 * không sao chép mã nguồn.
 */

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Hạt giống màu dùng cho thanh tiêu đề native (ngoài Compose) */
val XOYO_SEED_DARK = 0xFFFFB86B.toInt()
val XOYO_SEED_LIGHT = 0xFF8A5A12.toInt()
val MONOCHROME_SEED_DARK = 0xFFEDEDED.toInt()
val MONOCHROME_SEED_LIGHT = 0xFF2B2B2B.toInt()

/** XOYO tối: nền mực ấm, nhấn hổ phách, phụ teal, tam violet */
internal val xoyoDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF3F2400),
    primaryContainer = Color(0xFF5A3600),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFF7FD1C7),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004F47),
    onSecondaryContainer = Color(0xFF9FF0E4),
    tertiary = Color(0xFFBBA7FF),
    onTertiary = Color(0xFF2B1B63),
    tertiaryContainer = Color(0xFF42327C),
    onTertiaryContainer = Color(0xFFE5DCFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E0F12),
    onBackground = Color(0xFFE6E2DA),
    surface = Color(0xFF0E0F12),
    onSurface = Color(0xFFE6E2DA),
    surfaceVariant = Color(0xFF2A2C31),
    onSurfaceVariant = Color(0xFFC9C4BB),
    outline = Color(0xFF8C8A84),
    outlineVariant = Color(0xFF3E4046),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E2DA),
    inverseOnSurface = Color(0xFF2F3136),
    inversePrimary = Color(0xFF8A5A12),
    surfaceDim = Color(0xFF0B0C0E),
    surfaceBright = Color(0xFF2F3136),
    surfaceContainerLowest = Color(0xFF08090B),
    surfaceContainerLow = Color(0xFF15171A),
    surfaceContainer = Color(0xFF1A1C20),
    surfaceContainerHigh = Color(0xFF22252A),
    surfaceContainerHighest = Color(0xFF2B2E34),
)

/** XOYO sáng: giấy ngà ấm, cùng nhóm nhấn hổ phách và teal */
internal val xoyoLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A12),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2A1600),
    secondary = Color(0xFF1F6A62),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA7F3E8),
    onSecondaryContainer = Color(0xFF002019),
    tertiary = Color(0xFF5B43A8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DCFF),
    onTertiaryContainer = Color(0xFF1C0F4A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFECE6DC),
    onSurfaceVariant = Color(0xFF4D4941),
    outline = Color(0xFF7E786E),
    outlineVariant = Color(0xFFCFC8BC),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF31302C),
    inverseOnSurface = Color(0xFFF5F0E7),
    inversePrimary = Color(0xFFFFB86B),
    surfaceDim = Color(0xFFE3DED5),
    surfaceBright = Color(0xFFFFFCF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F1E8),
    surfaceContainer = Color(0xFFF1EBE0),
    surfaceContainerHigh = Color(0xFFEBE4D8),
    surfaceContainerHighest = Color(0xFFE5DED1),
)

/** Monochrome tối: nền đen tuyệt đối (tiết kiệm pin màn OLED), chỉ dùng thang xám */
internal val monochromeDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFEDEDED),
    onPrimary = Color(0xFF101010),
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = Color(0xFFF0F0F0),
    secondary = Color(0xFFB8B8B8),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFDCDCDC),
    tertiary = Color(0xFF9A9A9A),
    onTertiary = Color(0xFF141414),
    tertiaryContainer = Color(0xFF1F1F1F),
    onTertiaryContainer = Color(0xFFCFCFCF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFB5B5B5),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF2E2E2E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF4A4A4A),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
)

/** Monochrome sáng: giấy trắng và thang xám */
internal val monochromeLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF5C5C5C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF707070),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEFEFEF),
    onTertiaryContainer = Color(0xFF1A1A1A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF474747),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFFD0D0D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2B2B2B),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFC8C8C8),
    surfaceDim = Color(0xFFDADADA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE6E6E6),
)
