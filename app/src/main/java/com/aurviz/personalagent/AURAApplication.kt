package com.aurviz.personalagent

import android.app.Application
import com.aurviz.personalagent.di.AppContainer
import com.aurviz.personalagent.utils.Logger

class AURAApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Logger.i("AURAApplication", "AURA Personal Agent starting...")

        appContainer = AppContainer(applicationContext)

        Logger.i("AURAApplication", "AURA initialized successfully")
    }

    companion object {
        const val APP_NAME = "AURA"
        const val VERSION = "1.0.0"
    }
}
