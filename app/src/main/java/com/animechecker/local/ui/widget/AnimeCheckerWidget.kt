package com.animechecker.local.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.animechecker.local.AnimeCheckerApp
import com.animechecker.local.MainActivity
import com.animechecker.local.R
import com.animechecker.local.domain.AiredEps
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.theme.DarkColors
import com.animechecker.local.ui.theme.LightColors
import kotlinx.coroutines.flow.first
import androidx.core.graphics.createBitmap
import androidx.glance.unit.ColorProvider

/**
 * 在看追番单卡片桌面组件。
 */
class AnimeCheckerWidget: GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 取首屏数据，消除加载空窗
        val repository = (context.applicationContext as AnimeCheckerApp).container.repository
        val initialRows = repository.tracksWithSubjects().first()
        provideContent {
            val rows by repository.tracksWithSubjects().collectAsState(initial = initialRows)
            GlanceTheme(colors = WidgetColors.providers) {
                WidgetSurface(items = widgetItems(rows))
            }
        }
    }

    /**
     * API 35+组件选择器动态预览。
     * 启动时经 [androidx.glance.appwidget.GlanceAppWidgetManager.setWidgetPreviews] 发布。
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme(colors = WidgetColors.providers) {
                WidgetSurface(items = previewItems(context), previewCover = previewCoverPlaceholder())
            }
        }
    }

    companion object {
        /** 当前条目锚点。 */
        val AnchorKey = longPreferencesKey("widget_anchor_subject_id")

        /** 预览示例。 */
        private fun previewItems(context: Context): List<WidgetItem> = listOf(
            WidgetItem(
                localId = 1,
                subjectId = 1,
                title = context.getString(R.string.widget_preview_title_a),
                coverUrl = null,
                watchedCount = 4,
                aired = 6,
                epsTotal = 12,
                airWeekday = AiredEps.today().dayOfWeek.value,
                isToday = true,
            ),
            WidgetItem(
                localId = 2,
                subjectId = 2,
                title = context.getString(R.string.widget_preview_title_b),
                coverUrl = null,
                watchedCount = 7,
                aired = 7,
                epsTotal = 24,
                airWeekday = 6,
                isToday = false,
            ),
        )

        /** 预览封面占位。 */
        private fun previewCoverPlaceholder(): Bitmap {
            return createBitmap(148, 208).apply {
                eraseColor(0xFFE3F0FF.toInt())
            }
        }
    }
}

/** 组件配色，包含深浅两种配色。 */
private object WidgetColors {
    val providers = ColorProviders(light = LightColors, dark = DarkColors)
}

/** 封面尺寸（4x2 定稿）：文字列高度与封面同高，锚定上下两端。 */
private val CoverWidth = 86.dp
private val CoverHeight = 128.dp

/**
 * 组件根容器。
 */
@Composable
private fun WidgetSurface(items: List<WidgetItem>, previewCover: Bitmap? = null) {
    val context = LocalContext.current
    val anchor = currentState(AnimeCheckerWidget.AnchorKey)
    val current = items.getOrNull(anchorIndex(items, anchor))
    // 封面随当前条目异步加载，切卡后按coverUrl重新加载
    val loadedCover by produceState<Bitmap?>(initialValue = null, key1 = current?.coverUrl) {
        value = loadWidgetCover(context, current?.coverUrl)
    }
    Scaffold(
        titleBar = { WidgetHeader() },
        backgroundColor = GlanceTheme.colors.surface,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                items.isEmpty() -> Text(
                    text = Strings.WIDGET_EMPTY,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                    modifier = GlanceModifier.clickable(
                        openAppAction(context, subjectId = null),
                    ),
                )
                else -> ItemCard(item = current!!, cover = previewCover ?: loadedCover)
            }
        }
    }
}

/**
 * 标头：应用图标 + 标题 + 循环切换箭头。
 */
@Composable
private fun WidgetHeader() {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = GlanceModifier
                .size(24.dp)
                .cornerRadius(6.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = Strings.WIDGET_TITLE,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.defaultWeight())
        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.ic_widget_chevron_left),
            contentDescription = Strings.WIDGET_PREV,
            contentColor = GlanceTheme.colors.primary,
            backgroundColor = null,
            onClick = actionRunCallback<WidgetPrevAction>(),
            modifier = GlanceModifier.size(36.dp),
        )
        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.ic_widget_chevron_right),
            contentDescription = Strings.WIDGET_NEXT,
            contentColor = GlanceTheme.colors.primary,
            backgroundColor = null,
            onClick = actionRunCallback<WidgetNextAction>(),
            modifier = GlanceModifier.size(36.dp),
        )
    }
}

/**
 * 追番条目卡片，显示信息布局与应用内卡片保持一致。
 */
@SuppressLint("RestrictedApi") // ColorProvider 误报
@Composable
private fun ItemCard(item: WidgetItem, cover: Bitmap?) {
    val total = item.epsTotal ?: item.aired?.takeIf { it > 0 }
    val denominator = (total ?: item.watchedCount).coerceAtLeast(1).toFloat()
    val progress = (item.watchedCount / denominator).coerceIn(0f, 1f)
    Row(
        modifier = GlanceModifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(openAppAction(LocalContext.current, subjectId = item.subjectId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) {
            Image(
                provider = ImageProvider(cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier
                    .size(width = CoverWidth, height = CoverHeight)
                    .cornerRadius(10.dp),
            )
            Spacer(GlanceModifier.width(14.dp))
        }
        // 文字列
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .height(CoverHeight),
        ) {
            Text(
                text = item.title ?: Strings.WIDGET_TITLE_MISSING,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${item.watchedCount}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = " / ${total ?: "?"}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                    modifier = GlanceModifier.padding(bottom = 4.dp),
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.outline,
            )
            Spacer(GlanceModifier.height(4.dp))
            // 星期标签在今日放送时整段变蓝底白字 pill 以起到提醒作用。
            // 两种状态都保留 pill 的内边距，切卡时行宽不跳变。
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                val today = item.isToday
                Text(
                    text = item.airWeekday?.let { Strings.weekdayName(it) } ?: Strings.NO_AIR_WEEKDAY,
                    style = TextStyle(
                        color = if (today) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier
                        .background(
                            if (today) GlanceTheme.colors.primary else ColorProvider(Color.Transparent),
                        )
                        .cornerRadius(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                val airedText = when (item.aired) {
                    null -> null
                    0 -> Strings.NOT_AIRED
                    else -> Strings.AIRED_COUNT.format(item.aired)
                }
                if (airedText != null) {
                    Text(
                        text = "·",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(horizontal = 1.dp)
                    )
                    Text(
                        text = airedText,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(start = 6.dp),
                    )
                }
            }
        }
        Spacer(GlanceModifier.width(12.dp))
        if (caughtUp(item.watchedCount, item.aired, item.epsTotal)) {
            Text(
                text = Strings.WIDGET_CAUGHT_UP,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1,
            )
        } else {
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = Strings.BTN_PLUS_ONE,
                contentColor = GlanceTheme.colors.onPrimary,
                backgroundColor = GlanceTheme.colors.primary,
                onClick = actionRunCallback<IncrementAction>(
                    actionParametersOf(IncrementAction.LocalIdKey to item.localId),
                ),
            )
        }
    }
}

/** 循环切换：查询列表定位锚点，获取切换后的列表索引后写回状态并触发重组。 */
private suspend fun shift(context: Context, glanceId: GlanceId, delta: Int) {
    val repository = (context.applicationContext as AnimeCheckerApp).container.repository
    val items = widgetItems(repository.tracksWithSubjects().first())
    if (items.size < 2) return
    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
    val current = anchorIndex(items, prefs[AnimeCheckerWidget.AnchorKey])
    val next = items[stepIndex(current, items.size, delta)]
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[AnimeCheckerWidget.AnchorKey] = next.subjectId
        }
    }
    AnimeCheckerWidget().update(context, glanceId)
}

/** 标头动作：切到上一个条目（循。 */
class WidgetPrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        shift(context, glanceId, -1)
    }
}

/** 标头动作：切到下一个条目（循。 */
class WidgetNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        shift(context, glanceId, +1)
    }
}

/** 卡片动作：+1 记录一集。 */
class IncrementAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val localId = parameters[LocalIdKey] ?: return
        val repository = (context.applicationContext as AnimeCheckerApp).container.repository
        repository.increment(localId)
        // 显式 update 防止因进程冷启动未触发重组
        AnimeCheckerWidget().update(context, glanceId)
    }

    companion object {
        val LocalIdKey = ActionParameters.Key<Long>("localId")
    }
}

/** 打开应用，携带条目 id 直接跳转到详情页。 */
private fun openAppAction(context: Context, subjectId: Long?): Action {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (subjectId != null) {
        intent.putExtra(MainActivity.EXTRA_SUBJECT_ID, subjectId)
    }
    return actionStartActivity(intent)
}
