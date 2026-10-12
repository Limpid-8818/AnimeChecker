package com.animechecker.local.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * 应用主题：浅色为默认，跟随系统切深色；不启用动态取色。
 * 颜色与字体只在 ui/theme 定义，其余代码一律取 MaterialTheme。
 */
@Composable
fun AnimeCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
