package com.atlasv.android.media3.demo.download.ui.theme

import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.request.ContentRequestStringModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.toLongOrDefault

/**
 * Created by weiping on 2024/8/6
 */
class ContentLengthFetcher(private val okhttpClient: OkHttpClient) :
    ResourceContentFetcher<ContentRequestStringModel, Long> {
    override suspend fun fetch(inputData: ContentRequestStringModel): Long {
        val request = Request.Builder().url(inputData.uriString).method("HEAD", null).build()
        return okhttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.headers["Content-Length"]?.toLongOrDefault(-1L) ?: -1L
            } else {
                -1
            }
        }
    }

    override fun cancel(key: String) {

    }
}