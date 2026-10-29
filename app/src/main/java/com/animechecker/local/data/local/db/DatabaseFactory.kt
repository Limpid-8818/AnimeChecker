package com.animechecker.local.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDatabase 的唯一创建入口，由 [com.animechecker.local.di.AppContainer] 调用。
 *
 * - 库名固定为 [DB_NAME]，首次创建即当前 schema 版本。
 * - 结构变更仍应递增 AppDatabase 版本并补 Migration，避免误触发重建。
 */
object DatabaseFactory {

    const val DB_NAME = "animechecker.db"

    /** v2：subjects 增加 finished 完结标记。 */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE subjects ADD COLUMN finished INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun create(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DB_NAME,
    ).addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
