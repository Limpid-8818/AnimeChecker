package com.animechecker.local.data.local.db

import android.content.Context
import androidx.room.Room

/**
 * AppDatabase 的唯一创建入口，由 [com.animechecker.local.di.AppContainer] 调用。
 *
 * - 库名固定为 [DB_NAME]，首次创建即当前 schema 版本。
 * - 结构变更仍应递增 AppDatabase 版本并补 Migration，避免误触发重建。
 */
object DatabaseFactory {

    const val DB_NAME = "animechecker.db"

    fun create(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DB_NAME,
    ).fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
