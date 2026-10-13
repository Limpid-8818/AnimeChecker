package com.animechecker.local.data.repository

import com.animechecker.local.data.anilist.AniListException
import com.animechecker.local.data.llm.LlmException
import com.animechecker.local.data.anilist.AniListException.Kind as AniListKind

/**
 * 领域错误：repository 只给分类，用户可见文案由 UI 按 [Kind] 从 `ui.Strings` 取用。
 *
 * 数据源异常（[AniListException] 等）在 repository 边界处收敛成本类型，不外传。
 */
class AppError(
    val kind: Kind,
    cause: Throwable? = null,
) : Exception(kind.name, cause) {

    enum class Kind {
        /** 网络不可用或上游不可达。 */
        NETWORK,

        /** 上游限流，稍后可重试。 */
        RATE_LIMITED,

        /** 上游返回错误或拒绝了请求。 */
        UPSTREAM,

        /** 请求的数据不存在。 */
        NOT_FOUND,

        /** 翻译服务鉴权失败：API Key 无效或未授权。 */
        LLM_AUTH,

        /** 翻译服务响应结构不合法。 */
        LLM_FORMAT,

        /** 翻译服务其他失败：非 2xx、连接失败或正文为空。 */
        LLM_FAILED,

        /** 其他未归类失败。 */
        UNKNOWN,
    }
}

/** 数据源异常 → 领域错误；已收敛过的原样返回，其余归入 [AppError.Kind.UNKNOWN]。 */
internal fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is AniListException -> AppError(
        when (kind) {
            AniListKind.NETWORK -> AppError.Kind.NETWORK
            AniListKind.RATE_LIMITED -> AppError.Kind.RATE_LIMITED
            AniListKind.HTTP, AniListKind.GRAPHQL -> AppError.Kind.UPSTREAM
            AniListKind.NOT_FOUND -> AppError.Kind.NOT_FOUND
        },
        this,
    )

    is LlmException -> AppError(
        when (kind) {
            LlmException.Kind.AUTH -> AppError.Kind.LLM_AUTH
            LlmException.Kind.FORMAT -> AppError.Kind.LLM_FORMAT
            LlmException.Kind.HTTP, LlmException.Kind.EMPTY, LlmException.Kind.NETWORK -> AppError.Kind.LLM_FAILED
        },
        this,
    )

    else -> AppError(AppError.Kind.UNKNOWN, this)
}
