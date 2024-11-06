package com.atlasv.android.mediax.downloader.datasource

import androidx.media3.datasource.DataSource

/**
 *
 * [支持的网络堆栈](https://developer.android.com/media/media3/exoplayer/network-stacks?hl=zh-cn#supported-network)
 * * 当前仅支持OKhttp
 * * Android 最新推荐Cronet，后续加入支持，AB Test 看效果(>=API 34)
 *
 * Created by weiping on 2024/11/6
 */
class ConfigurableUpstreamStrategy(private val okhttpStrategy: OkhttpUpstreamStrategy) :
    UpstreamStrategy {
    override fun createDataSourceFactory(): DataSource.Factory {
        return okhttpStrategy.createDataSourceFactory()
    }
}