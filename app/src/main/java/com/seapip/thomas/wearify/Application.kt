package com.seapip.thomas.wearify

import com.seapip.thomas.wearify.di.appModule
import org.koin.android.ext.android.startKoin

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin(this, appModule)
    }
}