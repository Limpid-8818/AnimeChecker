package com.animechecker.local.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.animechecker.local.R

/**
 * Plus Jakarta Sans 可变字体（打包进 res/font，不依赖下载服务），中文走系统 sans-serif 回退。
 *
 * 拉丁与数字用该字体；数字统一开启等宽特性 tnum：
 * 保证进度数字变化时整行宽度不变，"记录一集"的滚动动画不抖版。
 */
val AppFontFamily = FontFamily(
    Font(
        R.font.plus_jakarta_sans,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.plus_jakarta_sans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.plus_jakarta_sans,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.plus_jakarta_sans,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

private fun TextStyle.withAppFont(): TextStyle =
    copy(fontFamily = AppFontFamily, fontFeatureSettings = "tnum")

val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.withAppFont(),
        displayMedium = base.displayMedium.withAppFont(),
        displaySmall = base.displaySmall.withAppFont(),
        headlineLarge = base.headlineLarge.withAppFont(),
        headlineMedium = base.headlineMedium.withAppFont(),
        headlineSmall = base.headlineSmall.withAppFont(),
        titleLarge = base.titleLarge.withAppFont(),
        titleMedium = base.titleMedium.withAppFont(),
        titleSmall = base.titleSmall.withAppFont(),
        bodyLarge = base.bodyLarge.withAppFont(),
        bodyMedium = base.bodyMedium.withAppFont(),
        bodySmall = base.bodySmall.withAppFont(),
        labelLarge = base.labelLarge.withAppFont(),
        labelMedium = base.labelMedium.withAppFont(),
        labelSmall = base.labelSmall.withAppFont(),
    )
}
