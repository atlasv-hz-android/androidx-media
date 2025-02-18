package androidx.media3.common

import com.google.common.net.MediaType

/**
 * Created by weiping on 2025/2/17
 */
fun MediaType.guessSubtype(): String {
    if (!this.hasWildcard()) {
        return this.subtype()
    }
    return if (this.`is`(MediaType.ANY_IMAGE_TYPE)) {
        "jpg"
    } else if (this.`is`(MediaType.ANY_VIDEO_TYPE)) {
        "mp4"
    } else if (this.`is`(MediaType.ANY_AUDIO_TYPE)) {
        "mp3"
    } else {
        ""
    }
}