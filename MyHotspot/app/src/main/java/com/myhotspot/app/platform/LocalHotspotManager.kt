package com.myhotspot.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.myhotspot.app.domain.models.HotspotState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHotspotManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyServer: LocalProxyServer
) {
    private val tag = "LocalHotspotManager"
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    private val _state = MutableStateFlow<HotspotState>(HotspotState.Idle)
    val state: StateFlow<HotspotState> = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startHotspot() {
        if (_state.value is HotspotState.Active || _state.value is HotspotState.Starting) {
            return
        }

        _state.value = HotspotState.Starting

        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation?) {
                    super.onStarted(res)
                    reservation = res
                    if (res == null) {
                        _state.value = HotspotState.Error("Hotspot reservation returned null")
                        return
                    }

                    var ssid = "MyHotspot"
                    var passphrase = ""

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = res.softApConfiguration
                        if (config != null) {
                            ssid = config.ssid ?: ssid
                            passphrase = config.passphrase ?: ""
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val config = res.wifiConfiguration
                        if (config != null) {
                            ssid = config.SSID ?: ssid
                            passphrase = config.preSharedKey ?: ""
                        }
                    }

                    // Clean quotes if present
                    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length - 1)
                    }
                    if (passphrase.startsWith("\"") && passphrase.endsWith("\"")) {
                        passphrase = passphrase.substring(1, passphrase.length - 1)
                    }

                    val localIp = getHotspotIpAddress()

                    // Automatically start bypass proxy server
                    proxyServer.start(8282)

                    _state.value = HotspotState.Active(
                        ssid = ssid,
                        passphrase = passphrase,
                        ipAddress = localIp,
                        proxyPort = 8282
                    )
                    Log.d(tag, "Hotspot active: SSID=$ssid, IP=$localIp, Proxy=8282")
                }

                override fun onStopped() {
                    super.onStopped()
                    reservation = null
                    proxyServer.stop()
                    _state.value = HotspotState.Idle
                    Log.d(tag, "Hotspot stopped")
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    reservation = null
                    proxyServer.stop()
                    val errorMessage = when (reason) {
                        1 -> "Generic internal error"
                        2 -> "No channel available"
                        3 -> "LocalOnlyHotspot is unsupported on this device"
                        4 -> "Incompatible mode (Wi-Fi busy)"
                        5 -> "Tethering disallowed by system policy"
                        else -> "Failed to start hotspot (Code: $reason)"
                    }
                    _state.value = HotspotState.Error(errorMessage)
                    Log.e(tag, "Hotspot failed: $errorMessage")
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            _state.value = HotspotState.Error("Permissions missing: ${e.message}")
        } catch (e: Exception) {
            _state.value = HotspotState.Error("Failed: ${e.message}")
        }
    }

    fun stopHotspot() {
        try {
            reservation?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing hotspot: ${e.message}")
        }
        reservation = null
        proxyServer.stop()
        _state.value = HotspotState.Idle
    }

    fun getHotspotIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

            // 1. First priority: interfaces dedicated to SoftAP / Hotspot (swlan, ap, softap, p2p)
            for (iface in interfaces) {
                val name = iface.name.lowercase()
                if (name.startsWith("swlan") || name.startsWith("ap") || name.startsWith("softap") || name.startsWith("p2p") || name.contains("wigig")) {
                    for (addr in iface.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotEmpty() && !host.startsWith("127.")) {
                                return host
                            }
                        }
                    }
                }
            }

            // 2. Second priority: Standard Android hotspot subnets (192.168.49.x for LocalOnlyHotspot or 192.168.43.x)
            for (iface in interfaces) {
                for (addr in iface.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.49.") || host.startsWith("192.168.43.")) {
                            return host
                        }
                    }
                }
            }

            // 3. Third priority: Any 192.168.x.x interface that is not wlan0 (client)
            for (iface in interfaces) {
                val name = iface.name.lowercase()
                if (!name.startsWith("wlan0") && !name.startsWith("rmnet") && !name.startsWith("ccmni")) {
                    for (addr in iface.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: ""
                            if (host.startsWith("192.168.")) return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 192.168.49.1 is the universal LocalOnlyHotspot default gateway on Android (Samsung, Pixel, Xiaomi, etc.)
        return "192.168.49.1"
    }
}
