package com.atlasv.android.mediax.downloader.core

/**
 * Created by weiping on 2024/11/11
 */
class CallbackRateLimiter(private val intervalMillis: Long) {

    // 每次调用记录时间，以便计算连续两次调用的间隔
    private var lastCheckTime = 0L

    // 时间起点
    private var firstCheckTime = 0L
    fun checkLimit(forceCheck: Boolean, action: (durationMillis: Long) -> Unit) {
        val nowMillis = System.currentTimeMillis()
        if (firstCheckTime <= 0) {
            firstCheckTime = nowMillis
        }
        val durationMillis = nowMillis - firstCheckTime
        val sinceLastCheck = nowMillis - lastCheckTime
        lastCheckTime = nowMillis
        if ((forceCheck && durationMillis > 0) || sinceLastCheck >= intervalMillis) {
            action(durationMillis)
        }
    }
}