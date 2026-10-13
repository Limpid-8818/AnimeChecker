package com.animechecker.local.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.appViewModel
import com.animechecker.local.ui.components.AppTopAppBar
import com.animechecker.local.ui.components.EmptyState
import com.animechecker.local.ui.components.ErrorBar
import com.animechecker.local.ui.components.LoadingSkeleton

/**
 * 放送表：按星期查看放送窗口内的条目。
 *
 * 进入本页触发一次目录级拉取（refreshCatalog，一次 10-25 秒，60 秒冷却内不重复拉）；
 * 列表数据全部来自 Room，只有"本地一条都没有且正在首次拉取"时才给骨架屏，其余情况不做网络 loading 门槛。
 */
@Composable
fun CalendarScreen(
    onOpenDetail: (Long) -> Unit,
) {
    val viewModel: CalenderViewModel = appViewModel { CalenderViewModel(it) }
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = { AppTopAppBar(title = Strings.TITLE_CALENDAR) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                Strings.CALENDAR_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            AnimatedVisibility(visible = viewModel.offline) {
                ErrorBar(
                    message = Strings.OFFLINE_BANNER,
                    actionText = Strings.SYNC_NOW,
                    onAction = { viewModel.syncNow() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            WeekdayRow(
                selectedDay = viewModel.selectedDay,
                today = viewModel.today,
                onDaySelected = viewModel::updateSelectedDay,
            )

            val trackedIds = remember(tracks) { tracks.map { it.track.subjectId }.toSet() }
            val items = remember(subjects, viewModel.selectedDay) {
                calendarItems(subjects, viewModel.selectedDay)
            }

            // 已有内容时的后台刷新显示细进度条
            AnimatedVisibility(visible = viewModel.syncing && items.isNotEmpty()) {
                LinearProgressIndicator(
                    trackColor = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            when {
                // 正在拉目录且当前列表还空：显示骨架屏。
                // 行高与间距对齐 CalendarRow（封面 56dp + 上下 10dp）与列表的 spacedBy(8.dp)。
                viewModel.syncing && items.isEmpty() -> LoadingSkeleton(
                    rows = 8,
                    rowHeight = CALENDAR_ROW_HEIGHT,
                    spacing = 8.dp,
                )

                items.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { subject ->
                        CalendarRow(
                            subject = subject,
                            tracked = subject.id in trackedIds,
                            onOpenDetail = { onOpenDetail(subject.id) },
                        )
                    }
                }

                // 整库都没有条目：连缓存都没有，给手动拉取入口
                subjects.isEmpty() -> EmptyState(
                    title = Strings.EMPTY_CALENDAR_TITLE,
                    hint = Strings.EMPTY_CALENDAR_HINT,
                    actionText = Strings.SYNC_NOW,
                    onAction = { viewModel.syncNow() },
                    face = Strings.FACE_WAITING,
                )

                else -> EmptyState(
                    title = Strings.EMPTY_DAY_TITLE,
                    hint = Strings.EMPTY_DAY_HINT,
                )
            }
        }
    }
}

/**
 * 当日放送条目：只认处于放送窗口内（inCalendar）且放送星期等于 [day] 的条目，按评分降序，无评分排最后。
 */
internal fun calendarItems(subjects: List<Subject>, day: Int): List<Subject> =
    subjects
        .filter { it.inCalendar && it.airWeekday == day }
        .sortedByDescending { it.ratingScore ?: 0.0 }

/** 放送表行的实际高度：CalendarRow 的封面 56dp + 上下内边距各 10dp。骨架屏按它对齐。 */
private val CALENDAR_ROW_HEIGHT = 76.dp
