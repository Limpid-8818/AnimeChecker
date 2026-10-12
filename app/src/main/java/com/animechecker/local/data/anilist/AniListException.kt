package com.animechecker.local.data.anilist

/**
 * AniList 请求失败。
 *
 * [kind] 供上层分流处理（限流可稍后重试、断网可提示检查网络）；[message] 只面向日志与上报，
 * 用户可见文案集中在 ui.Strings，由 repository 映射，不从这里直出。
 */
internal class AniListException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    enum class Kind {
        /** 连接级失败：DNS、路由、TLS 抖动或请求超时，重试后仍失败。 */
        NETWORK,

        /** 429 限流，按 Retry-After 退避重试后仍失败。 */
        RATE_LIMITED,

        /** 非 2xx 且响应体里没有可用的 GraphQL 错误信息。 */
        HTTP,

        /** 服务端返回 GraphQL errors，或以此拒绝了请求（如查询语法错误）。 */
        GRAPHQL,

        /** 服务端未返回本次请求的数据。 */
        NOT_FOUND,
    }
}
