package com.atlasv.android.media3.demo.download

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.atlasv.android.mediax.downloader.api.request.DownloadRequest
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

/**
 * Created by weiping on 2024/8/23
 */
@OptIn(UnstableApi::class)
class MainViewModel : ViewModel() {
    fun testDownload(downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DownloaderAgent.contentLengthLoader.fetch(ContentRequestStringModel(uriString = downloadUrl))
            testRangeCount(downloadUrl, 3, 1)
        }
    }

    private suspend fun testRangeCount(downloadUrl: String, rangeCount: Int, testCount: Int) {
        val avgTime = (1..testCount).map {
            measureTimeMillis {
                performDownload(downloadUrl, SimpleRangeStrategy(rangeCount))
            }.also {
                mediaXLogger?.d { "rangeCount: $rangeCount, cost time: $it millis" }
            }
        }.let {
            val filteredList = if (it.size >= 5) {
                it.sorted().subList(1, it.size - 1)
            } else {
                it
            }
            mediaXLogger?.d { "Calc list: $it -> $filteredList" }
            filteredList
        }.average().roundToInt()
        mediaXLogger?.d { "rangeCount: $rangeCount, avg time: $avgTime millis" }
    }

    private suspend fun performDownload(downloadUrl: String, rangeStrategy: SimpleRangeStrategy) {
        DownloaderAgent.client.fetch(
            DownloadRequest(
                url = downloadUrl,
                id = downloadUrl,
                destFile = File(
                    appContext.getExternalFilesDir(null),
                    "download-files/${Uri.parse(downloadUrl).lastPathSegment}"
                ),
                rangeCountStrategy = rangeStrategy
            )
        )
    }
}