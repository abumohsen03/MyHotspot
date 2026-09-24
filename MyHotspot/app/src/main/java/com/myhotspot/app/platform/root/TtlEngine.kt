package com.myhotspot.app.platform.root

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.myhotspot.app.domain.models.TtlEngineMode
import com.myhotspot.app.domain.models.TtlEngineState
import com.myhotspot.app.domain.models.TtlSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtlEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: RootShellManager
) {
    private val tag = "TtlEngine"

    private val _engineState = MutableStateFlow(TtlEngineState())
    val engineState: StateFlow<TtlEngineState> = _engineState.asStateFlow()

    private var activeUpstream: String = "None"
    private var activeHotspot: String = "None"
    private var currentMode: TtlEngineMode = TtlEngineMode.NONE

    /**
     * Starts the TTL Hotspot engine:
     * 1. Detects upstream & hotspot interfaces.
     * 2. Enables Linux kernel IP forwarding.
     * 3. Applies TTL rules with Primary (iptables) -> Fallback A (nftables) -> Fallback B (sysctl).
     * 4. Applies optional IPv6 block and DNS redirect rules if requested.
     */
    suspend fun start(settings: TtlSettings): Boolean = withContext(Dispatchers.IO) {
        if (!shell.isRootAvailable()) {
            _engineState.value = _engineState.value.copy(
                isRunning = false,
                errorMessage = "Root permission (Magisk/KernelSU) is required!"
            )
            return@withContext false
        }

        activeUpstream = detectUpstreamInterface()
        activeHotspot = detectHotspotInterface()

        Log.i(tag, "Detected interfaces -> Upstream: $activeUpstream, Hotspot: $activeHotspot")

        // 1. Enable IP Forwarding
        enableIpForwarding()

        // 2. Ensure NAT/MASQUERADE on upstream
        if (activeUpstream != "None") {
            ensureNatMasquerade(activeUpstream)
        }

        // 3. Try Primary: iptables xt_TTL
        var mode = TtlEngineMode.IPTABLES_TTL
        val iptablesSuccess = applyIptablesTtl(settings.ttlValue, activeUpstream)

        if (!iptablesSuccess) {
            Log.w(tag, "iptables TTL target failed, cascading to Fallback A (nftables)...")
            mode = TtlEngineMode.NFTABLES
            val nftSuccess = applyNftablesTtl(settings.ttlValue, activeUpstream)

            if (!nftSuccess) {
                Log.w(tag, "nftables failed, cascading to Fallback B (sysctl)...")
                mode = TtlEngineMode.SYSCTL
                applySysctlTtl(settings.ttlValue)
            }
        }

        currentMode = mode

        // 4. Handle optional settings
        if (settings.blockIpv6 && activeHotspot != "None") {
            applyIpv6Block(activeHotspot)
        }

        if (settings.redirectDns && activeHotspot != "None") {
            applyDnsRedirect(activeHotspot)
        }

        _engineState.value = TtlEngineState(
            isRunning = true,
            activeMode = mode,
            ttlValue = settings.ttlValue,
            upstreamInterface = activeUpstream,
            hotspotInterface = activeHotspot,
            isIpForwardingEnabled = true,
            isNatActive = true,
            isIpv6Blocked = settings.blockIpv6,
            isDnsRedirectActive = settings.redirectDns,
            errorMessage = null
        )

        true
    }

    /**
     * Clean teardown of all applied rules and restoring system defaults.
     */
    suspend fun stop(settings: TtlSettings) = withContext(Dispatchers.IO) {
        if (!shell.isRootAvailable()) return@withContext

        // Remove iptables rules
        removeIptablesTtl(settings.ttlValue, activeUpstream)
        removeNatMasquerade(activeUpstream)

        if (settings.blockIpv6 && activeHotspot != "None") {
            removeIpv6Block(activeHotspot)
        }

        if (settings.redirectDns && activeHotspot != "None") {
            removeDnsRedirect(activeHotspot)
        }

        // Remove nftables tables
        shell.execute("nft delete table ip ttl_mangle 2>/dev/null")
        shell.execute("nft delete table ip6 ttl_mangle 2>/dev/null")

        // Restore default sysctl TTL to 64
        shell.execute("sysctl -w net.ipv4.ip_default_ttl=64")
        shell.execute("sysctl -w net.ipv6.conf.all.hop_limit=64")

        _engineState.value = TtlEngineState(
            isRunning = false,
            activeMode = TtlEngineMode.NONE,
            ttlValue = settings.ttlValue,
            upstreamInterface = "None",
            hotspotInterface = "None",
            isIpForwardingEnabled = false,
            isNatActive = false,
            isIpv6Blocked = false,
            isDnsRedirectActive = false,
            errorMessage = null
        )
    }

    private suspend fun enableIpForwarding() {
        shell.execute("echo 1 > /proc/sys/net/ipv4/ip_forward")
        shell.execute("sysctl -w net.ipv4.ip_forward=1")
        shell.execute("sysctl -w net.ipv6.conf.all.forwarding=1")
    }

    // --- Primary: iptables xt_TTL ---
    private suspend fun applyIptablesTtl(ttl: Int, upstream: String): Boolean {
        val outDev = if (upstream != "None") "-o $upstream" else ""
        
        // 1. Check if rule exists
        val checkIpv4 = shell.execute("iptables -t mangle -C POSTROUTING $outDev -j TTL --ttl-set $ttl")
        if (!checkIpv4.isSuccess) {
            val res = shell.execute("iptables -t mangle -I POSTROUTING $outDev -j TTL --ttl-set $ttl")
            if (!res.isSuccess) return false
        }

        // 2. Check IPv6 HL rule
        val checkIpv6 = shell.execute("ip6tables -t mangle -C POSTROUTING $outDev -j HL --hl-set $ttl")
        if (!checkIpv6.isSuccess) {
            shell.execute("ip6tables -t mangle -I POSTROUTING $outDev -j HL --hl-set $ttl")
        }

        return true
    }

    private suspend fun removeIptablesTtl(ttl: Int, upstream: String) {
        val outDev = if (upstream != "None") "-o $upstream" else ""
        shell.execute("iptables -t mangle -D POSTROUTING $outDev -j TTL --ttl-set $ttl 2>/dev/null")
        shell.execute("ip6tables -t mangle -D POSTROUTING $outDev -j HL --hl-set $ttl 2>/dev/null")
    }

    // --- Fallback A: nftables ---
    private suspend fun applyNftablesTtl(ttl: Int, upstream: String): Boolean {
        val oifRule = if (upstream != "None") "oifname \"$upstream\"" else ""
        
        val cmds = listOf(
            "nft add table ip ttl_mangle",
            "nft 'add chain ip ttl_mangle postrouting { type filter hook postrouting priority mangle; }'",
            "nft add rule ip ttl_mangle postrouting $oifRule ip ttl set $ttl",
            "nft add table ip6 ttl_mangle",
            "nft 'add chain ip6 ttl_mangle postrouting { type filter hook postrouting priority mangle; }'",
            "nft add rule ip6 ttl_mangle postrouting $oifRule ip6 hoplimit set $ttl"
        )
        val results = cmds.map { shell.execute(it) }
        return results.all { it.isSuccess }
    }

    // --- Fallback B: sysctl ---
    private suspend fun applySysctlTtl(ttl: Int) {
        shell.execute("sysctl -w net.ipv4.ip_default_ttl=$ttl")
        shell.execute("sysctl -w net.ipv6.conf.all.hop_limit=$ttl")
    }

    // --- NAT / MASQUERADE ---
    private suspend fun ensureNatMasquerade(upstream: String) {
        val check = shell.execute("iptables -t nat -C POSTROUTING -o $upstream -j MASQUERADE")
        if (!check.isSuccess) {
            shell.execute("iptables -t nat -I POSTROUTING -o $upstream -j MASQUERADE")
        }
    }

    private suspend fun removeNatMasquerade(upstream: String) {
        if (upstream != "None") {
            shell.execute("iptables -t nat -D POSTROUTING -o $upstream -j MASQUERADE 2>/dev/null")
        }
    }

    // --- Optional Extra Detection Toggles ---
    private suspend fun applyIpv6Block(hotspotIface: String) {
        shell.execute("ip6tables -I FORWARD -i $hotspotIface -j DROP")
        shell.execute("echo 1 > /proc/sys/net/ipv6/conf/$hotspotIface/disable_ipv6 2>/dev/null")
    }

    private suspend fun removeIpv6Block(hotspotIface: String) {
        shell.execute("ip6tables -D FORWARD -i $hotspotIface -j DROP 2>/dev/null")
        shell.execute("echo 0 > /proc/sys/net/ipv6/conf/$hotspotIface/disable_ipv6 2>/dev/null")
    }

    private suspend fun applyDnsRedirect(hotspotIface: String) {
        shell.execute("iptables -t nat -I PREROUTING -i $hotspotIface -p udp --dport 53 -j DNAT --to-destination 8.8.8.8:53")
        shell.execute("iptables -t nat -I PREROUTING -i $hotspotIface -p tcp --dport 53 -j DNAT --to-destination 8.8.8.8:53")
    }

    private suspend fun removeDnsRedirect(hotspotIface: String) {
        shell.execute("iptables -t nat -D PREROUTING -i $hotspotIface -p udp --dport 53 -j DNAT --to-destination 8.8.8.8:53 2>/dev/null")
        shell.execute("iptables -t nat -D PREROUTING -i $hotspotIface -p tcp --dport 53 -j DNAT --to-destination 8.8.8.8:53 2>/dev/null")
    }

    // --- Interface Detection ---
    fun detectUpstreamInterface(): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNet = cm.activeNetwork
            if (activeNet != null) {
                val lp = cm.getLinkProperties(activeNet)
                val iface = lp?.interfaceName
                if (!iface.isNullOrEmpty()) return iface
            }
        } catch (_: Exception) {}

        // Fallback: check NetworkInterfaces for cellular or active wifi
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (iface in ifaces) {
                if (iface.isUp && !iface.isLoopback) {
                    val name = iface.name.lowercase()
                    if (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") || name == "wlan0") {
                        return iface.name
                    }
                }
            }
        } catch (_: Exception) {}

        return "None"
    }

    fun detectHotspotInterface(): String {
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            val hotspotPrefixes = listOf("ap0", "wlan1", "swlan0", "wlan2", "rndis0", "bt-pan")
            for (prefix in hotspotPrefixes) {
                for (iface in ifaces) {
                    if (iface.name.lowercase().startsWith(prefix) && iface.isUp) {
                        return iface.name
                    }
                }
            }
            // Even if not up yet, search all existing interfaces
            for (iface in ifaces) {
                val name = iface.name.lowercase()
                if (name.contains("ap") || name.contains("swlan") || name == "wlan1") {
                    return iface.name
                }
            }
        } catch (_: Exception) {}

        return "wlan1"
    }
}
