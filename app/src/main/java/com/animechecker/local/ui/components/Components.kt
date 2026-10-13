@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.animechecker.local.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.animechecker.local.domain.model.TrackStatus
import com.animechecker.local.ui.Strings

/**
 * 跨页面通用组件：只放被两个以上页面复用的部分，单页面私有的组件留在各 feature 包内。
 * 勾集弹层另见 EpisodeCheckSheet。
 */

/**
 * 页面顶栏。容器色与页面底一致。
 */
@Composable
fun AppTopAppBar(
    modifier: Modifier = Modifier,
    title: String = "",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.BACK)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/**
 * AppTopBar 预览
 */
@Preview(name = "AppBar", showBackground = true, showSystemUi = true)
@Preview(
    name = "AppBar-Night",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AppBarPreview() {
    AppTopAppBar(title = "预览Preview")
}

/** 状态 pill，五态配色见设计规范 06 §3.3。 */
@Composable
fun StatusPill(status: TrackStatus, modifier: Modifier = Modifier) {
    val (background, foreground) = when (status) {
        TrackStatus.DOING ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer

        TrackStatus.DONE ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer

        TrackStatus.WISH, TrackStatus.ON_HOLD, TrackStatus.DROPPED ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = Strings.statusName(status),
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 状态 pill 五态预览
 */
@Preview(name = "StatusPill", showBackground = true)
@Preview(name = "StatusPill-Night", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StatusPillPreview() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrackStatus.entries.forEach { StatusPill(it) }
    }
}

/** 评分行：琥珀星 + 一位小数；无评分给占位（设计规范 06 §1 tertiary）。 */
@Composable
fun RatingText(score: Double?, modifier: Modifier = Modifier) {
    if (score == null || score <= 0.0) {
        Text(
            Strings.SCORE_UNRATED,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    } else {
        Text(
            "★ %.1f".format(score),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier,
        )
    }
}

/**
 * 评分预览：有分与无分两态
 */
@Preview(name = "RatingText", showBackground = true)
@Composable
fun RatingTextPreview() {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RatingText(8.6)
        RatingText(null)
    }
}

/** 内容白卡：surface 底 + 1dp outline 描边 + 12dp 圆角，无阴影。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        content = content,
    )
}

/** 封面图，2:3；无图时显示 surfaceVariant 底色。 */
@Composable
fun CoverImage(model: Any?, modifier: Modifier = Modifier, corner: Int = 8) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * 封面占位预览（无图态）
 */
@Preview(name = "CoverImage", showBackground = true)
@Composable
fun CoverImagePreview() {
    CoverImage(model = null, modifier = Modifier.size(width = 64.dp, height = 92.dp))
}

/**
 * 在追标记。
 */
@Composable
fun TrackedDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

/** 集数进度：大数字 watched + 次级 /总数 + 细进度条；数字变化上滚，点数字可编辑。 */
@Composable
fun EpisodeProgress(
    watched: Int,
    modifier: Modifier = Modifier,
    total: Int? = null,
    onNumberClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimatedContent(
                targetState = watched,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn())
                        .togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "watchedEps",
                modifier = if (onNumberClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNumberClick)
                } else {
                    Modifier
                },
            ) { count ->
                Text(
                    "$count",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                "/ ${total ?: "?"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        // total 可能是 0（未开播）：分母保底 1，否则 0/0 会让进度条算出 NaN
        val denominator = (total ?: watched).coerceAtLeast(1).toFloat()
        LinearProgressIndicator(
            progress = { (watched.toFloat() / denominator).coerceIn(0f, 1f) },
            trackColor = MaterialTheme.colorScheme.outline,
            drawStopIndicator = {},
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
        )
    }
}

/**
 * 进度预览：总数已知与未知两种
 */
@Preview(name = "EpisodeProgress", showBackground = true)
@Composable
fun EpisodeProgressPreview() {
    Column(
        modifier = Modifier
            .width(200.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EpisodeProgress(watched = 5, total = 12)
        EpisodeProgress(watched = 3, total = null)
    }
}

/**
 * 列表加载骨架：surfaceVariant 圆角色块，不承载文案。
 *
 * [rowHeight] 与 [spacing] 要跟目标列表的真实行对齐，否则加载完成时整页会跳一下。
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 4,
    rowHeight: Dp = 92.dp,
    spacing: Dp = 12.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(rows) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Preview(name = "Skeleton")
@Composable
fun LoadingSkeletonPreview() {
    LoadingSkeleton(rows = 5)
}

/** 空态：图标位 + 标题 + 说明 + 动作按钮。 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionText) }
        }
    }
}

/** 页面级错误条：文案由调用方从 Strings.errorText 取，可附一个动作入口。 */
@Composable
fun ErrorBar(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    actionText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * 错误预览
 */
@Preview
@Composable
fun ErrorBarPreview() {
    ErrorBar("TestError", actionText = "TestAction")
}
