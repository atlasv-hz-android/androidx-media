package androidx.media3.common

import com.google.common.net.MediaType
import org.junit.Test

/**
 * Created by weiping on 2024/8/18
 */
class MediaTypeTest {

    @Test
    fun test() {
        listOf(
            MediaType.ANY_VIDEO_TYPE,
            MediaType.ANY_IMAGE_TYPE,
            MediaType.ANY_AUDIO_TYPE,
            MediaType.ANY_FONT_TYPE,
            MediaType.ANY_TEXT_TYPE,
            MediaType.GIF,
            MediaType.JPEG,
            MediaType.MP4_VIDEO,
            MediaType.MP4_AUDIO,
            MediaType.THREE_GPP_VIDEO,
            MediaType.parse("audio/mp3")
        ).forEach {
            println("$it, subtype=${it.subtype()}, hasWildcard=${it.hasWildcard()}, guessSubtype=${it.guessSubtype()}")
        }
    }
}