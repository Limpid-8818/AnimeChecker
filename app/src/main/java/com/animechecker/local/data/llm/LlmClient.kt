/**
 * LLM 翻译服务：OpenAI 兼容接口客户端与翻译队列策略。
 *
 * - 标题批量全量入队；简介仅 needSummary 标记（追番对象 + 打开过详情）的条目入队；
 * - 失败置 FAILED，等待手动重试，不自动重试；
 * - 推理型模型默认禁用 thinking；网关对未知字段回 400 时去参重试；「测试连接」不因空正文判失败。
 */
package com.animechecker.local.data.llm

import com.animechecker.local.domain.model.LlmSettings
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.IOException

/** 翻译服务请求失败。[kind] 供上层映射为用户文案，[message] 只面向日志。 */
internal class LlmException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    enum class Kind {
        /** 鉴权失败（401 / 403）：API Key 无效或未授权。 */
        AUTH,

        /** 非 2xx 且不属于鉴权问题。 */
        HTTP,

        /** 响应不是合法的 OpenAI 兼容结构。 */
        FORMAT,

        /** 结构合法但正文为空（推理模型可能把预算花在思维链上）。 */
        EMPTY,

        /** 网络连接失败：DNS、路由、TLS 抖动或请求超时。 */
        NETWORK,
    }
}

/** 翻译动作。 */
internal interface LlmTranslator {

    /** 批量翻译标题，返回 `id to 译名`；缺失或解析失败的条目不在结果里。 */
    suspend fun translateTitles(
        settings: LlmSettings,
        items: List<Pair<Long, String>>,
    ): List<Pair<Long, String>>

    /** 翻译简介长文本，返回纯文本译文。 */
    suspend fun translateText(settings: LlmSettings, text: String): String

    /** 测试连接：HTTP 200 且响应结构合法即视为可用。 */
    suspend fun test(settings: LlmSettings)
}

/**
 * OpenAI 兼容 `POST {base}/chat/completions` 客户端：
 * - 标题批量：一次请求译 ≤ [TITLE_BATCH_LIMIT] 条，返回行协议；
 * - 简介逐条：长文本单独请求，返回纯文本译文。
 */
internal class LlmClient(private val client: HttpClient) : LlmTranslator {

    private companion object {
        // 翻译无需开启思考模式（deepseek 等推理模型默认开启且 effort=high，思维链会吃掉小预算导致正文为空）。
        const val TITLE_MAX_TOKENS = 8000
        const val SUMMARY_MAX_TOKENS = 8000
        const val TEST_MAX_TOKENS = 512

        const val TITLE_SYSTEM =
            "把输入的动画标题译为简体中文。有通行中文译名时使用通行译名，" +
                "没有时按字面简洁翻译，不要加书名号。季数写为「第X季」形式，标题内的年份括号原样保留。" +
                "输出格式：每行一条，形如 编号|译名（例如 235|名侦探柯南），顺序与条数与输入一致，不要输出其它内容。"

        const val SUMMARY_SYSTEM =
            "把输入的英文动画简介译为简体中文。意译，保留原文信息量与大致篇幅。" +
                "人物姓名：若为汉语拼音（中文作品的罗马音），还原为通行中文名；" +
                "其余人名保留原文写法，不要自行猜测汉字。文末的资料来源标注（如 Source: …）省略。" +
                "不添加解释或评论，只输出译文正文。"

        const val TEST_SYSTEM = "只回复两个字符：ok"

        const val TITLE_BATCH_LIMIT = 10

        val TITLE_LINE = Regex("^(\\d+)\\|(.+)$")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 批量翻译标题，返回 `id to 译名`；缺失或解析失败的条目不在结果里。
     *
     * 行协议（编号|译名）：推理模型输出 JSON 时会系统性产生多余引号一类的笔误，
     * 行格式不经过 JSON 引号，解析天然免疫。
     */
    override suspend fun translateTitles(
        settings: LlmSettings,
        items: List<Pair<Long, String>>,
    ): List<Pair<Long, String>> {
        val out = mutableListOf<Pair<Long, String>>()
        items.chunked(TITLE_BATCH_LIMIT).forEach { batch ->
            val user = batch.joinToString("\n") { (id, title) -> "$id|$title" }
            val content = chat(settings, TITLE_SYSTEM, user, maxTokens = TITLE_MAX_TOKENS)
            out += parseTitleLines(content)
        }
        return out
    }

    override suspend fun translateText(settings: LlmSettings, text: String): String =
        chat(settings, SUMMARY_SYSTEM, text, maxTokens = SUMMARY_MAX_TOKENS)

    override suspend fun test(settings: LlmSettings) {
        chat(settings, TEST_SYSTEM, "ping", maxTokens = TEST_MAX_TOKENS, requireContent = false)
    }

    // 私有请求方法

    private suspend fun chat(
        settings: LlmSettings,
        system: String,
        user: String,
        maxTokens: Int,
        requireContent: Boolean = true,
    ): String {
        val url = settings.baseUrl.trim().trimEnd('/') + "/chat/completions"
        var response = post(url, settings, payload(settings, system, user, maxTokens, thinkingOff = true))
        if (response.status.value == 400) {
            // 兼容网关不认识 thinking 字段时返回 400：去掉该字段重试一次（等效开启思考，靠大预算兜底）
            response = post(url, settings, payload(settings, system, user, maxTokens, thinkingOff = false))
        }
        when (response.status.value) {
            401, 403 -> throw LlmException(
                LlmException.Kind.AUTH,
                "翻译服务鉴权失败（HTTP ${response.status.value}）",
            )

            in 200..299 -> Unit

            else -> throw LlmException(
                LlmException.Kind.HTTP,
                "翻译服务返回 HTTP ${response.status.value}",
            )
        }
        val reply = readReply(response.bodyAsText())
        val content = reply.content
        if (content.isNullOrBlank()) {
            if (!requireContent) return ""
            throw LlmException(
                LlmException.Kind.EMPTY,
                "翻译服务返回空结果（finish_reason=${reply.finishReason ?: "缺失"}，" +
                    "推理型模型可能需要更大的输出预算）",
            )
        }
        return content.trim()
    }

    private fun payload(
        settings: LlmSettings,
        system: String,
        user: String,
        maxTokens: Int,
        thinkingOff: Boolean,
    ): JsonObject = buildJsonObject {
        put("model", settings.model)
        put(
            "messages",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", system)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", user)
                    },
                )
            },
        )
        put("temperature", 0.2)
        put("max_tokens", maxTokens)
        if (thinkingOff) {
            put("thinking", buildJsonObject { put("type", "disabled") })
        }
    }

    private suspend fun post(url: String, settings: LlmSettings, payload: JsonObject): HttpResponse =
        try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                setBody(payload.toString())
            }
        } catch (e: IOException) {
            throw LlmException(
                LlmException.Kind.NETWORK,
                "连接翻译服务失败：${e.message ?: e.javaClass.simpleName}",
                e,
            )
        }

    /** 取第一个 choice 的正文与结束原因；结构不符一律 [LlmException.Kind.FORMAT]。 */
    private fun readReply(body: String): Reply {
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw LlmException(LlmException.Kind.FORMAT, "翻译服务响应格式异常", e)
        }
        val choice = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
            ?: throw LlmException(LlmException.Kind.FORMAT, "翻译服务响应缺少 choices")
        val content = (choice["message"] as? JsonObject)
            ?.get("content")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
        val finishReason = (choice["finish_reason"] as? JsonPrimitive)?.contentOrNull
        return Reply(content, finishReason)
    }

    /** 解析行协议输出：每行 `编号|译名`，容忍空行与无关行。 */
    private fun parseTitleLines(content: String): List<Pair<Long, String>> =
        content.lineSequence().mapNotNull { line ->
            val match = TITLE_LINE.find(line.trim()) ?: return@mapNotNull null
            val id = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val title = match.groupValues[2].trim()
            if (title.isNotBlank()) id to title else null
        }.toList()

    private class Reply(val content: String?, val finishReason: String?)
}
