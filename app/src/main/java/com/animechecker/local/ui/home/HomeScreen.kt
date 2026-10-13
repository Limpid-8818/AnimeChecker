package com.animechecker.local.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animechecker.local.domain.AiredEps
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.domain.model.TrackEntry
import com.animechecker.local.domain.model.TrackStatus
import com.animechecker.local.domain.model.TrackedSubject
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.appViewModel
import com.animechecker.local.ui.components.AppTopAppBar
import com.animechecker.local.ui.components.CoverImage
import com.animechecker.local.ui.components.EmptyState
import com.animechecker.local.ui.components.EpisodeCheckSheet
import com.animechecker.local.ui.components.EpisodeProgress
import com.animechecker.local.ui.components.ErrorBar
import com.animechecker.local.ui.components.StatusPill
import kotlinx.coroutines.launch

/**
 * 首页（追番清单）：条目列表、进度操作入口、轻量刷新。
 *
 * 清单与条目完全来自 Room Flow，无整页 loading；进入本页触发一次追番对象轻量刷新，切页签不重复拉取。
 */
@Composable
fun HomeScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onGoExplore: () -> Unit,
) {
    val viewModel: HomeViewModel = appViewModel { HomeViewModel(it) }
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    // 筛选用 rememberSaveable：进详情返回后保持所选状态；默认"在看"
    var filter by rememberSaveable { mutableStateOf<TrackStatus?>(TrackStatus.DOING) }
    var editing by remember { mutableStateOf<TrackedSubject?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = Strings.TITLE_LIST,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = Strings.TITLE_SETTINGS)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(visible = viewModel.refreshError != null) {
                ErrorBar(
                    message = Strings.OFFLINE_BANNER +
                        (viewModel.refreshError?.let { "（${Strings.errorText(it.kind)}）" } ?: ""),
                    actionText = Strings.GO_SETTINGS,
                    onAction = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (rows.isNotEmpty()) {
                Text(
                    Strings.LIST_STATS_FMT.format(rows.size, rows.sumOf { it.track.watched.size }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            HomeFilterRow(selected = filter, rows = rows, onSelect = { filter = it })

            val visible = rows.filter { filter == null || it.track.status == filter }
            when {
                visible.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.track.localId }) { row ->
                        TrackCard(
                            row = row,
                            onIncrement = {
                                scope.launch {
                                    if (!viewModel.increment(row.track.localId)) {
                                        snackbar.showSnackbar(Strings.CAUGHT_UP)
                                    }
                                }
                            },
                            onDecrement = {
                                scope.launch {
                                    viewModel.decrement(row.track.localId)
                                    snackbar.showSnackbar(Strings.UNDO_HINT)
                                }
                            },
                            onEditProgress = { editing = row },
                            onClick = { onOpenDetail(row.track.subjectId) },
                        )
                    }
                }

                rows.isEmpty() -> EmptyState(
                    title = Strings.EMPTY_LIST_TITLE,
                    hint = Strings.EMPTY_LIST_HINT,
                    actionText = Strings.EMPTY_LIST_ACTION,
                    onAction = onGoExplore,
                    face = Strings.FACE_INVITE,
                )

                else -> EmptyState(title = Strings.EMPTY_FILTER_TITLE)
            }
        }
    }

    editing?.let { row ->
        EpisodeCheckSheet(
            title = row.subject?.nameCn ?: row.subject?.name ?: "",
            watched = row.track.watched,
            aired = AiredEps.subjectAired(row.subject),
            total = row.subject?.epsTotal,
            onToggleEpisode = { episode ->
                scope.launch { viewModel.toggleEpisode(row.track.localId, episode) }
            },
            onDismiss = { editing = null },
        )
    }
}

/** 状态筛选：全部 / 五态，带计数，选中态用弱容器色。 */
@Composable
private fun HomeFilterRow(
    selected: TrackStatus?,
    rows: List<TrackedSubject>,
    onSelect: (TrackStatus?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FILTERS.forEach { (status, label) ->
            val isSelected = selected == status
            Text(
                "$label ${rows.countOf(status)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(status) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

private val FILTERS: List<Pair<TrackStatus?, String>> = listOf(
    null to Strings.FILTER_ALL,
    TrackStatus.DOING to Strings.STATUS_DOING,
    TrackStatus.WISH to Strings.STATUS_WISH,
    TrackStatus.DONE to Strings.STATUS_DONE,
    TrackStatus.ON_HOLD to Strings.STATUS_HOLD,
    TrackStatus.DROPPED to Strings.STATUS_DROPPED,
)

/**
 * 追番卡片（设计规范 06 §3.2）：封面 + 标题 + 状态 pill + 进度 + 放送行，右下 +1 圆钮；
 * 长按 +1 圆钮撤销一集，点进度数字进集数勾选弹层。
 */
@Composable
private fun TrackCard(
    row: TrackedSubject,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditProgress: () -> Unit,
    onClick: () -> Unit,
) {
    val track = row.track
    val subject = row.subject
    val aired = AiredEps.subjectAired(subject)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(model = subject?.coverUrl, modifier = Modifier.size(width = 64.dp, height = 92.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                subject?.nameCn ?: subject?.name ?: "…",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (track.status == TrackStatus.DROPPED) {
                    TextDecoration.LineThrough
                } else {
                    null
                },
            )
            Spacer(Modifier.height(6.dp))
            StatusPill(track.status)
            Spacer(Modifier.height(8.dp))
            EpisodeProgress(
                watched = track.watched.size,
                total = subject?.epsTotal ?: aired?.takeIf { it > 0 },
                onNumberClick = onEditProgress,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(subject?.airWeekday?.let { Strings.weekdayName(it) } ?: Strings.NO_AIR_WEEKDAY)
                    when (aired) {
                        null -> Unit
                        0 -> append(" · ").append(Strings.NOT_AIRED)
                        else -> append(" · ").append(Strings.AIRED_COUNT.format(aired))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .combinedClickable(onClick = onIncrement, onLongClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = Strings.BTN_PLUS_ONE,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * 追番卡片预览
 */
@Preview(name = "TrackCard", showBackground = true)
@Composable
private fun TrackCardPreview() {
    MaterialTheme {
        TrackCard(
            row = TrackedSubject(
                track = TrackEntry(
                    localId = 1,
                    subjectId = 100,
                    status = TrackStatus.DOING,
                    watched = setOf(1, 2, 3, 4, 5),
                ),
                subject = Subject(
                    id = 100,
                    name = "Kimi no Koto ga Suki na 100-nin no Kanojo",
                    nameCn = "超超超超超喜欢你的100个女朋友",
                    epsTotal = 12,
                    airWeekday = 5,
                    nextEpisode = 6,
                    nextAiringAt = System.currentTimeMillis() / 1000 + 86_400,
                ),
            ),
            onIncrement = {},
            onDecrement = {},
            onEditProgress = {},
            onClick = {},
        )
    }
}
