package com.atlasv.android.media3.demo.download

import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.mediax.downloader.api.MediaXDownloadClient
import com.atlasv.android.mediax.downloader.core.ContentLengthLoader
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCacheSupplier
import com.atlasv.android.mediax.downloader.core.SimpleMediaXCacheSupplier
import com.atlasv.android.mediax.downloader.datasource.ConfigurableUpstreamStrategy
import com.atlasv.android.mediax.downloader.datasource.OkhttpUpstreamStrategy
import com.atlasv.android.mediax.downloader.datasource.UpstreamStrategy
import okhttp3.OkHttpClient
import java.io.File

/**
 * Created by weiping on 2024/11/5
 */
@UnstableApi
object DownloaderAgent : DownloadListener {
    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }
    private val contentLengthLoader by lazy {
        ContentLengthLoader(okHttpClient)
    }
    private val databaseProvider by lazy {
        // Note: This should be a singleton in your app.
        StandaloneDatabaseProvider(App.app)
    }
    val client by lazy {
        MediaXDownloadClient(
            mediaXCacheSupplier = getCacheSupplier(),
            downloadListener = this,
            listener = null,
            contentLengthLoader = contentLengthLoader,
        )
    }

    private fun getCacheSupplier(): MediaXCacheSupplier {
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
        downloadUrl: String,
        id: String
    ) {
    }
}