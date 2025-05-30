package com.android.now.mediax.effect.ext.db.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.now.appcontext.AppContextHolder.Companion.appContext

/**
 *
 * 视频导出数据库
 *
 * Created by weiping on 2024/12/26
 */
@Database(
    version = 1,
    entities = [VideoExportRecord::class]
)
abstract class VideoExportDb : RoomDatabase() {
    abstract fun recordDao(): VideoExportRecordDao

    companion object {
        private const val DATABASE_NAME = "video_export"
        private val instance by lazy {
            buildDatabase(appContext)
        }

        private fun buildDatabase(context: Context): VideoExportDb {
            return Room
                .databaseBuilder(context, VideoExportDb::class.java, DATABASE_NAME)
                .allowMainThreadQueries().build()
        }

        val recordDao get() = instance.recordDao()
    }
}