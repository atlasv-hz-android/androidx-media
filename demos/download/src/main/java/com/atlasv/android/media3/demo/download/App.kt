package com.atlasv.android.media3.demo.download

import android.app.Application
import com.android.now.logger.ILogger
import timber.log.Timber

/**
 * Created by weiping on 2024/8/23
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        lateinit var app: App
    }
}

private class LoggerImpl(private val tag: String) : ILogger {
    override fun d(messageSupplier: () -> String) {
        Timber.tag(tag).d(messageSupplier)
    }

    override fun w(messageSupplier: () -> String) {
        Timber.tag(tag).w(messageSupplier)
    }

    override fun w(cause: Throwable?, messageSupplier: () -> String) {
        Timber.tag(tag).w(cause, messageSupplier)
    }

    override fun e(messageSupplier: () -> String) {
        Timber.tag(tag).e(messageSupplier)
    }

    override fun e(cause: Throwable?, messageSupplier: () -> String) {
        Timber.tag(tag).e(cause, messageSupplier)
    }
}