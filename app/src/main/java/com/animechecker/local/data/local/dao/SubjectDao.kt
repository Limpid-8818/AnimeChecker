package com.animechecker.local.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.animechecker.local.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Upsert
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Upsert
    suspend fun upsert(subject: SubjectEntity)

    @Query("SELECT * FROM subjects")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeById(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun byId(id: Long): SubjectEntity?

    @Query(
        "SELECT * FROM subjects WHERE nameCn LIKE '%' || :kw || '%' " +
            "OR name LIKE '%' || :kw || '%'"
    )
    suspend fun search(kw: String): List<SubjectEntity>

    // 翻译队列

    @Query(
        "SELECT * FROM subjects WHERE translateState = 0 " +
            "AND (nameCn IS NULL OR (needSummary = 1 AND summaryEn IS NOT NULL AND summary IS NULL))"
    )
    suspend fun translationPending(): List<SubjectEntity>

    @Query(
        "SELECT COUNT(*) FROM subjects WHERE translateState = 0 " +
            "AND (nameCn IS NULL OR (needSummary = 1 AND summaryEn IS NOT NULL AND summary IS NULL))"
    )
    fun translationPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM subjects WHERE translateState = 2")
    fun translationFailedCount(): Flow<Int>

    /**
     * 简介入翻译队列：置 needSummary 并复位翻译状态（只译标题的条目此前已是 DONE，
     * 不复位就永远不会被 translationPending 取到）；FAILED 保持不动，等设置页手动重试。
     */
    @Query("UPDATE subjects SET needSummary = 1, translateState = 0 WHERE id = :id AND translateState != 2")
    suspend fun markNeedSummary(id: Long)

    /** 批量入队：追番对象刷新按批写入，避免逐条 UPDATE。 */
    @Query("UPDATE subjects SET needSummary = 1, translateState = 0 WHERE id IN (:ids) AND translateState != 2")
    suspend fun markNeedSummaryAll(ids: List<Long>)

    @Query("UPDATE subjects SET nameCn = :nameCn WHERE id = :id")
    suspend fun setNameCn(id: Long, nameCn: String)

    @Query("UPDATE subjects SET summary = :summary WHERE id = :id")
    suspend fun setSummary(id: Long, summary: String)

    @Query("UPDATE subjects SET translateState = :state WHERE id = :id")
    suspend fun setTranslateState(id: Long, state: Int)

    @Query("UPDATE subjects SET translateState = 0 WHERE translateState = 2")
    suspend fun resetFailedTranslations(): Int

    // 放送日历标记

    @Query("UPDATE subjects SET inCalendar = 1 WHERE id IN (:ids)")
    suspend fun markInCalendar(ids: List<Long>)

    @Query("UPDATE subjects SET inCalendar = 0 WHERE inCalendar = 1 AND id NOT IN (:keep)")
    suspend fun clearInCalendarExcept(keep: List<Long>)

    @Query("UPDATE subjects SET inCalendar = 0 WHERE inCalendar = 1")
    suspend fun clearAllInCalendar()
}
