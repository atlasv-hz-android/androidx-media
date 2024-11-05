package com.atlasv.android.media3.demo.download

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.mediax.downloader.api.request.DownloadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Created by weiping on 2024/8/23
 */
@OptIn(UnstableApi::class)
class MainViewModel : ViewModel() {
    private val logger get() = Timber.tag("download-test")
    fun testDownload(downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            performDownload(downloadUrl)
        }
    }

    private suspend fun performDownload(downloadUrl: String) {
        DownloaderAgent.client.fetch(DownloadRequest(
            url = downloadUrl,
            id = downloadUrl,
            destFile = File(appContext.getExternalFilesDir(null), "download-files").also {
                it.mkdirs()
            }
        ))
    }
}