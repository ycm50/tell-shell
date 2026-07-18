package com.tellshell.app

import android.app.Application
import rikka.shizuku.ShizukuProvider

class TellShellApp : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        ShizukuProvider.disableAutomaticSuiInitialization()
        super.attachBaseContext(base)
    }
}
