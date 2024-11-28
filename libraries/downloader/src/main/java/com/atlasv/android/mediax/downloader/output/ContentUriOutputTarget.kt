package com.atlasv.android.mediax.downloader.output

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.google.common.net.MediaType
import java.io.OutputStream

/**
 * Created by weiping on 2024/11/28
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ContentUriOutputTarget(
    val appContext: Context,
    val downloadUrl: String,
    val relativePath: String,
    val mediaType: MediaType,
    val destNameCreator: (downloadUrl: String) -> String = {
        Uri.parse(downloadUrl).lastPathSegment ?: "default-${System.currentTimeMillis()}"
    }
) : OutputTarget {
    private val destUriWithDetails by lazy {
        val resolver = appContext.contentResolver
        val displayName = destNameCreator(downloadUrl)
        val nowSeconds = System.currentTimeMillis() / 1000
        val mediaDetails = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.DATE_ADDED, nowSeconds)
            put(MediaStore.MediaColumns.DATE_MODIFIED, nowSeconds)
            put(MediaStore.MediaColumns.MIME_TYPE, mediaType.toString())
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$relativePath"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val destMediaUri =
            resolver.insert(downloadSaveUri, mediaDetails)
                ?: error("Can not insert $mediaDetails to $downloadSaveUri")
        destMediaUri to mediaDetails
    }

    @SuppressLint("Recycle")
    override fun getOutputStream(): OutputStream {
        val (destMediaUri, _) = destUriWithDetails
        // 调用者负责use{}或close
        return appContext.contentResolver.openOutputStream(destMediaUri, "w")
            ?: error("openOutputStream failed:$destMediaUri")
    }

    override fun onSucceed() {
        val (destMediaUri, mediaDetails) = destUriWithDetails
        mediaDetails.clear()
        mediaDetails.put(MediaStore.MediaColumns.IS_PENDING, 0)
        appContext.contentResolver.update(destMediaUri, mediaDetails, null, null)
    }

    override fun toString(): String {
        return destUriWithDetails.first.toString()
    }

    companion object {
        val downloadSaveUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }
}