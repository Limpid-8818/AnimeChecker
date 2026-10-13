package com.animechecker.local.domain.model

/**
 * 翻译服务配置：用户自配置 OpenAI 兼容接口。
 *
 * [usable] 为 false 时翻译整体降级：标题显示罗马音、简介显示英文原文，应用功能不受影响。
 */
data class LlmSettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
) {
    /** 三项齐全才可调用；缺失时后台翻译为空操作，不报错。 */
    val usable: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}
