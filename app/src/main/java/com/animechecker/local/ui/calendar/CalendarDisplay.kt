package com.animechecker.local.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.animechecker.local.domain.AiredEps
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.components.CoverImage
import com.animechecker.local.ui.components.TrackedDot
import java.time.LocalDate

/**
 * 每周日期选择行。
 *
 * 包括当日与选中日高亮；横向滚动跟随选中日（默认为当天）。
 */
@Composable
internal fun WeekdayRow(selectedDay: Int, today: Int, onDaySelected: (Int) -> Unit) {
    val listState = rememberLazyListState()
    // 滚动跟随选中日，并在左侧留一个 pill 作左边距
    LaunchedEffect(selectedDay) {
        listState.scrollToItem((selectedDay - 2).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(count = WEEKDAYS, key = { it + 1 }) { index ->
            val wd = index + 1
            val selected = selectedDay == wd
            val isToday = today == wd
            val fg = when {
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .animateContentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onDaySelected(wd) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    Strings.weekdayName(wd),
                    style = MaterialTheme.typography.labelMedium,
                    color = fg
                )
                Spacer(Modifier.height(2.dp))
                Box(Modifier.size(4.dp, 4.dp)) {
                    if (isToday) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

private const val WEEKDAYS = 7

/**
 * 放送表行。
 */
@Composable
internal fun CalendarRow(
    subject: Subject,
    tracked: Boolean,
    onOpenDetail: () -> Unit,
) {
    val aired = AiredEps.subjectAired(subject)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenDetail)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            CoverImage(
                model = subject.coverUrl,
                modifier = Modifier.size(width = 40.dp, height = 56.dp),
                corner = 6
            )
            if (tracked) {
                TrackedDot(Modifier.padding(2.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                subject.nameCn ?: subject.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (subject.airDate != null) {
                Text(
                    subject.airDate.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            when (aired) {
                null -> "—"
                0 -> Strings.NOT_AIRED
                else -> Strings.AIRED_COUNT.format(aired)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * 预览
 */
@Preview(name = "Weekday Row", showBackground = true)
@Composable
fun WeekdayRowPreview() {
    WeekdayRow(
        5,
        today = 3,
        onDaySelected = {}
    )
}

@Preview(name = "Calendar Row", showBackground = true)
@Composable
fun CalendarRowPreview() {
    CalendarRow(
        subject = Subject(
            id = 1,
            name = "Test",
            nameCn = "测试名称",
            airDate = LocalDate.now()
        ),
        tracked = true,
        onOpenDetail = { }
    )
}