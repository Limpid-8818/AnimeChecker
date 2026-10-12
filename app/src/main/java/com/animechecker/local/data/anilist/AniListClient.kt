package com.animechecker.local.data.anilist

import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AniList（GraphQL）客户端：放送表、搜索、季度目录与详情的唯一出口。
 *
 * 约定：
 * - 对外只给领域模型（[Subject]），DTO、查询文档与传输细节不向外暴露；
 * - 直连，不经代理；封面直载 AniList CDN；
 * - 服务端限流 30 次/分钟：节流与 429 退避在 [AniListTransport]；
 * - 分页只信 `hasNextPage`；`Page.pageInfo.total` 是 5000 上限值，不作为依据。
 */
class AniListClient internal constructor(private val transport: AniListTransport) {

    private companion object {
        /** 放送表窗口：过去 7 天起，7 天一段共 3 段（过去 7 天 + 未来 14 天）。 */
        const val WINDOW_DAYS = 7
        const val WINDOW_COUNT = 3

        /** 单窗口翻页上限：正常每窗口 1 页，翻页只是兜底。 */
        const val MAX_PAGES_PER_WINDOW = 3

        /** 季度目录页数上限（每页 50，见 [Queries.SEASON]）。 */
        const val SEASON_MAX_PAGES = 2

        /** id_in 单批上限，与 [Queries.DETAILS] 的 perPage 一致。 */
        const val DETAILS_CHUNK = 50

        const val SECONDS_PER_DAY = 86_400L
    }

    /**
     * 放送表：过去 7 天到未来 14 天。
     * 同一条目以更晚的放送时间为准（跨周条目取最近一次），成人向条目丢弃。
     */
    suspend fun schedule(): List<Subject> {
        val windowSeconds = WINDOW_DAYS * SECONDS_PER_DAY
        val byId = LinkedHashMap<Long, Subject>()
        var cursor = System.currentTimeMillis() / 1000 - windowSeconds
        repeat(WINDOW_COUNT) {
            var page = 1
            while (page <= MAX_PAGES_PER_WINDOW) {
                val data = pageOf(
                    Queries.SCHEDULE,
                    buildJsonObject {
                        put("from", cursor)
                        put("to", cursor + windowSeconds)
                        put("page", page)
                    },
                ) ?: break
                data.airingSchedules.forEach { row ->
                    val subject = row.toSubjectOrNull() ?: return@forEach
                    byId[subject.id] = subject
                }
                if (data.pageInfo?.hasNextPage != true) break
                page++
            }
            cursor += windowSeconds
        }
        return byId.values.toList()
    }

    /** 关键词搜索：第一页 24 条，按相关度。 */
    suspend fun search(keyword: String): List<Subject> {
        val data = pageOf(Queries.SEARCH, buildJsonObject { put("s", keyword) }) ?: return emptyList()
        return data.media.map { it.toSubject() }
    }

    /** 季度目录：按人气取前两页，以 `hasNextPage` 判停。 */
    suspend fun season(season: Season): List<Subject> {
        val byId = LinkedHashMap<Long, Subject>()
        var page = 1
        while (page <= SEASON_MAX_PAGES) {
            val data = pageOf(
                Queries.SEASON,
                buildJsonObject {
                    put("season", season.toAniListSeason())
                    put("year", season.year)
                    put("page", page)
                },
            ) ?: break
            data.media.forEach { media -> byId[media.id] = media.toSubject() }
            if (data.pageInfo?.hasNextPage != true) break
            page++
        }
        return byId.values.toList()
    }

    /** 单条详情：服务端未返回该条目时抛 [AniListException.Kind.NOT_FOUND]。 */
    suspend fun detail(id: Long): Subject {
        val data = transport.post(
            Queries.DETAIL,
            buildJsonObject { put("id", id) },
            MediaData.serializer(),
        )
        val media = data.media ?: throw AniListException(
            AniListException.Kind.NOT_FOUND,
            "AniList 未返回条目 $id",
        )
        return media.toSubject()
    }

    /** 批量详情：`id_in` 分批拉取；空入参不发请求（首页空清单零网络）。 */
    suspend fun details(ids: List<Long>): List<Subject> {
        if (ids.isEmpty()) return emptyList()
        val out = mutableListOf<Subject>()
        ids.chunked(DETAILS_CHUNK).forEach { chunk ->
            val data = pageOf(
                Queries.DETAILS,
                buildJsonObject { put("ids", JsonArray(chunk.map { JsonPrimitive(it) })) },
            ) ?: return@forEach
            data.media.forEach { out += it.toSubject() }
        }
        return out
    }

    private suspend fun pageOf(query: String, variables: JsonObject): PageDto? =
        transport.post(query, variables, PageData.serializer()).page
}
