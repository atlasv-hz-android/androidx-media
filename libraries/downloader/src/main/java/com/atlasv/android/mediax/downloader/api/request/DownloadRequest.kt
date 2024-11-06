package com.atlasv.android.mediax.downloader.api.request

import com.atlasv.android.loader.request.ContentRequestModel
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy
import java.io.File

/**
 * Created by weiping on 2024/9/8
 */
class DownloadRequest(
    val url: String,
    val id: String,
    val destFile: File,
    val rangeCountStrategy: RangeCountStrategy = SimpleRangeStrategy(1)
) :
    ContentRequestModel {
    override fun getResourceKey(): String {
        return id
    }
}