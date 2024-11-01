package com.atlasv.android.media3.demo.download

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.atlasv.android.mediax.downloader.core.MediaXCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Created by weiping on 2024/8/23
 */
@OptIn(UnstableApi::class)
class MainViewModel : ViewModel() {
    private val logger get() = Timber.tag("download-test")
    private val databaseProvider by lazy {
        StandaloneDatabaseProvider(appContext)
    }
    private val downloadDirectory by lazy {
        File(appContext.getExternalFilesDir(null), "player-download-cache").also {
            it.mkdirs()
        }
    }
    val mediaXCache by lazy {
        MediaXCache(
            appContext,
            SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider),
            okhttpClient
        )
    }
    private val okhttpClient by lazy { createOkhttpClient() }
    private val contentLengthLoader by lazy {
        ContentLengthLoader(okhttpClient = okhttpClient)
    }

    fun testDownload(downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                performDownload(downloadUrl)
            }.getOrElse {
                logger.e(it) { "testDownload failed" }
            }
        }
    }

    private suspend fun performDownload(downloadUrl: String) {
        mediaXCache.cache.removeResource(downloadUrl)
        val destFile = File(
            appContext.getExternalFilesDir(null),
            "player-downloads/${Uri.parse(downloadUrl).lastPathSegment}"
        ).also {
            it.parentFile?.mkdirs()
        }
        val totalLength =
            contentLengthLoader.fetch(ContentRequestStringModel(downloadUrl)).result
                ?: Long.MAX_VALUE
        logger.d { "Content-Length=$totalLength, url=$downloadUrl" }
        coroutineScope {
            val rangeCount = 3
            val rangeLength = totalLength / rangeCount
            val jobs = (0 until rangeCount).map { index ->
                val rangeStart = index * rangeLength
                async {
                    val dataSpec =
                        DataSpec.Builder().setUri(downloadUrl)
                            .setPosition((rangeStart).coerceAtLeast(0)).apply {
                                if (index != rangeCount - 1) {
                                    setLength(rangeLength)
                                }
                            }.build()
                    val dataSource = mediaXCache.createDataSource()
                    CacheWriter(dataSource, dataSpec, null, null).cache()
                    logger.d {
                        "[${dataSpec.position}+${dataSpec.length}->${if (dataSpec.length > 0) (dataSpec.position + dataSpec.length) else "End"}]:${
                            dataSource.cache.getProgressInfo(
                                downloadUrl
                            )
                        }"
                    }
                }
            }
            jobs.forEach {
                it.await()
            }
            val dataSpecMerge = DataSpec.Builder().setUri(downloadUrl).build()
            val dataSource3 = mediaXCache.createDataSource()
            logger.d { "Merge start, ${dataSource3.cache.getProgressInfo(downloadUrl)}" }
            CacheWriter(dataSource3, dataSpecMerge, null, null).cache()
            logger.d { "Merge finish, ${dataSource3.cache.getProgressInfo(downloadUrl)}" }
            dataSource3.saveDataSpec(dataSpecMerge, destFile)
            // adb pull /storage/emulated/0/Android/data/com.atlasv.android.media3.demo.download/files/player-downloads/
            logger.d { "Save to file:$destFile(${destFile.length()})" }
            dataSource3.cache.removeResource(downloadUrl)
        }
    }

    private fun createOkhttpClient(): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }
}


@OptIn(UnstableApi::class)
fun DataSource.saveDataSpec(dataSpec: DataSpec, destFile: File) {
    destFile.parentFile?.mkdirs()
    val tmpFile = File(destFile.parentFile, destFile.name + ".tmp")
    val fos = FileOutputStream(tmpFile)
    var bytesRead = 0
    val bufferLength = CacheWriter.DEFAULT_BUFFER_SIZE_BYTES
    val buffer = ByteArray(bufferLength)
    try {
        this.open(dataSpec)
        while (bytesRead != C.RESULT_END_OF_INPUT) {
            bytesRead = this.read(buffer, 0, bufferLength)
            if (bytesRead != C.RESULT_END_OF_INPUT) {
                fos.write(buffer, 0, bytesRead)
            }
        }
        destFile.delete()
        tmpFile.renameTo(destFile)
    } finally {
        fos.close()
        this.close()
        tmpFile.delete()
    }
}

@OptIn(UnstableApi::class)
fun Cache.getContentLength(key: String): Long {
    return getContentMetadata(key).get(ContentMetadata.KEY_CONTENT_LENGTH, 0)
}

@OptIn(UnstableApi::class)
fun Cache.getCachedBytes(key: String): Long {
    return getCachedBytes(key, 0, Long.MAX_VALUE)
}

@OptIn(UnstableApi::class)
fun Cache.isCacheComplete(key: String): Boolean {
    return getContentLength(key) > 0 && getCachedBytes(key) >= getContentLength(key)
}

@OptIn(UnstableApi::class)
fun Cache.getProgressInfo(downloadUrl: String): String {
    return "${this.getCachedBytes(downloadUrl)}/${getContentLength(downloadUrl)}(complete=${
        isCacheComplete(
            downloadUrl
        )
    })"
}