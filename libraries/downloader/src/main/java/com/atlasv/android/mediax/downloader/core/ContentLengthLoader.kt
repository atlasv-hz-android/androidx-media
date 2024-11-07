package com.atlasv.android.mediax.downloader.core

import androidx.media3.common.C
import com.atlasv.android.loader.ResourceContentLoader
import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.request.ContentRequestStringModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient

/**
 * Created by weiping on 2024/8/6
 */
class ContentLengthLoader(okhttpClient: OkHttpClient) :
    ResourceContentLoader<ContentRequestStringModel, Long>(
        listener = null,
        validPredicate = { it > 0 }) {
    override val fetchers: List<ResourceContentFetcher<ContentRequestStringModel, Long>> =
        listOf(ContentLengthFetcher(okhttpClient))
    val resultMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    suspend fun batchFetch(urls: Set<String>) {
        coroutineScope {
            urls.forEach { uriString ->
                val contentLength = fetch(ContentRequestStringModel(uriString = uriString)).result
                contentLength?.also {
                    resultMap.update {
                        it + (uriString to contentLength)
                    }
                }
            }
        }
    }

    suspend fun getContentLengthOrUnset(uriString: String): Long {
        return this.fetch(ContentRequestStringModel(uriString)).result?.takeIf { it > 0 }
            ?: C.LENGTH_UNSET.toLong()
    }
}