@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.animechecker.local.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animechecker.local.ui.Strings

/**
 * 集数勾选底部弹层：清单页与详情页共用的进度编辑入口。
 *
 * 单集独立开关：点一格勾选/取消该集；仅已放送的集数可选。
 * 点选即保存，无确认按钮。
 */
@Composable
fun EpisodeCheckSheet(
    title: String,
    watched: Set<Int>,
    aired: Int?,
    total: Int?,
    onToggleEpisode: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val cap = maxOf(watched.maxOrNull() ?: 0, aired ?: 0, total ?: 0, MIN_GRID_ROWS)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "${watched.size} / ${total ?: "?"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                Strings.SHEET_PICK_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 384.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items((1..cap).toList()) { episode ->
                    EpisodeChip(
                        episode = episode,
                        checked = episode in watched,
                        // 仅已放送的集数可选；已勾选的始终可点，便于撤销
                        enabled = episode in watched || aired == null || episode <= aired,
                        onClick = { onToggleEpisode(episode) },
                    )
                }
            }
        }
    }
}

private const val MIN_GRID_ROWS = 12

@Composable
private fun EpisodeChip(
    episode: Int,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .height(44.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    checked -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .border(
                width = 1.dp,
                color = if (enabled && !checked) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = shape,
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$episode",
            style = MaterialTheme.typography.titleMedium,
            color = when {
                checked -> MaterialTheme.colorScheme.onPrimary
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
