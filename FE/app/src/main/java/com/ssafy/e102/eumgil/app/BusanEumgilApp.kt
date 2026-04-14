package com.ssafy.e102.eumgil.app

import android.app.Application

class BusanEumgilApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(context = this)
    }
}
