package com.animechecker.local.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.appViewModel
import com.animechecker.local.ui.components.AppTopAppBar
import com.animechecker.local.ui.components.EmptyState
import com.animechecker.local.ui.components.ErrorBar

/**
 * 番库（季度目录）：按季度浏览，或输入关键词搜索 AniList。
 *
 * 关键词与搜索结果都放在 ExploreViewModel 里，从搜索结果进详情再返回时原样保留；
 * 季度目录只在本页首次进入时拉一次，之后靠切季度或"重试"手动触发。
 */
@Composable
fun ExploreScreen(
    onOpenDetail: (Long) -> Unit,
) {
    val viewModel: ExploreViewModel = appViewModel { ExploreViewModel(it) }
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    // 记录已拉取
    var loadedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 上次是拉到一半离开的，这里补拉；否则只在首次进入时拉一次
        val stale = viewModel.consumeSeasonStale()
        if (!loadedOnce || stale) {
            loadedOnce = true
            viewModel.refreshSeason()
        }
    }
    // 离开本页丢掉在途请求
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelInFlight() }
    }

    Scaffold(
        topBar = { AppTopAppBar(title = Strings.TITLE_EXPLORE) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = viewModel.keyword,
                onValueChange = viewModel::updateKeyword,
                placeholder = { Text(Strings.SEARCH_HINT) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            AnimatedVisibility(visible = viewModel.searchFromCache) {
                ErrorBar(
                    message = Strings.SEARCH_OFFLINE_HINT,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            AnimatedVisibility(visible = viewModel.offline && viewModel.searchItems == null) {
                ErrorBar(
                    message = Strings.OFFLINE_BANNER,
                    actionText = Strings.RETRY,
                    onAction = { viewModel.refreshSeason() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            val keyword = viewModel.keyword.trim()
            val results = viewModel.searchItems
            val items = remember(subjects, viewModel.selectedSeason) {
                seasonItems(subjects, viewModel.selectedSeason)
            }

            // 季度目录在途且已有内容时的细进度条。
            AnimatedVisibility(visible = viewModel.loading && results == null && items.isNotEmpty()) {
                LinearProgressIndicator(
                    trackColor = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            when {
                // 搜索在途：刷新指示器
                viewModel.loading && keyword.isNotEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                results != null -> if (results.isEmpty()) {
                    EmptyState(
                        title = if (keyword.length < MIN_SEARCH_LENGTH) {
                            Strings.SEARCH_TOO_SHORT
                        } else {
                            Strings.SEARCH_NO_RESULT
                        },
                    )
                } else {
                    PosterGrid(items = results, onOpenDetail = onOpenDetail)
                }

                else -> {
                    SeasonChipsRow(
                        options = viewModel.seasonOptions,
                        selected = viewModel.selectedSeason,
                        onSelect = viewModel::selectSeason,
                    )
                    when {
                        viewModel.loading && items.isEmpty() -> PosterGridSkeleton()
                        items.isEmpty() -> EmptyState(title = Strings.EXPLORE_EMPTY)
                        else -> PosterGrid(items = items, onOpenDetail = onOpenDetail)
                    }
                }
            }
        }
    }
}

/** 当季条目：按所选季度过滤后按评分降序，无评分排最后。 */
internal fun seasonItems(subjects: List<Subject>, season: Season): List<Subject> =
    subjects
        .filter { it.season == season }
        .sortedByDescending { it.ratingScore ?: 0.0 }

/** 搜索关键词下限。 */
private const val MIN_SEARCH_LENGTH = 2

/** 海报墙骨架：与 PosterGrid 同样的两列、间距与卡片形状。 */
@Composable
private fun PosterGridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(POSTER_SKELETON_COUNT) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(6.dp))
                SkeletonLine(fraction = 1f)
                Spacer(Modifier.height(4.dp))
                SkeletonLine(fraction = 0.6f)
            }
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

private const val POSTER_SKELETON_COUNT = 6
