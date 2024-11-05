package com.atlasv.android.mediax.downloader.cache

/**
 * Created by weiping on 2024/11/5
 */
interface RangeCountStrategy {
    /**
     * 分片数
     *
     * [contentLength] 文件总大小
     */
    fun getRangeCount(contentLength: Long): Int
}