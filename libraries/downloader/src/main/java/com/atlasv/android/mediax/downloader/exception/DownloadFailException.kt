package com.atlasv.android.mediax.downloader.exception

/**
 * Created by weiping on 2024/8/22
 */
class DownloadFailException(failedUrl: String, cause: Throwable?) :
    Exception("Download failed, url=$failedUrl", cause)