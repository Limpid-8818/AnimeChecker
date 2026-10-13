package com.animechecker.local.data.llm

import com.animechecker.local.domain.model.LlmSettings
import com.animechecker.local.domain.model.TranslateState
import kotlinx.coroutines.CancellationException

/** 队列里的一条待译条目。 */
internal data class QueuedSubject(
    val id: Long,
    val name: String,
    val nameCn: String?,
    val summary: String?,
    val summaryEn: String?,
    val needSummary: Boolean,
)

/**
 * 队列的数据访问端口。
 *
 * [pending] 表示"仍有可译内容且未被判失败"。
 */
internal interface TranslationQueueStore {

    suspend fun pending(): List<QueuedSubject>

    /** 重读一条，用于收尾判定。 */
    suspend fun fresh(id: Long): QueuedSubject?

    suspend fun saveNameCn(id: Long, nameCn: String)

    suspend fun saveSummary(id: Long, summary: String)

    suspend fun setState(id: Long, state: TranslateState)
}

/**
 * 翻译队列策略：一轮把待译条目译完，并逐条判定状态。
 *
 * - 标题批量（客户端按 10 条/请求切分），简介逐条且只译 needSummary 条目；
 * - 尝试过仍有缺失 → FAILED：可由设置页手动触发重试；
 * - 单条失败不影响同轮其余条目（失败被吞在队列内部），协程取消照旧向上传播。
 */
internal class TranslationQueue(
    private val store: TranslationQueueStore,
    private val translator: LlmTranslator,
) {

    suspend fun pump(settings: LlmSettings) {
        val pending = store.pending()
        if (pending.isEmpty()) return

        // 标题批：一次请求带 ≤10 条（客户端切分），成功的逐条进库
        val titleItems = pending.filter { it.nameCn == null }
        if (titleItems.isNotEmpty()) {
            attempt { translator.translateTitles(settings, titleItems.map { it.id to it.name }) }
                .getOrElse { emptyList() }
                .forEach { (id, nameCn) -> store.saveNameCn(id, nameCn) }
        }
        val titleAttempted = titleItems.mapTo(mutableSetOf()) { it.id }

        // 简介逐条：仅 needSummary 且原文在、译文缺的条目
        for ((id, _, _, summary, summaryEn, needSummary) in pending) {
            val original = summaryEn ?: continue
            if (!needSummary || summary != null) continue
            attempt { translator.translateText(settings, original) }
                .onSuccess { store.saveSummary(id, it) }
        }

        // 收尾：逐条对照本轮前后的状态，全部可译项完成 → DONE，
        // 尝试过但仍有缺失 → FAILED（等待手动重试），其余留在队列里
        for ((id, _, _, summary, summaryEn, needSummary) in pending) {
            val fresh = store.fresh(id) ?: continue
            val titleStillMissing = id in titleAttempted && fresh.nameCn == null
            val summaryAttempted = needSummary && summaryEn != null && summary == null
            val summaryStillMissing = summaryAttempted && fresh.summary == null
            val workLeft = fresh.nameCn == null ||
                (fresh.needSummary && fresh.summaryEn != null && fresh.summary == null)
            store.setState(
                id,
                when {
                    !workLeft -> TranslateState.DONE
                    titleStillMissing || summaryStillMissing -> TranslateState.FAILED
                    else -> TranslateState.PENDING
                },
            )
        }
    }

    /** 翻译失败不中断整轮，但协程取消必须上抛。 */
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
