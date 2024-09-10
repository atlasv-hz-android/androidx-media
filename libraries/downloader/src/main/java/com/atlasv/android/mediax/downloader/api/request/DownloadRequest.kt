package com.atlasv.android.mediax.downloader.api.request

import com.atlasv.android.loader.request.ContentRequestModel
import java.io.File

/**
 * Created by weiping on 2024/9/8
 */
data class DownloadRequest(val url: String, val id: String, val destFile: File) :
    ContentRequestModel {
    override fun getResourceKey(): String {
        return id
    }
}