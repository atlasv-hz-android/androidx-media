package com.atlasv.android.mediax.effect.ext.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by weiping on 2024/12/26
 */
@Entity(tableName = "video_export_record")
data class VideoExportRecord(
    @PrimaryKey val taskId: String,
    val inputUri: String,
    val savedUri: String?,
    val createAt: Long,
    val needCredits: Int
)
