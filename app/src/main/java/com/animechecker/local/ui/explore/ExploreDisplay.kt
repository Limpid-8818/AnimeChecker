package com.animechecker.local.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.animechecker.local.domain.model.Quarter
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.components.CoverImage
import com.animechecker.local.ui.components.RatingText

/**
 * Season选择条。
 */
@Composable
internal fun SeasonChipsRow(
    options: List<Season>,
    selected: Season,
    onSelect: (Season) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { season ->
            val isSelected = selected == season
            Text(
                text = Strings.seasonTitle(season),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(season) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

/**
 * 番剧库网格
 */
@Composable
internal fun PosterGrid(
    items: List<Subject>,
    onOpenDetail: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { subject ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { onOpenDetail(subject.id) }
                    .padding(8.dp),
            ) {
                CoverImage(
                    model = subject.coverUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                    corner = 8,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subject.nameCn ?: subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                RatingText(subject.ratingScore)
            }
        }
    }
}

/**
 * 预览
 */
@Preview(name = "SeasonChipsRow", showBackground = true)
@Composable
fun SeasonChipsRowPreview() {
    SeasonChipsRow(
        options = listOf(
            Season(2026, Quarter.FALL),
            Season(2026, Quarter.SUMMER),
            Season(2026, Quarter.SPRING),
            Season(2026, Quarter.WINTER)
        ),
        selected = Season(2026, Quarter.FALL)
    ) { }
}

@Preview(name = "PosterGridPreview", showBackground = true)
@Composable
fun PosterGridPreview() {
    PosterGrid(
        items = listOf(
            Subject(1, "Test1", ratingScore = 10.0),
            Subject(2, "Test2"),
            Subject(3, "Test3")
        )
    ) { }
}