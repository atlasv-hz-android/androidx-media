package com.atlasv.android.media3.demo.download

import android.app.Application
import timber.log.Timber

/**
 * Created by weiping on 2024/8/23
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}