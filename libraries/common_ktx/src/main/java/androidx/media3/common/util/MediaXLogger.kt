package androidx.media3.common.util

/**
 * Created by weiping on 2025/1/10
 */
interface MediaXLogger {
    fun d(messageSupplier: () -> String)
    fun w(messageSupplier: () -> String)
    fun e(messageSupplier: () -> String)
    fun w(cause: Throwable?, messageSupplier: () -> String)
    fun e(cause: Throwable?, messageSupplier: () -> String)
}