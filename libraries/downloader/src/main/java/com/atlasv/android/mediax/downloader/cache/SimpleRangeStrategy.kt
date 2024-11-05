package com.atlasv.android.mediax.downloader.cache

/**
 * Created by weiping on 2024/11/5
 */
class SimpleRangeStrategy(private val rangeCount: Int) : RangeCountStrategy {
    override fun getRangeCount(contentLength: Long): Int {
        return rangeCount
    }
}