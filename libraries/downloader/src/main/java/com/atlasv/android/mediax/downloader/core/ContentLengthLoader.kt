package com.atlasv.android.mediax.downloader.core

import com.atlasv.android.loader.ResourceContentLoader
import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.request.ContentRequestStringModel
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
}