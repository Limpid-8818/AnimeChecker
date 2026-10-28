package com.animechecker.local.ui.widget

import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 组件封面加载。
 * 通过Coil加载器加载。
 * 失败返回null，界面回退纯文字卡片。
 */
internal suspend fun loadWidgetCover(context: Context, url: String?): Bitmap? {
    if (url == null) return null
    return withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(COVER_SIZE_PX)
            .build()
        (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
    }
}

/** 封面加载状态。 */
internal sealed interface CoverState {
    data object Loading : CoverState
    data class Ready(val bitmap: Bitmap) : CoverState
    data object Failed : CoverState
}

private const val COVER_SIZE_PX = 512
