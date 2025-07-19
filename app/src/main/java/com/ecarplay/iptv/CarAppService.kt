package com.ecarplay.iptv

import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class CarAppService : CarAppService() {
    
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return IPTVSession()
    }
}

class IPTVSession : Session() {
    
    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return M3UFileListScreen(carContext)
    }
}