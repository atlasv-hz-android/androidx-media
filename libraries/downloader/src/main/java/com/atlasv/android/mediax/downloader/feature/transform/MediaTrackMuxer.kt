package com.atlasv.android.mediax.downloader.feature.transform

import java.io.File

/**
 *
 * 音视频合成
 *
 * Created by weiping on 2025/1/13
 */
interface MediaTrackMuxer {
    suspend fun mux(videoFile: File, audioFile: File, destFile: File)
}