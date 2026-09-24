package com.myhotspot.app.domain.models

data class DiagnosticItem(
    val id: String,
    val title: String,
    val isPassed: Boolean,
    val detail: String,
    val isCritical: Boolean = true
)

data class CommandLog(
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val exitCode: Int,
    val output: String
)

data class ConnectedDevice(
    val ip: String,
    val mac: String,
    val hostname: String = "Connected Client",
    val state: String = "REACHABLE",
    val interfaceName: String = "Hotspot"
)

data class TtlSettings(
    val ttlValue: Int = 65,
    val blockIpv6: Boolean = false,
    val redirectDns: Boolean = false,
    val autoStartOnHotspot: Boolean = true,
    val startOnBoot: Boolean = false
)
