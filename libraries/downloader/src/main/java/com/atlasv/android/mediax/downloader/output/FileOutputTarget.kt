package com.atlasv.android.mediax.downloader.output

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Created by weiping on 2024/11/28
 */
class FileOutputTarget(private val targetFileSupplier: () -> File) : OutputTarget {
    override fun getOutputStream(): OutputStream {
        val targetFile = targetFileSupplier()
        targetFile.parentFile?.mkdirs()
        return FileOutputStream(targetFile)
    }
}