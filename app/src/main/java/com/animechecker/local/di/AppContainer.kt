package com.animechecker.local.di

import android.content.Context
import com.animechecker.local.BuildConfig
import com.animechecker.local.data.anilist.AniListClient
import com.animechecker.local.data.anilist.AniListTransport
import com.animechecker.local.data.llm.LlmClient
import com.animechecker.local.data.local.db.AppDatabase
import com.animechecker.local.data.local.db.DatabaseFactory
import com.animechecker.local.data.repository.AppRepository
import com.animechecker.local.data.settings.SettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * 手动依赖装配：唯一组合根，仅在 [com.animechecker.local.AnimeCheckerApp] 创建一次，成员一律 lazy。
 *
 * 数据源成员（AppDatabase / AniListClient / LlmClient / SettingsStore）只被 AppRepository 消费，不暴露给 UI。
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val versionName: String = BuildConfig.VERSION_NAME

    private val database: AppDatabase by lazy { DatabaseFactory.create(appContext) }

    /**
     * 共用连接池：AniList 由 [AniListTransport] 按请求设 30s 超时，翻译长文本沿用这里的 120s。
     */
    private val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    retryOnConnectionFailure(true)
                    connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    readTimeout(LONG_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    callTimeout(LONG_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = LONG_REQUEST_TIMEOUT_SECONDS * 1000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = false
                        explicitNulls = false
                    },
                )
            }
        }
    }

    private val anilist: AniListClient by lazy {
        AniListClient(
            AniListTransport(
                client = httpClient,
                userAgent = "AnimeChecker/$versionName (Android)",
            ),
        )
    }

    /** LLM 翻译客户端。 */
    private val llm by lazy { LlmClient(httpClient) }

    private val settings by lazy { SettingsStore(appContext) }

    /** 应用级作用域，后台翻译等不绑定界面的任务。 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: AppRepository by lazy {
        AppRepository(database, anilist, llm, settings, applicationScope)
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val LONG_REQUEST_TIMEOUT_SECONDS = 120L
    }
}
