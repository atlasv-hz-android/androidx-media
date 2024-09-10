package com.atlasv.android.mediax.downloader.datasource

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import java.io.File
import java.io.FileOutputStream

/**
 * Created by weiping on 2024/9/8
 */


@OptIn(UnstableApi::class)
fun DataSource.saveDataSpec(
    dataSpec: DataSpec, destFile: File,
    temporaryBuffer: ByteArray = ByteArray(
        CacheWriter.DEFAULT_BUFFER_SIZE_BYTES
    )
) {
    destFile.parentFile?.mkdirs()
    val tmpFile = File(destFile.parentFile, destFile.name + ".tmp")
    val fos = FileOutputStream(tmpFile)
    var bytesRead = 0
    try {
        this.open(dataSpec)
        while (bytesRead != C.RESULT_END_OF_INPUT) {
            bytesRead = this.read(temporaryBuffer, 0, temporaryBuffer.size)
            if (bytesRead != C.RESULT_END_OF_INPUT) {
                fos.write(temporaryBuffer, 0, bytesRead)
            }
        }
        tmpFile.renameTo(destFile)
    } finally {
        fos.close()
        this.close()
        tmpFile.delete()
    }
}