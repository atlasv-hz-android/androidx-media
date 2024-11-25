package com.atlasv.android.mediax.downloader.cache

/**
 * Created by weiping on 2024/11/5
 */
class SimpleRangeStrategy(val rangeCount: Int) : RangeCountStrategy {
    override fun getRangeCount(contentLength: Long): Int {
        return rangeCount
    }

    companion object {
        val SingleRangeStrategy = SimpleRangeStrategy(1)
    }
}

fun RangeCountStrategy.isSingleRange(): Boolean {
    return (this is SimpleRangeStrategy) && this.rangeCount == 1
}