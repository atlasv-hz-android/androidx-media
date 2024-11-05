package com.atlasv.android.media3.demo.download

import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.mediax.downloader.api.MediaXDownloadClient
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy
import com.atlasv.android.mediax.downloader.core.ContentLengthLoader
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.SimpleMediaXCacheSupplier
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import okhttp3.OkHttpClient
import java.io.File

/**
 * Created by weiping on 2024/11/5
 */
object DownloaderAgent : DownloadListener {
    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }
    private val contentLengthLoader by lazy {
        ContentLengthLoader(okHttpClient)
    }
    val client by lazy {
        MediaXDownloadClient(
            mediaXCacheSupplier = SimpleMediaXCacheSupplier(
                appContext = App.app,
                okhttpClient = okHttpClient,
                cacheDirSupplier = {
                    File(appContext.getExternalFilesDir(null), "download-cache").also {
                        it.mkdirs()
                    }
                },
            ),
            downloadListener = this,
            listener = null,
            contentLengthLoader = contentLengthLoader,
            rangeCountStrategy = SimpleRangeStrategy(4)
        )
    }

    override fun onProgress(
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long,
        downloadUrl: String,
        id: String
    ) {
        mediaXLogger?.d { "onProgress: $bytesCached/${requestLength}(${(bytesCached * 100f / requestLength).toInt()})($downloadUrl)" }
    }
}