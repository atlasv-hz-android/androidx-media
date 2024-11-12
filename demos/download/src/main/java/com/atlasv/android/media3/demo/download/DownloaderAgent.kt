package com.atlasv.android.media3.demo.download

import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.mediax.downloader.analytics.DownloadPerfTracker
import com.atlasv.android.mediax.downloader.core.ContentLengthLoader
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCacheSupplier
import com.atlasv.android.mediax.downloader.core.MediaXDownloaderCore
import com.atlasv.android.mediax.downloader.core.SimpleMediaXCacheSupplier
import com.atlasv.android.mediax.downloader.datasource.ConfigurableUpstreamStrategy
import com.atlasv.android.mediax.downloader.datasource.OkhttpUpstreamStrategy
import com.atlasv.android.mediax.downloader.datasource.UpstreamStrategy
import com.atlasv.android.mediax.downloader.model.SpecProgressInfo
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
            mediaXCacheSupplier,
            contentLengthLoader,
            perfTracker = object : DownloadPerfTracker {
                override fun trackDownloadSpeed(bytesPerSecond: Long) {
                    mediaXLogger?.d { "PerfTrack: speed=${bytesPerSecond / 1024}" }
                }

                override fun trackDownloadStart() {
                    mediaXLogger?.d { "PerfTrack: trackDownloadStart" }
                }

                override fun trackDownloadSuccess(fileSize: Long) {
                    mediaXLogger?.d { "PerfTrack: trackDownloadSuccess: $fileSize" }
                }

                override fun trackSaveSuccess() {
                    mediaXLogger?.d { "PerfTrack: trackSaveSuccess" }
                }

                override fun trackDownloadFailed(cause: Throwable) {
                    mediaXLogger?.e(cause) { "PerfTrack: trackDownloadFailed" }
                }
            })
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
        id: String,
        specProgressInfoMap: Map<Int, SpecProgressInfo>
    ) {
        progressMap.update { map ->
            map + (downloadUrl to ProgressItem(
                downloadUrl = downloadUrl,
                requestLength = requestLength,
                bytesCached = bytesCached,
                speedPerSeconds = speedPerSeconds,
                specs = specProgressInfoMap.values.toList().sortedBy { it.specIndex }
            ))
        }
    }
}