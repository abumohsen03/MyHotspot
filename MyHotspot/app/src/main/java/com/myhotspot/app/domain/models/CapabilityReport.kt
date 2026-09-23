package com.myhotspot.app.domain.models

data class CapabilityReport(
    val is5GHzSupported: Boolean,
    val isWifiDirectSupported: Boolean,
    val isLocalOnlyHotspotSupported: Boolean,
    val isStaApConcurrencySupported: Boolean,
    val localIpAddress: String,
    val androidVersion: String,
    val deviceModel: String
)
