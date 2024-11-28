package com.atlasv.android.mediax.downloader.output

import java.io.OutputStream

/**
 * Created by weiping on 2024/11/28
 */
interface OutputTarget {
    fun getOutputStream(): OutputStream
}