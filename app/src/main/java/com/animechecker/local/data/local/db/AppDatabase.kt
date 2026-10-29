package com.animechecker.local.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.animechecker.local.data.local.dao.SubjectDao
import com.animechecker.local.data.local.dao.TrackDao
import com.animechecker.local.data.local.entity.SubjectEntity
import com.animechecker.local.data.local.entity.TrackEntity

/**
 * 本地数据库 Room。
 * schema 导出到 app/schemas 随库进版本控制；结构变更递增版本并写 Migration。
 * 实例创建见 [DatabaseFactory]，业务层只经 dao 访问。
 */
@Database(
    entities = [SubjectEntity::class, TrackEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun trackDao(): TrackDao
}
