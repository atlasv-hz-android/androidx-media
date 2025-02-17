package com.atlasv.android.mediax.downloader.output

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Created by weiping on 2024/11/28
 */
class FileOutputTarget(private val targetFileSupplier: () -> File) : OutputTarget {
    val targetFile by lazy {
        targetFileSupplier()
    }

    override fun getOutputStream(): OutputStream {
        targetFile.parentFile?.mkdirs()
        return FileOutputStream(targetFile)
    }

    override fun toString(): String {
        return targetFile.absolutePath
    }

    override fun onSucceed() {
        // do nothing
    }
}

fun File.asOutputTarget(): FileOutputTarget {
    return FileOutputTarget(targetFileSupplier = { this })
}

fun String?.asUuidFileName(): String {
    val type = this?.substringAfterLast(".", "").orEmpty()
    val suffix = if (type.isNotEmpty()) ".$type" else ""
    return UUID.randomUUID().toString() + suffix
}