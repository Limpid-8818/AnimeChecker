package com.animechecker.local.data.anilist

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * AniList HTTP 通道：节流、重试与响应信封校验。
 *
 * 约定：
 * - 直连 `graphql.anilist.co`，无鉴权；请求显式带 User-Agent，缺 UA 会被前置 CDN 判 403；
 * - 服务端限流 30 次/分钟：相邻请求间隔 ≥ [minIntervalMs]，429 按 Retry-After 退避；
 * - [maxRetry] 统一含义为「最多重试次数」：网络抖动与限流同一语义，最多发起 [maxRetry] + 1 次请求；
 * - 只捕获 [IOException]：[CancellationException] 必须继续向上传播，切页取消不得算作网络失败；
 * - 调用方的 [HttpClient] 需安装 HttpTimeout 插件：本类用 per-request 超时，使 AniList（[requestTimeoutMs]）与 LLM 长请求可以共用一个连接池。
 */
internal class AniListTransport(
    private val client: HttpClient,
    private val minIntervalMs: Long = MIN_INTERVAL_MS,
    private val maxRetry: Int = MAX_RETRY,
    private val requestTimeoutMs: Long = REQUEST_TIMEOUT_MS,
    private val networkBackoffMs: Long = NETWORK_BACKOFF_MS,
    private val userAgent: String = DEFAULT_USER_AGENT,
) {
    companion object {
        private const val ENDPOINT = "https://graphql.anilist.co"
        private const val MIN_INTERVAL_MS = 2_200L
        private const val MAX_RETRY = 3
        private const val REQUEST_TIMEOUT_MS = 30_000L
        private const val NETWORK_BACKOFF_MS = 1_500L
        private const val RETRY_AFTER_DEFAULT_SECONDS = 10L
        private const val RETRY_AFTER_CAP_SECONDS = 60L
        private const val DEFAULT_USER_AGENT = "AnimeChecker (Android; Kotlin/Ktor)"

        private val JSON = Json { ignoreUnknownKeys = true }
    }

    private val rateMutex = Mutex()
    private var lastRequestAt = 0L

    /**
     * 发送查询并解出信封的 data 段。
     *
     * [serializer] 对应 data 段的类型（`PageData` / `MediaData`）。
     * HTTP 非 2xx、GraphQL errors、data 缺失、限流或网络重试耗尽，一律抛 [AniListException]。
     */
    suspend fun <T> post(query: String, variables: JsonObject, serializer: KSerializer<T>): T {
        val payload = JSON.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("query", query)
                put("variables", variables)
            },
        )
        var networkFailures = 0
        var rateLimited = 0
        while (true) {
            throttle()
            val response = try {
                client.post(ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.UserAgent, this@AniListTransport.userAgent)
                    timeout { requestTimeoutMillis = requestTimeoutMs }
                    setBody(payload)
                }
            } catch (e: IOException) {
                networkFailures++
                if (networkFailures > maxRetry) {
                    throw AniListException(
                        AniListException.Kind.NETWORK,
                        "连接 AniList 失败：${e.message ?: e.javaClass.simpleName}",
                        e,
                    )
                }
                delay((networkBackoffMs * networkFailures).milliseconds)
                continue
            }
            when (response.status.value) {
                429 -> {
                    rateLimited++
                    if (rateLimited > maxRetry) {
                        throw AniListException(
                            AniListException.Kind.RATE_LIMITED,
                            "AniList 限流，退避重试后仍未成功",
                        )
                    }
                    val waitSeconds = response.headers["Retry-After"]?.toLongOrNull()
                        ?: RETRY_AFTER_DEFAULT_SECONDS
                    delay((waitSeconds.coerceAtMost(RETRY_AFTER_CAP_SECONDS) * 1000).milliseconds)
                }
                in 200..299 -> {
                    val envelope = JSON.decodeFromString(
                        Envelope.serializer(serializer),
                        response.bodyAsText(),
                    )
                    if (envelope.errors.isNotEmpty()) {
                        throw AniListException(
                            AniListException.Kind.GRAPHQL,
                            "AniList 返回错误：${envelope.errors.first().message}",
                        )
                    }
                    return envelope.data ?: throw AniListException(
                        AniListException.Kind.NOT_FOUND,
                        "AniList 未返回数据",
                    )
                }
                else -> {
                    val rejected = graphQlErrorMessage(response)
                    throw if (rejected != null) {
                        AniListException(AniListException.Kind.GRAPHQL, "AniList 拒绝请求：$rejected")
                    } else {
                        AniListException(
                            AniListException.Kind.HTTP,
                            "AniList 请求失败（HTTP ${response.status.value}）",
                        )
                    }
                }
            }
        }
    }

    /** 相邻请求间隔 ≥ [minIntervalMs]；在锁内等待，保证请求串行发起。 */
    private suspend fun throttle() {
        rateMutex.withLock {
            val wait = lastRequestAt + minIntervalMs - System.currentTimeMillis()
            if (wait > 0) delay(wait.milliseconds)
            lastRequestAt = System.currentTimeMillis()
        }
    }

    /** 非 2xx 响应体里可能带 GraphQL errors（如查询语法错误被 400 拒），取出来便于定位。 */
    private suspend fun graphQlErrorMessage(response: HttpResponse): String? = try {
        JSON.parseToJsonElement(response.bodyAsText())
            .jsonObject["errors"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
