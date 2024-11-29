package com.atlasv.android.mediax.downloader.exception

import kotlinx.coroutines.CancellationException
import java.io.InterruptedIOException

/**
 * Created by weiping on 2024/11/29
 */

/**
 * 判断是否用户主动取消/中断导致的异常，例如暂停下载。这类异常属于正常行为，不需要上报。
 */
fun Throwable.isIoCancelException(): Boolean {
    val causes = listOfNotNull(this, this.cause)
    return causes.any {
        it is InterruptedIOException || it is CancellationException
    }
}

fun Throwable.wrapAsDownloadFailedException(downloadUrl: String): Throwable {
    return DownloadFailException(downloadUrl, this)
}