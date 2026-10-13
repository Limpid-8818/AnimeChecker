package com.animechecker.local.data.repository

import com.animechecker.local.data.llm.QueuedSubject
import com.animechecker.local.data.llm.TranslationQueueStore
import com.animechecker.local.data.local.dao.SubjectDao
import com.animechecker.local.data.local.entity.SubjectEntity
import com.animechecker.local.domain.model.TranslateState

/**
 * 翻译队列的 Room 实现：把队列需要的五个动作翻译成 DAO 调用。
 */
internal class RoomTranslationQueueStore(private val subjectDao: SubjectDao) : TranslationQueueStore {

    override suspend fun pending(): List<QueuedSubject> =
        subjectDao.translationPending().map { it.toQueued() }

    override suspend fun fresh(id: Long): QueuedSubject? = subjectDao.byId(id)?.toQueued()

    override suspend fun saveNameCn(id: Long, nameCn: String) = subjectDao.setNameCn(id, nameCn)

    override suspend fun saveSummary(id: Long, summary: String) = subjectDao.setSummary(id, summary)

    override suspend fun setState(id: Long, state: TranslateState) =
        subjectDao.setTranslateState(id, state.toCode())

    private fun SubjectEntity.toQueued() = QueuedSubject(
        id = id,
        name = name,
        nameCn = nameCn,
        summary = summary,
        summaryEn = summaryEn,
        needSummary = needSummary,
    )

    /** 存储编码与 [SubjectEntity] 的三个常量一一对应。 */
    private fun TranslateState.toCode(): Int = when (this) {
        TranslateState.PENDING -> SubjectEntity.TRANSLATE_PENDING
        TranslateState.DONE -> SubjectEntity.TRANSLATE_DONE
        TranslateState.FAILED -> SubjectEntity.TRANSLATE_FAILED
    }
}
