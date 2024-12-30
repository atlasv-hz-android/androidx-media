package com.atlasv.android.mediax.effect.ext.util

import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.google.common.collect.ImmutableList

/**
 * Created by weiping on 2024/12/30
 */

/**
 * 对视频应用单效果的便捷API
 */
fun MediaItem.asCompositionWithEffect(effect: Effect): Composition {
    return asCompositionWithEffects(listOf(effect))
}

/**
 * 对视频应用效果列表的便捷API
 */
fun MediaItem.asCompositionWithEffects(effects: List<Effect>): Composition {
    val editedMediaItemBuilder = EditedMediaItem.Builder(this)
    editedMediaItemBuilder.setEffects(Effects(ImmutableList.of(), ImmutableList.copyOf(effects)))
    val compositionBuilder =
        Composition.Builder(
            EditedMediaItemSequence.Builder(editedMediaItemBuilder.build()).build()
        )
    return compositionBuilder.build()
}