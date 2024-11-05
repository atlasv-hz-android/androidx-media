package com.atlasv.android.media3.demo.download

import android.app.Application
import com.atlasv.android.loader.ResourceContentLoader
import com.atlasv.android.mediax.downloader.util.MediaXLogger
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr
import timber.log.Timber

/**
 * Created by weiping on 2024/8/23
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        Timber.plant(Timber.DebugTree())
        ResourceContentLoader.loggerProvider = {
            Timber.tag("res-load")
        }
        MediaXLoggerMgr.loggerSupplier = {
            object : MediaXLogger {
                override fun d(messageSupplier: () -> String) {
                    Timber.tag("res-load-mediax").d(messageSupplier)
                }

                override fun w(messageSupplier: () -> String) {
                    Timber.tag("res-load-mediax").w(messageSupplier)
                }

                override fun w(cause: Throwable?, messageSupplier: () -> String) {
                    Timber.tag("res-load-mediax").w(cause, messageSupplier)
                }

                override fun e(messageSupplier: () -> String) {
                    Timber.tag("res-load-mediax").e(messageSupplier)
                }

                override fun e(cause: Throwable?, messageSupplier: () -> String) {
                    Timber.tag("res-load-mediax").e(cause, messageSupplier)
                }
            }
        }
    }

    companion object {
        lateinit var app: App
    }
}