package com.atlasv.android.mediax.downloader.datasource

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import com.atlasv.android.mediax.downloader.output.OutputTarget
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger

/**
 * Created by weiping on 2024/9/8
 */


@OptIn(UnstableApi::class)
fun DataSource.saveDataSpec(
    dataSpec: DataSpec, outputTarget: OutputTarget,
    temporaryBuffer: ByteArray = ByteArray(
        CacheWriter.DEFAULT_BUFFER_SIZE_BYTES
    )
) {
    val outputStream = outputTarget.getOutputStream()
    var bytesRead = 0
    outputStream.use {
        try {
            this.open(dataSpec)
            while (bytesRead != C.RESULT_END_OF_INPUT) {
                bytesRead = this.read(temporaryBuffer, 0, temporaryBuffer.size)
                if (bytesRead != C.RESULT_END_OF_INPUT) {
                    outputStream.write(temporaryBuffer, 0, bytesRead)
                }
            }
        } finally {
            this.close()
        }
    }
}

@OptIn(UnstableApi::class)
fun Cache.getContentLength(key: String): Long {
    return getContentMetadata(key).get(ContentMetadata.KEY_CONTENT_LENGTH, 0)
}

@OptIn(UnstableApi::class)
fun Cache.getCachedBytes(key: String): Long {
    return getCachedBytes(key, 0, Long.MAX_VALUE)
}

@OptIn(UnstableApi::class)
fun Cache.isCacheComplete(key: String): Boolean {
    return getContentLength(key) > 0 && getCachedBytes(key) >= getContentLength(key)
}

@OptIn(UnstableApi::class)
fun Cache.getProgressInfo(downloadUrl: String): String {
    return "${this.getCachedBytes(downloadUrl)}/${getContentLength(downloadUrl)}(complete=${
        isCacheComplete(
            downloadUrl
        )
    })"
}

fun Cache.removeResourceWithTrack(uriString: String) {
    mediaXLogger?.w { "Remove resource(${this.getCachedBytes(uriString)}), key=$uriString" }
    this.removeResource(uriString)
    mediaXLogger?.w { "Resource removed,remain size=${getCachedBytes(uriString)}, key=$uriString" }
}