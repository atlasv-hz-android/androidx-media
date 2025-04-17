package androidx.media3.ui.compose.ext.data

import androidx.annotation.OptIn
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.zeroVideoSize
import com.google.common.net.MediaType

/**
 * Created by weiping on 2024/7/18
 */
@OptIn(UnstableApi::class)
interface IMediaItemModel {
    fun getUri(): String
    fun getPreviewUri(): String?
    fun getMediaType(): MediaType
    fun getValidPreviewUri(): String {
        return getPreviewUri() ?: getUri()
    }

    fun getWidth(): Int
    fun getHeight(): Int
    fun getChildItems(): List<IMediaItemModel>
    fun isGroup(): Boolean
    fun wrapChildAt(index: Int): IMediaItemModel

    fun getVideoSize(): VideoSize {
        return if (getWidth() > 0 && getHeight() > 0) {
            VideoSize(getWidth(), getHeight())
        } else {
            zeroVideoSize()
        }
    }

    /**
     * 底层播放页不需要知道下载状态，减少方法调用
     */
    fun asPurePlayerModel(): IMediaItemModel
}
