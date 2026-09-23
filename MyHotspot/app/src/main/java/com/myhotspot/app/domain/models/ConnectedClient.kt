package com.myhotspot.app.domain.models

data class ConnectedClient(
    val ipAddress: String,
    val macAddress: String = "",
    val deviceName: String = "Client Device",
    val isBypassActive: Boolean = false
)
