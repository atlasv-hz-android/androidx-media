package com.atlasv.android.mediax.downloader.core

import androidx.media3.datasource.HttpUtil
import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.google.common.net.HttpHeaders
import kotlinx.coroutines.delay
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.toLongOrDefault

/**
 * Created by weiping on 2024/8/6
 */
class ContentLengthFetcher(private val okhttpClient: OkHttpClient) :
    ResourceContentFetcher<ContentRequestStringModel, Long> {
    private val retryTimes = 3
    override suspend fun fetch(inputData: ContentRequestStringModel): Long {
        (0 until retryTimes).forEach { _ ->
            performFetch(inputData).takeIf { it > 0 }?.also {
                return it
            }
            delay(1_000)
        }
        return -1
    }

    private fun performFetch(inputData: ContentRequestStringModel): Long {
        return byHeadMethod(uriString = inputData.uriString).takeIf { it > 0 } ?: byGetMethod(
            uriString = inputData.uriString
        )
    }

    private fun byHeadMethod(uriString: String): Long {
        val cacheHeader = CacheControl.Builder().noCache().noStore().build().toString()
        val request = Request.Builder().url(uriString).method("HEAD", null)
            .header("Cache-Control", cacheHeader)
            .build()
        return okhttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.headers["Content-Length"]?.toLongOrDefault(-1L) ?: -1L
            } else {
                -1
            }
        }
    }

    private fun byGetMethod(uriString: String): Long {
        val cacheHeader = CacheControl.Builder().noCache().noStore().build().toString()
        val request = Request.Builder().url(uriString)
            .header("Cache-Control", cacheHeader)
            .header(HttpHeaders.RANGE, "bytes=0-0")
            .build()
        return okhttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                HttpUtil.getDocumentSize(response.headers[HttpHeaders.CONTENT_RANGE])
            } else {
                -1
            }
        }
    }

    override fun cancel(key: String) {

    }
}