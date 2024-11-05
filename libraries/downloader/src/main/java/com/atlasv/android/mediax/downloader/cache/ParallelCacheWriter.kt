package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.CacheWriter.ProgressListener
import com.atlasv.android.mediax.downloader.core.MediaXCache
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Created by weiping on 2024/11/5
 */
class ParallelCacheWriter(
    private val mediaXCache: MediaXCache
) {
    suspend fun cache(
        uriString: String,
        contentLength: Long,
        rangeCountStrategy: RangeCountStrategy,
        progressListener: ProgressListener?
    ) {
        val isContentLengthKnown = contentLength > 0
        val rangeCount =
            if (!isContentLengthKnown) 1 else rangeCountStrategy.getRangeCount(contentLength = contentLength)
                .coerceAtLeast(1)
        val rangeLength = if (isContentLengthKnown) contentLength / rangeCount else contentLength
        val baseDataSpec = DataSpec.Builder().setUri(uriString)
        mediaXLogger?.d { "Set range count to $rangeCount, contentLength=$contentLength, uriString=$uriString" }
        coroutineScope {
            val jobs = (0 until rangeCount).map { index ->
                async {
                    val rangeStart = rangeLength * index
                    val dataSpec =
                        baseDataSpec.setPosition(rangeStart).apply {
                            if (index != rangeCount - 1) {
                                setLength(rangeLength)
                            }
                        }.build()
                    mediaXLogger?.d { "Build DataSpec: $dataSpec" }
                    val dataSource = mediaXCache.createDataSource()
                    CacheWriter(dataSource, dataSpec, null, null).cache()
                }
            }
            jobs.awaitAll()
        }
    }
}

