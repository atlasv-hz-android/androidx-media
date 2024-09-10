package com.atlasv.android.mediax.downloader.datasource

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.FileOutputStream

/**
 * Created by weiping on 2024/9/8
 */


@OptIn(UnstableApi::class)
fun DataSource.saveDataSpec(dataSpec: DataSpec, destFile: File) {
    destFile.parentFile?.mkdirs()
    val tmpFile = File(destFile.parentFile, destFile.name + ".tmp")
    val fos = FileOutputStream(tmpFile)
    var bytesRead = 0
    val bufferLength = 1024
    val buffer = ByteArray(bufferLength)
    try {
        this.open(dataSpec)
        while (bytesRead != C.RESULT_END_OF_INPUT) {
            bytesRead = this.read(buffer, 0, bufferLength)
            if (bytesRead != C.RESULT_END_OF_INPUT) {
                fos.write(buffer, 0, bytesRead)
            }
        }
        tmpFile.renameTo(destFile)
    } finally {
        fos.close()
        this.close()
        tmpFile.delete()
    }
}