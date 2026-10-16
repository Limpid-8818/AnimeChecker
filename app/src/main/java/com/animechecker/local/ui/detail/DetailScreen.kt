package com.animechecker.local.ui.detail

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animechecker.local.data.repository.AppError
import com.animechecker.local.domain.AiredEps
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.domain.model.TrackEntry
import com.animechecker.local.domain.model.TrackStatus
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.appViewModel
import com.animechecker.local.ui.components.AppCard
import com.animechecker.local.ui.components.AppTopAppBar
import com.animechecker.local.ui.components.CoverImage
import com.animechecker.local.ui.components.EpisodeCheckSheet
import com.animechecker.local.ui.components.EpisodeProgress
import com.animechecker.local.ui.components.RatingText
import kotlinx.coroutines.launch

/**
 * 条目详情：条目信息、简介、我的进度与状态、评分备注、入退番。
 */
@Composable
fun DetailScreen(
    subjectId: Long,
    onBack: () -> Unit,
) {
    val viewModel: DetailViewModel = appViewModel { DetailViewModel(it, subjectId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // 简介看译文还是原文：工具性偏好，随页面保存，不进设置
    var showOriginal by rememberSaveable { mutableStateOf(false) }
    var editingEpisodes by remember { mutableStateOf(false) }
    var editingRating by remember { mutableStateOf(false) }
    var editingComment by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { AppTopAppBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // 未加入清单时才占底部一条：全宽"加入追番"，其余情况内容自行收尾
            if (state.loaded && state.track == null) {
                Button(
                    onClick = { scope.launch { viewModel.add() } },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(Strings.ADD_TO_LIST)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val subject = state.subject
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                !state.loaded -> Unit

                subject == null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        Strings.errorText(viewModel.loadError?.kind ?: AppError.Kind.NOT_FOUND),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { viewModel.load() }) { Text(Strings.RETRY) }
                }

                else -> DetailContent(
                    subject = subject,
                    track = state.track,
                    showOriginal = showOriginal,
                    onToggleOriginal = { showOriginal = !showOriginal },
                    onEditEpisodes = { editingEpisodes = true },
                    onEditRating = { editingRating = true },
                    onEditComment = { editingComment = true },
                    onRemove = { confirmRemove = true },
                    onPickStatus = { status ->
                        state.track?.let { track ->
                            scope.launch {
                                viewModel.setStatus(
                                    track.localId,
                                    status
                                )
                            }
                        }
                    },
                    onCatchup = {
                        state.track?.let { track ->
                            scope.launch {
                                if (!viewModel.catchup(track.localId)) {
                                    snackbar.showSnackbar(Strings.CATCHUP_UNAVAILABLE)
                                }
                            }
                        }
                    },
                    onIncrement = {
                        state.track?.let { track ->
                            scope.launch {
                                if (!viewModel.increment(track.localId) &&
                                    snackbar.currentSnackbarData?.visuals?.message != Strings.CAUGHT_UP
                                ) {
                                    snackbar.showSnackbar(Strings.CAUGHT_UP)
                                }
                            }
                        }
                    },
                    onDecrement = {
                        state.track?.let { track ->
                            scope.launch {
                                if (viewModel.decrement(track.localId)) {
                                    snackbar.showSnackbar(Strings.UNDO_HINT)
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    // ---------- 对话框与弹层 ----------

    val track = state.track
    val subject = state.subject

    if (confirmRemove && track != null && subject != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(Strings.REMOVE_CONFIRM_TITLE) },
            text = { Text(Strings.REMOVE_CONFIRM_TEXT.format(subject.nameCn ?: subject.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    scope.launch { viewModel.removeTrack(track.localId) }
                }) {
                    Text(Strings.CONFIRM_REMOVE, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(Strings.CANCEL) }
            },
        )
    }

    if (editingRating && track != null) {
        AlertDialog(
            onDismissRequest = { editingRating = false },
            title = { Text(Strings.MY_RATING) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..10).chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            row.forEach { score ->
                                ScoreButton(score, selected = track.rating == score) {
                                    scope.launch { viewModel.setRating(track.localId, score) }
                                    editingRating = false
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        scope.launch { viewModel.setRating(track.localId, 0) }
                        editingRating = false
                    }) {
                        Text(
                            Strings.CLEAR_RATING,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { editingRating = false }) { Text(Strings.CANCEL) }
            },
        )
    }

    if (editingComment && track != null) {
        var draft by remember(editingComment) { mutableStateOf(track.comment.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingComment = false },
            title = { Text(Strings.MY_COMMENT) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(Strings.COMMENT_PLACEHOLDER) },
                    minLines = 2,
                    maxLines = 4,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.setComment(track.localId, draft) }
                    editingComment = false
                }) {
                    Text(Strings.SAVE)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingComment = false }) { Text(Strings.CANCEL) }
            },
        )
    }

    if (editingEpisodes && track != null && subject != null) {
        EpisodeCheckSheet(
            title = subject.nameCn ?: subject.name,
            watched = track.watched,
            aired = AiredEps.subjectAired(subject),
            total = subject.epsTotal,
            onToggleEpisode = { episode ->
                scope.launch {
                    viewModel.toggleEpisode(
                        track.localId,
                        episode
                    )
                }
            },
            onDismiss = { editingEpisodes = false },
        )
    }
}

// ---------- 内容 ----------

@Composable
private fun DetailContent(
    subject: Subject,
    track: TrackEntry?,
    showOriginal: Boolean,
    onToggleOriginal: () -> Unit,
    onEditEpisodes: () -> Unit,
    onEditRating: () -> Unit,
    onEditComment: () -> Unit,
    onRemove: () -> Unit,
    onPickStatus: (TrackStatus) -> Unit,
    onCatchup: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val aired = AiredEps.subjectAired(subject)
    val info = infoCells(subject, aired)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Header(subject)

        // 数据行：已放送 / 总集数 / 首播 三格同卡
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoCell(info.aired)
            InfoCell(info.total)
            InfoCell(info.firstAir)
        }

        if (track != null) {
            // 我的进度：状态 chips + 进度与 +1 圆钮同卡 + 追平 / 移出清单
            Spacer(Modifier.height(16.dp))
            SectionTitle(Strings.MY_PROGRESS)
            Spacer(Modifier.height(8.dp))
            StatusChipsRow(current = track.status, onPick = onPickStatus)
            Spacer(Modifier.height(8.dp))
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EpisodeProgress(
                        watched = track.watched.size,
                        total = subject.epsTotal ?: aired?.takeIf { it > 0 },
                        onNumberClick = onEditEpisodes,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
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
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onCatchup) { Text(Strings.BTN_CATCHUP) }
                TextButton(onClick = onRemove) {
                    Text(Strings.REMOVE_TRACK, color = MaterialTheme.colorScheme.error)
                }
            }

            // 评分与备注
            Spacer(Modifier.height(8.dp))
            AppCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Strings.MY_RATING, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        track.rating?.let { "★ $it" } ?: Strings.UNRATED,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (track.rating != null) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEditRating)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(10.dp))
                Text(
                    track.comment?.takeIf { it.isNotBlank() } ?: Strings.COMMENT_PLACEHOLDER,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (track.comment.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditComment)
                        .padding(vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SummarySection(
            subject = subject,
            showOriginal = showOriginal,
            onToggleOriginal = onToggleOriginal,
        )
    }
}

/** 头部：封面 96×136 + 名称、原名、评分、季度与放送日。 */
@Composable
private fun Header(subject: Subject) {
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        CoverImage(
            model = subject.coverUrl,
            modifier = Modifier.size(width = 96.dp, height = 136.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                subject.nameCn ?: subject.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subject.nameCn != null && subject.nameCn != subject.name) {
                Text(
                    subject.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            RatingText(subject.ratingScore)
            val meta = listOfNotNull(
                subject.season?.let { Strings.seasonTitle(it) },
                subject.airWeekday?.let { Strings.weekdayName(it) },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 简介：默认显示译文，译文与原文都在库时右上角给出切换入口；
 * 正文默认 3 行折叠，实际溢出可展开。
 *
 * 标题行恒定预留 [SECTION_HEADER_MIN_HEIGHT]：切换入口只在"译文与原文都在库"时出现，
 * 翻译写回 Room 的瞬间入口才会冒出来，若不预留，这一行会从标题高长到按钮高，
 * 下面是整块简介跟着跳一下。
 */
@Composable
private fun SummarySection(
    subject: Subject,
    showOriginal: Boolean,
    onToggleOriginal: () -> Unit,
) {
    val display = summaryDisplay(subject, showOriginal)
    var expanded by remember { mutableStateOf(false) }
    // 按当前正文记：切换译文/原文后重新判断是否溢出
    var overflowing by remember(display.text) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SECTION_HEADER_MIN_HEIGHT)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Strings.SUMMARY, style = MaterialTheme.typography.titleMedium)
        if (display.canToggle) {
            // 无内边距：让链接文字右端与卡片右缘对齐，与左侧标题对称
            TextButton(onClick = onToggleOriginal, contentPadding = PaddingValues(0.dp)) {
                Text(
                    if (showOriginal) Strings.SUMMARY_VIEW_TRANSLATED else Strings.SUMMARY_VIEW_ORIGINAL,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    AppCard {
        Text(
            display.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else SUMMARY_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow },
        )
        if (display.showOriginalNote) {
            Text(
                Strings.SUMMARY_ORIGINAL_NOTE,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (overflowing || expanded) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(if (expanded) Strings.COLLAPSE else Strings.EXPAND)
            }
        }
    }
}

/** 数据行的一格：与卡片同级、次级色小字。 */
@Composable
private fun InfoCell(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 我的进度状态行：五态横滚，选中态用弱容器色，与首页筛选 chips 同一形态。 */
@Composable
private fun StatusChipsRow(current: TrackStatus, onPick: (TrackStatus) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrackStatus.entries.forEach { item ->
            StatusChip(
                status = item,
                selected = item == current,
                onClick = { onPick(item) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun StatusChip(status: TrackStatus, selected: Boolean, onClick: () -> Unit) {
    Text(
        Strings.statusName(status),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun ScoreButton(value: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$value",
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 简介折叠行数。 */
private const val SUMMARY_COLLAPSED_LINES = 3

/**
 * 简介标题行最小高度：等于右侧"查看原文"文字按钮的实际高度（Material 文字按钮的最小交互尺寸 48dp）。
 * 有无入口时行高相同，翻译写回 Room 后简介整块不会跳动。
 */
private val SECTION_HEADER_MIN_HEIGHT = 48.dp
