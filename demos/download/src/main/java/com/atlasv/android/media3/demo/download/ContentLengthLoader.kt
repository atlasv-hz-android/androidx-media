package com.atlasv.android.media3.demo.download

import com.atlasv.android.loader.ResourceContentLoader
import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.atlasv.android.media3.demo.download.ui.theme.ContentLengthFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient

/**
 * Created by weiping on 2024/8/6
 */
class ContentLengthLoader(okhttpClient: OkHttpClient) :
    ResourceContentLoader<ContentRequestStringModel, Long>(listener = null, validPredicate = { it > 0 }) {
    override val fetchers: List<ResourceContentFetcher<ContentRequestStringModel, Long>> =
        listOf(ContentLengthFetcher(okhttpClient))
    val resultMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    suspend fun batchFetch(urls: Set<String>) {
        coroutineScope {
            val jobs = urls.map {
                async {
                    fetch(ContentRequestStringModel(uriString = it))
                }
            }
            val results = jobs.awaitAll().mapNotNull { response ->
                response.result?.let { result ->
                    response.inputModel.uriString to result
                }
            }.associateBy(keySelector = { it.first }, valueTransform = { it.second })
            resultMap.update {
                it + results
            }
        }
    }
}