package com.atlasv.android.media3.demo.download

import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.mediax.downloader.core.ContentLengthLoader
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCacheSupplier
import com.atlasv.android.mediax.downloader.core.MediaXDownloaderCore
import com.atlasv.android.mediax.downloader.core.SimpleMediaXCacheSupplier
import com.atlasv.android.mediax.downloader.datasource.ConfigurableUpstreamStrategy
import com.atlasv.android.mediax.downloader.datasource.OkhttpUpstreamStrategy
import com.atlasv.android.mediax.downloader.datasource.UpstreamStrategy
import com.atlasv.android.mediax.downloader.model.SpecProgressInfo
import com.atlasv.android.mediax.downloader.output.OutputTarget
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.io.File

/**
 * Created by weiping on 2024/11/5
 */
@UnstableApi
object DownloaderAgent : DownloadListener {
    val progressMap = MutableStateFlow<Map<String, ProgressItem>>(emptyMap())
    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }
    val contentLengthLoader by lazy {
        ContentLengthLoader(okHttpClient)
    }
    private val databaseProvider by lazy {
        // Note: This should be a singleton in your app.
        StandaloneDatabaseProvider(App.app)
    }
    private val mediaXCacheSupplier by lazy {
        createCacheSupplier()
    }

    val downloadCore by lazy {
        MediaXDownloaderCore(
            mediaXCacheSupplier, contentLengthLoader
        )
    }

    private fun createCacheSupplier(): MediaXCacheSupplier {
        return SimpleMediaXCacheSupplier(
            upstreamStrategy = getUpstreamDataSourceStrategy(),
            cacheDirSupplier = {
                File(appContext.getExternalFilesDir(null), "mediax-download-cache")
            },
            databaseProvider = databaseProvider
        )
    }

    private fun getUpstreamDataSourceStrategy(): UpstreamStrategy {
        return ConfigurableUpstreamStrategy(
            okhttpStrategy = OkhttpUpstreamStrategy(
                appContext,
                okHttpClient
            )
        )
    }

    override fun onProgress(
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long,
        speedPerSeconds: Long,
        downloadUrl: String,
        taskId: String,
        specProgressInfoMap: Map<Int, SpecProgressInfo>
    ) {
        progressMap.update { map ->
            map + (taskId to ProgressItem(
                taskId = taskId,
                downloadUrl = downloadUrl,
                requestLength = requestLength,
                bytesCached = bytesCached,
                speedPerSeconds = speedPerSeconds,
                specs = specProgressInfoMap.values.toList().sortedBy { it.specIndex }
            ))
        }
    }

    override fun onDownloadSpeed(
        taskId: String,
        downloadUrl: String,
        bytesPerSecond: Long,
        rangeCount: Int
    ) {
        mediaXLogger?.d { "onDownloadSpeed: speed=${bytesPerSecond / 1024}, rangeCount=$rangeCount($downloadUrl)" }
    }

    override fun onDownloadStart(taskId: String, downloadUrl: String) {
        mediaXLogger?.d { "onDownloadStart: ($downloadUrl)" }
    }

    override fun onDownloadSuccess(taskId: String, downloadUrl: String, rangeCount: Int) {
        mediaXLogger?.d { "onDownloadSuccess: rangeCount=$rangeCount($downloadUrl)" }
    }

    override fun onSaveSuccess(
        taskId: String,
        downloadUrl: String,
        fileSize: Long,
        outputTarget: OutputTarget
    ) {
        mediaXLogger?.d { "onSaveSuccess: fileSize=$fileSize($downloadUrl)($outputTarget)" }
    }

    override fun onDownloadFailed(taskId: String, downloadUrl: String, cause: Throwable) {
        mediaXLogger?.e(cause) { "onDownloadFailed($downloadUrl)" }
    }
}