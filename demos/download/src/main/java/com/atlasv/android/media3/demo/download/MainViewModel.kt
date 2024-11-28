package com.atlasv.android.media3.demo.download

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy
import com.atlasv.android.mediax.downloader.output.DownloadResult
import com.atlasv.android.mediax.downloader.output.FileOutputTarget
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

/**
 * Created by weiping on 2024/8/23
 */
@OptIn(UnstableApi::class)
class MainViewModel : ViewModel() {
    private val downloadResultMap = MutableStateFlow<Map<String, DownloadResult>>(emptyMap())
    val downloadItems =
        combine(downloadResultMap, DownloaderAgent.progressMap) { downloadResultMap, progressMap ->
            val progressItems = progressMap.map { it.value }
            progressItems.map {
                it to downloadResultMap[it.downloadUrl]
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun testDownload(downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                DownloaderAgent.contentLengthLoader.fetch(ContentRequestStringModel(uriString = downloadUrl))
                testRangeCount(downloadUrl, 3, 1)
            }.getOrElse {
                mediaXLogger?.e(it) { "testDownload failed" }
            }
        }
    }

    private suspend fun testRangeCount(downloadUrl: String, rangeCount: Int, testCount: Int) {
        val avgTime = (1..testCount).map {
            measureTimeMillis {
                performDownload(downloadUrl, SimpleRangeStrategy(rangeCount))
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
    }

    private suspend fun performDownload(downloadUrl: String, rangeStrategy: SimpleRangeStrategy) {
        DownloaderAgent.downloadCore.download(
            downloadUrl = downloadUrl,
            id = downloadUrl,
            outputTarget = FileOutputTarget(
                targetFileSupplier = {
                    File(
                        appContext.getExternalFilesDir(null),
                        "download-files/${Uri.parse(downloadUrl).lastPathSegment}"
                    )
                }
            ),
            rangeCountStrategy = rangeStrategy,
            downloadListener = DownloaderAgent
        )?.also { result ->
            downloadResultMap.update {
                it + (result.downloadUrl to result)
            }
        }
    }
}