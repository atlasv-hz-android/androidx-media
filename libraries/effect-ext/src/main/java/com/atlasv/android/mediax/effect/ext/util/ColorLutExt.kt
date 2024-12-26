package com.atlasv.android.mediax.effect.ext.util

import android.graphics.Bitmap
import androidx.media3.effect.SingleColorLut

/**
 * Created by weiping on 2024/12/26
 */
fun Bitmap.asSingleColorLut(): SingleColorLut {
    return SingleColorLut.createFromSquareBitmap(this)
}