package com.atlasv.android.mediax.downloader.feature

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import com.atlasv.android.logger.ILogger
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy.Companion.SingleRangeStrategy
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXDownloaderCore
import com.atlasv.android.mediax.downloader.feature.transform.MediaTrackMuxer
import com.atlasv.android.mediax.downloader.output.DownloadResult
import com.atlasv.android.mediax.downloader.output.FileOutputTarget
import com.atlasv.android.mediax.downloader.output.asOutputTarget
import com.atlasv.android.mediax.downloader.output.asUuidFileName
import java.io.File

/**
 * Created by weiping on 2025/1/13
 */
class MediaXDownloaderClient(
    private val appContext: Context,
    private val core: MediaXDownloaderCore,
    private val mediaTrackMuxer: MediaTrackMuxer? = null,
    private val logger: ILogger? = null
) {
    suspend fun download(
        request: DownloadRequest,
        outputTarget: FileOutputTarget,
        downloadListener: DownloadListener?,
        rangeCountStrategy: RangeCountStrategy? = null,
        throwException: Boolean = false
    ): DownloadResult? {
        val downloadResult: DownloadResult = core.download(
            downloadUrl = request.downloadUrl,
            taskId = request.taskId,
            outputTarget = outputTarget,
            rangeCountStrategy = rangeCountStrategy,
            downloadListener = downloadListener,
            throwException = throwException
        ) ?: return null
        if (!MimeTypes.isVideo(request.mediaType.toString())) {
            return downloadResult
        }
        try {
            val outputFile = outputTarget.targetFile
            val metaData: TrackGroupArray = MetadataRetriever.retrieveMetadata(
                appContext,
                MediaItem.fromUri(Uri.fromFile(outputFile))
            ).get()
            val containsAudio = metaData.containsAudioTrack()
            logger?.d { "Check media tracks: (${metaData.trackTypes}), containsAudio=$containsAudio, outputFile=$outputFile(${outputFile.length()})" }
            if (containsAudio) {
                return downloadResult
            }
            if (!request.attachAudioUrl.isNullOrEmpty() && mediaTrackMuxer != null) {
                logger?.d { "No audio track found in $outputFile, will download audio from ${request.attachAudioUrl}" }
                val destAudioFile = prepareAudioFile(request, outputTarget.targetFile.parentFile)
                core.download(
                    downloadUrl = request.attachAudioUrl,
                    taskId = request.taskId + "-audio",
                    outputTarget = destAudioFile.asOutputTarget(),
                    rangeCountStrategy = SingleRangeStrategy,
                    downloadListener = null,
                    throwException = true
                )
                logger?.d { "Audio file downloaded to $destAudioFile(${destAudioFile.length()}), start mux..." }
                mediaTrackMuxer.mux(
                    videoFile = outputFile,
                    audioFile = destAudioFile,
                    destFile = outputFile
                )
                logger?.d { "Mux successfully to ${outputFile}(${outputFile.length()})" }
            }
            return downloadResult
        } catch (cause: Throwable) {
            logger?.e(cause) { "Check media track " }
            return downloadResult
        }
    }

    private fun prepareAudioFile(request: DownloadRequest, dir: File?): File {
        val destAudioFileName =
            Uri.parse(request.attachAudioUrl)?.lastPathSegment.asUuidFileName(request.mediaType)
        val destAudioFile =
            dir?.let { File(dir, destAudioFileName) }
                ?: error("Can not create audio file: $request, dir=$dir")
        return destAudioFile
    }

    fun cancel(id: String, alsoDelete: Boolean = false) {
        core.cancel(id, alsoDelete)
    }
}

private fun TrackGroupArray.containsAudioTrack(): Boolean {
    return this.trackTypes.any {
        it == C.TRACK_TYPE_AUDIO
    }
}