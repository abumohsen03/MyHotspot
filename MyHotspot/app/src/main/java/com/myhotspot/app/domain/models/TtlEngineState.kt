package com.myhotspot.app.domain.models

enum class TtlEngineMode(val displayNameEn: String, val displayNameAr: String) {
    IPTABLES_TTL("iptables (xt_TTL)", "نظام iptables (هدف TTL)"),
    NFTABLES("nftables (nft)", "نظام nftables"),
    SYSCTL("sysctl (Kernel Default)", "نظام sysctl (افتراضي النواة)"),
    NONE("Inactive / Stopped", "متوقف / غير نشط")
}

data class TtlEngineState(
    val isRunning: Boolean = false,
    val activeMode: TtlEngineMode = TtlEngineMode.NONE,
    val ttlValue: Int = 65,
    val upstreamInterface: String = "None",
    val hotspotInterface: String = "None",
    val isIpForwardingEnabled: Boolean = false,
    val isNatActive: Boolean = false,
    val isIpv6Blocked: Boolean = false,
    val isDnsRedirectActive: Boolean = false,
    val errorMessage: String? = null
)
