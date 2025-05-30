package com.android.now.mediax.effect.ext.db.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.media3.effect.ColorLut
import androidx.media3.effect.SingleColorLut
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size

/**
 * Created by weiping on 2024/12/26
 */
fun Bitmap.asSingleColorLut(): SingleColorLut {
    return SingleColorLut.createFromSquareBitmap(this)
}

suspend fun ImageLoader.loadAsLutBitmap(context: Context, data: String): Bitmap? {
    val drawable = this.execute(
        ImageRequest.Builder(context).data(data)
            .size(Size.ORIGINAL) // 必须保持原大小
            .bitmapConfig(Bitmap.Config.ARGB_8888) // 必须为ARGB_8888格式
            .build()
    ).drawable
    return (drawable as? BitmapDrawable)?.bitmap
}

suspend fun ImageLoader.loadAsColorLut(context: Context, data: String): ColorLut? {
    return loadAsLutBitmap(context, data)?.asSingleColorLut()
}

