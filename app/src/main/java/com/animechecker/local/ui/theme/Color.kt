package com.animechecker.local.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 以 SkyBlue 为主题色。

private val SkyBlue = Color(0xFF2E90FA)
private val SkyBlueDark = Color(0xFF0D5FB8)
private val SkyBlueContainer = Color(0xFFE3F0FF)
private val GrassGreen = Color(0xFF34A853)
private val GrassGreenDark = Color(0xFF1E7B41)
private val GrassContainer = Color(0xFFE7F5EC)
private val Amber = Color(0xFFE8940A)

val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueDark,
    secondary = GrassGreen,
    onSecondary = Color.White,
    secondaryContainer = GrassContainer,
    onSecondaryContainer = GrassGreenDark,
    tertiary = Amber,
    onTertiary = Color.White,
    error = Color(0xFFDE4436),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2329),
    surface = Color.White,
    onSurface = Color(0xFF1F2329),
    surfaceVariant = Color(0xFFEFF3F7),
    onSurfaceVariant = Color(0xFF6E7784),
    outline = Color(0xFFE3E9F0),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF7AB5FF),
    onPrimary = Color(0xFF00284D),
    primaryContainer = Color(0xFF1C4A80),
    onPrimaryContainer = Color(0xFFCCE5FF),
    secondary = Color(0xFF6FCF97),
    onSecondary = Color(0xFF0B2E1B),
    secondaryContainer = Color(0xFF1D4A30),
    onSecondaryContainer = Color(0xFFC9F2DA),
    tertiary = Color(0xFFFFC14D),
    onTertiary = Color(0xFF3D2B00),
    error = Color(0xFFFF8A80),
    background = Color(0xFF101418),
    onBackground = Color(0xFFF0F3F7),
    surface = Color(0xFF1A1F26),
    onSurface = Color(0xFFF0F3F7),
    surfaceVariant = Color(0xFF242B34),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF343D49),
)
