package com.atlasv.android.mediax.effect.ext.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Created by weiping on 2024/12/26
 */
@Dao
interface VideoExportRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: VideoExportRecord)

    @Query("SELECT * FROM video_export_record ORDER BY createAt DESC")
    fun getAllAsFlow(): Flow<List<VideoExportRecord>>

    @Query("SELECT * FROM video_export_record ORDER BY createAt DESC LIMIT 1")
    fun getNewestRecord(): VideoExportRecord?

    @Query("DELETE FROM video_export_record WHERE taskId=:taskId")
    fun deleteByTaskId(taskId: String)
}