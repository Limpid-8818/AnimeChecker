package com.animechecker.local.data.anilist

/**
 * AniList GraphQL 查询：字段片段与查询全部在此处编写，client 不内联查询文本。
 *
 * 约定：
 * - `$` 在 Kotlin 字符串模板里要转义，统一用 [D] 拼接；
 * - 分页查询一律请求 `pageInfo { hasNextPage }`：分页只信该字段，服务端的 `total` 是 5000 上限值，不作判断依据；
 * - 请求的字段与 Dtos.kt 一一对应：DTO 里没有的字段不要请求。
 */
internal object Queries {

    private const val D = "$"

    /** 条目字段：不含简介。放送表用，调用处另补 `isAdult` 以过滤成人向条目。 */
    private const val MEDIA_CORE = """
        id episodes status averageScore
        coverImage { large }
        title { romaji english native }
        nextAiringEpisode { episode airingAt }
        startDate { year month day }
        season seasonYear
    """

    /** 条目字段：在 [MEDIA_CORE] 之上补简介原文（HTML 片段）。 */
    private const val MEDIA_FULL = "$MEDIA_CORE description"

    /** 放送表：窗口内按放送时间排序，由调用方按 7 天分段。 */
    val SCHEDULE = """
        query (${D}from: Int, ${D}to: Int, ${D}page: Int) {
          Page(page: ${D}page, perPage: 50) {
            airingSchedules(airingAt_greater: ${D}from, airingAt_lesser: ${D}to, sort: TIME) {
              airingAt
              media { $MEDIA_CORE isAdult }
            }
            pageInfo { hasNextPage }
          }
        }
    """.trimIndent()

    /** 关键词搜索：第一页 24 条，按相关度。 */
    val SEARCH = """
        query (${D}s: String) {
          Page(perPage: 24) {
            media(search: ${D}s, type: ANIME, isAdult: false, sort: SEARCH_MATCH) { $MEDIA_FULL }
          }
        }
    """.trimIndent()

    /** 季度目录：按人气排序，每页 50，翻页以 hasNextPage 为准。 */
    val SEASON = """
        query (${D}season: MediaSeason, ${D}year: Int, ${D}page: Int) {
          Page(page: ${D}page, perPage: 50) {
            media(season: ${D}season, seasonYear: ${D}year, type: ANIME, isAdult: false, sort: POPULARITY_DESC) { $MEDIA_FULL }
            pageInfo { hasNextPage }
          }
        }
    """.trimIndent()

    /** 单条详情。 */
    val DETAIL = """
        query (${D}id: Int) { Media(id: ${D}id) { $MEDIA_FULL } }
    """.trimIndent()

    /** 批量详情：id_in 单页 50，由调用方分批发。 */
    val DETAILS = """
        query (${D}ids: [Int]) {
          Page(perPage: 50) { media(id_in: ${D}ids, type: ANIME) { $MEDIA_FULL } }
        }
    """.trimIndent()
}
