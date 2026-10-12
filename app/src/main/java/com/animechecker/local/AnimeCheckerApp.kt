package com.animechecker.local

import android.app.Application
import com.animechecker.local.di.AppContainer

class AnimeCheckerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
