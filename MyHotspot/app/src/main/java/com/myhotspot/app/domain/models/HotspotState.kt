package com.myhotspot.app.domain.models

sealed interface HotspotState {
    data object Idle : HotspotState
    data object Starting : HotspotState
    data class Active(
        val ssid: String,
        val passphrase: String,
        val ipAddress: String = "192.168.43.1",
        val proxyPort: Int = 8282,
        val startTime: Long = System.currentTimeMillis()
    ) : HotspotState
    data class Error(val message: String) : HotspotState
}
