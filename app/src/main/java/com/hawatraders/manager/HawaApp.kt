package com.hawatraders.manager

import android.app.Application

class HawaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Clean abandoned temp files without blocking startup.
        Thread { container.imageStore.purgeStaleTemp() }.start()
    }
}
