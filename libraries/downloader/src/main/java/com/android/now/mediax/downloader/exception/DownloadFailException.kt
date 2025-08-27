package com.android.now.mediax.downloader.exception

/**
 * Created by weiping on 2024/8/22
 */
class DownloadFailException(failedUrl: String, cause: Throwable?, isNewTask: Boolean) :
    Exception("Download failed, isNewTask = $isNewTask, url=$failedUrl", cause)