package com.animechecker.local.ui

import com.animechecker.local.data.repository.AppError
import com.animechecker.local.domain.model.Quarter
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.TrackStatus

/**
 * 应用内全部用户可见文案的唯一来源。
 *
 * 工具风格中性短句，不口语化；按钮为动词短语，状态为名词短语，错误为现象加建议动作；composable 内不得内联面向用户的字符串。
 */
object Strings {
    const val APP_NAME = "AnimeChecker"

    // 页面标题（各页统一左对齐 TopAppBar）
    const val TITLE_LIST = "追番清单"
    const val TITLE_CALENDAR = "放送表"
    const val TITLE_EXPLORE = "番库"
    const val TITLE_SETTINGS = "设置"
    const val TAB_LIST = "追番"
    const val TAB_CALENDAR = "放送"
    const val TAB_EXPLORE = "番库"

    // 追番清单
    const val FILTER_ALL = "全部"
    const val STATUS_DOING = "在看"
    const val STATUS_WISH = "想看"
    const val STATUS_DONE = "看完"
    const val STATUS_HOLD = "搁置"
    const val STATUS_DROPPED = "抛弃"
    const val BTN_PLUS_ONE = "记录一集"
    const val BTN_MINUS_ONE = "撤销一集"
    const val BTN_CATCHUP = "同步"
    const val EMPTY_LIST_TITLE = "清单为空"
    const val EMPTY_LIST_HINT = "从番库中添加番剧"
    const val EMPTY_LIST_ACTION = "前往番库"
    const val EMPTY_FILTER_TITLE = "该状态下暂无番剧"
    const val REMOVE_TRACK = "移出清单"
    const val REMOVE_CONFIRM_TITLE = "移出清单"
    const val REMOVE_CONFIRM_TEXT = "将《%s》移出清单？观看进度与备注将同时删除。"
    const val CONFIRM_REMOVE = "移出"
    const val CANCEL = "取消"
    const val AIRED_COUNT = "已更新 %1\$d 话"
    const val NOT_AIRED = "未开播"
    const val NO_AIR_WEEKDAY = "放送日期未知"
    const val LIST_STATS_FMT = "共 %1\$d 部 · 累计观看 %2\$d 话"
    const val UNDO_HINT = "已减一集"
    const val BACK = "返回"

    // 进度编辑（集数勾选弹层）
    const val SHEET_PICK_HINT = "点选切换单集观看状态；仅已放送的集数可选"
    const val CAUGHT_UP = "已追上放送进度"
    const val CATCHUP_UNAVAILABLE = "放送集数未知，无法追平"
    const val SAVE = "保存"
    const val CLEAR = "清除"

    // 放送表
    const val CALENDAR_NOTE = "根据公开消息整理得到的放送日程，以实际播出情况为准"
    const val EMPTY_CALENDAR_TITLE = "暂无放送数据"
    const val EMPTY_CALENDAR_HINT = "拉取 AniList 放送数据后可用"
    const val EMPTY_DAY_TITLE = "当日暂无放送记录"
    const val EMPTY_DAY_HINT = "数据未拉取时可能缺失，可先刷新"
    const val SYNC_NOW = "立即刷新"
    const val SYNCING = "刷新中…"

    // 番库
    const val SEARCH_HINT = "搜索番剧名称"
    const val SEARCH_TOO_SHORT = "关键词至少两个字符"
    const val SEARCH_NO_RESULT = "无匹配结果"
    const val SEARCH_OFFLINE_HINT = "AniList 不可达，已在本地缓存中搜索"
    const val EXPLORE_EMPTY = "该季度暂无数据"

    // 详情
    const val ADD_TO_LIST = "加入追番"
    const val ALREADY_TRACKED = "已在清单"
    const val MY_PROGRESS = "我的进度"
    const val AIRED_EPISODES = "已放送 %1\$d 话"
    const val AIRED_EPISODES_UNKNOWN = "放送集数未知"
    const val TOTAL_EPS = "总集数 %1\$s"
    const val TOTAL_EPS_UNKNOWN = "总集数未知"
    const val FIRST_AIR = "首播 %1\$s"
    const val FIRST_AIR_UNKNOWN = "首播时间未知"
    const val SCORE_UNRATED = "暂无评分"
    const val SUMMARY = "简介"
    const val SUMMARY_EMPTY = "暂无简介"
    const val SUMMARY_ORIGINAL_NOTE = "简介原文（未翻译）"
    const val SUMMARY_VIEW_ORIGINAL = "查看原文"
    const val SUMMARY_VIEW_TRANSLATED = "查看译文"
    const val EXPAND = "展开"
    const val COLLAPSE = "收起"
    const val CHANGE_STATUS = "状态"

    // 我的评分与备注
    const val MY_RATING = "我的评分"
    const val UNRATED = "未评分"
    const val MY_COMMENT = "备注"
    const val COMMENT_PLACEHOLDER = "添加备注"
    const val CLEAR_RATING = "清除评分"

    // 设置：翻译服务
    const val LLM_SECTION = "翻译服务"
    const val LLM_BASE_URL_LABEL = "接口地址"
    const val LLM_BASE_URL_SUPPORT = "OpenAI 兼容接口，形如 https://api.deepseek.com/v1"
    const val LLM_API_KEY_LABEL = "API Key"
    const val LLM_MODEL_LABEL = "模型名称"
    const val LLM_SAVE = "保存配置"
    const val LLM_TEST = "测试连接"
    const val LLM_TESTING = "测试中…"
    const val LLM_TEST_OK = "连接正常"
    const val LLM_TEST_EMPTY = "请先填写接口地址、API Key 与模型名称"
    const val LLM_UNCONFIGURED_HINT = "未配置时应用可正常使用：标题显示罗马音，简介显示英文原文"
    const val TRANSLATE_STATUS_FMT = "待翻译 %1\$d 项 · 失败 %2\$d 项"
    const val TRANSLATE_RETRY = "重试失败项"

    // 设置：数据
    const val DATA_SECTION = "数据"
    const val LAST_REFRESH_FMT = "上次刷新：%1\$s"
    const val LAST_REFRESH_NEVER = "从未刷新"
    const val DATA_CREDIT = "番剧数据来自 AniList"
    const val STORAGE_NOTE = "卸载应用将清除全部本地数据"

    // 设置：关于
    const val ABOUT_SECTION = "关于"
    const val VERSION_FMT = "版本 %1\$s"
    const val ABOUT_NETWORK_NOTE = "网络仅用于拉取 AniList 数据与调用翻译服务"

    // 通用
    const val RETRY = "重试"
    const val GO_SETTINGS = "前往设置"
    const val OFFLINE_BANNER = "离线模式：显示本地缓存"

    // 空态表情（颜文字）：按语义取用，不内联到组件
    const val FACE_INVITE = "(・ω・)"
    const val FACE_NEUTRAL = "(・_・)"
    const val FACE_WAITING = "×﹏×"
    const val FACE_NO_RESULT = "(?_?)"

    // 错误文案：按领域错误分类取用，UI 不自行拼句子
    const val ERROR_NETWORK = "网络不可用或 AniList 暂不可达，请稍后重试"
    const val ERROR_RATE_LIMITED = "请求过于频繁，请稍后重试"
    const val ERROR_UPSTREAM = "AniList 返回异常，请稍后重试"
    const val ERROR_NOT_FOUND = "未找到该条目"
    const val ERROR_UNKNOWN = "操作失败，请重试"
    const val ERROR_LLM_AUTH = "翻译服务鉴权失败，请检查 API Key"
    const val ERROR_LLM_FORMAT = "翻译服务响应格式异常"
    const val ERROR_LLM_FAILED = "翻译服务请求失败，请稍后重试"

    /** 状态名：领域状态 → 显示文案。 */
    fun statusName(status: TrackStatus): String = when (status) {
        TrackStatus.WISH -> STATUS_WISH
        TrackStatus.DOING -> STATUS_DOING
        TrackStatus.DONE -> STATUS_DONE
        TrackStatus.ON_HOLD -> STATUS_HOLD
        TrackStatus.DROPPED -> STATUS_DROPPED
    }

    /** 放送星期名，1 = 周一；缺失时给占位。 */
    fun weekdayName(weekday: Int): String = when (weekday) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> "—"
    }

    /** 季度标题。 */
    fun seasonTitle(season: Season): String = when (season.quarter) {
        Quarter.WINTER -> "${season.year} 年 1 月番"
        Quarter.SPRING -> "${season.year} 年 4 月番"
        Quarter.SUMMER -> "${season.year} 年 7 月番"
        Quarter.FALL -> "${season.year} 年 10 月番"
    }

    /** 错误分类 → 用户文案。 */
    fun errorText(kind: AppError.Kind): String = when (kind) {
        AppError.Kind.NETWORK -> ERROR_NETWORK
        AppError.Kind.RATE_LIMITED -> ERROR_RATE_LIMITED
        AppError.Kind.UPSTREAM -> ERROR_UPSTREAM
        AppError.Kind.NOT_FOUND -> ERROR_NOT_FOUND
        AppError.Kind.LLM_AUTH -> ERROR_LLM_AUTH
        AppError.Kind.LLM_FORMAT -> ERROR_LLM_FORMAT
        AppError.Kind.LLM_FAILED -> ERROR_LLM_FAILED
        AppError.Kind.UNKNOWN -> ERROR_UNKNOWN
    }
}
