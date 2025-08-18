package com.atlasv.android.media3.demo.download

import android.os.Build
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.android.now.appcontext.AppContextHolder.Companion.appContext
import com.android.now.mediax.downloader.cache.SimpleRangeStrategy
import com.android.now.mediax.downloader.output.ContentUriOutputTarget
import com.android.now.mediax.downloader.output.DownloadResult
import com.android.now.mediax.downloader.output.FileOutputTarget
import com.android.now.mediax.downloader.output.OutputTarget
import com.android.now.mediax.downloader.output.asUuidFileName
import com.google.common.net.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
                it to downloadResultMap[it.taskId]
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun testDownload(downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            testRangeCount(downloadUrl, 3)
        }
    }

    private fun createOutputTarget(downloadUrl: String, id: String): OutputTarget {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            ContentUriOutputTarget(
                appContext = App.app,
                downloadUrl = downloadUrl,
                relativePath = "DemoFiles",
                mediaType = MediaType.MP4_VIDEO, // TODO 根据实际解析结果来传
                destNameCreator = {
                    "$id-${downloadUrl.toUri().lastPathSegment}"
                }
            )
        } else {
            FileOutputTarget(
                targetFileSupplier = {
                    File(
                        appContext.getExternalFilesDir(null),
                        "download-files/${
                            downloadUrl.toUri().lastPathSegment.asUuidFileName(
                                MediaType.ANY_VIDEO_TYPE
                            )
                        }"
                    )
                }
            )
        }
    }

    private suspend fun testRangeCount(
        downloadUrl: String,
        rangeCount: Int,
        id: String = downloadUrl,
    ) {
        performDownload(downloadUrl, SimpleRangeStrategy(rangeCount), id)
    }

    private suspend fun performDownload(
        downloadUrl: String,
        rangeStrategy: SimpleRangeStrategy,
        id: String = downloadUrl,
    ) {
        DownloaderAgent.downloadCore.download(
            downloadUrl = downloadUrl,
            taskId = id,
            outputTarget = createOutputTarget(downloadUrl, id),
            rangeCountStrategy = rangeStrategy,
            downloadListener = null
        )?.also { result ->
            downloadResultMap.update {
                it + (result.taskId to result)
            }
        }
    }
}