package com.animechecker.local

import android.app.Application
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.collection.intSetOf
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.animechecker.local.di.AppContainer
import com.animechecker.local.ui.widget.AnimeCheckerWidget
import com.animechecker.local.ui.widget.AnimeCheckerWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

class AnimeCheckerApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        observeDataChangedForWidget()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            publishWidgetPreviews()
        }
    }

    /**
     * 共享图片加载器
     * 配置封面图磁盘缓存，应用内列表与桌面组件共用缓存。
     */
    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()

    /** 发布由Glance动态生成的组件选择器预览；API 35+可用。 */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun publishWidgetPreviews() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                GlanceAppWidgetManager(this@AnimeCheckerApp)
                    .setWidgetPreviews(
                        AnimeCheckerWidgetReceiver::class,
                        intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
                    )
            }
        }
    }

    /**
     * 监听数据更新，触发组件update。
     */
    private fun observeDataChangedForWidget() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            container.repository.dataChanged.collect {
                runCatching {
                    val widget = AnimeCheckerWidget()
                    GlanceAppWidgetManager(this@AnimeCheckerApp)
                        .getGlanceIds(AnimeCheckerWidget::class.java)
                        .forEach { id -> widget.update(this@AnimeCheckerApp, id) }
                }
            }
        }
    }
}
