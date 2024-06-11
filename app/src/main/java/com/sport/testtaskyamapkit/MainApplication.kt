package com.sport.testtaskyamapkit

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey("a64ed739-363a-40fc-bf91-efa9da252cd4")
        MapKitFactory.initialize(this)
    }
}