package com.animechecker.local.di

import android.content.Context

/**
 * 手动依赖装配。
 *
 * 约定：
 * - 仅在 [com.animechecker.local.AnimeCheckerApp] 创建一次，成员一律 lazy；
 * - 数据源成员（AppDatabase / AniListClient / LlmClient / SettingsStore）只被AppRepository 消费，不直接暴露给 UI。
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext
}
