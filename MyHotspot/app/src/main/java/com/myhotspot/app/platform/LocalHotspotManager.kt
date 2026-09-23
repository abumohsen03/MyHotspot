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
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val name = iface.name.lowercase()
                // Typical hotspot/AP interface names on Android: wlan0, wlan1, ap0, swlan0, rndis0
                if (name.contains("ap") || name.contains("wlan") || name.contains("hotspot")) {
                    val addrs = iface.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress ?: ""
                            if (ip.isNotEmpty()) return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.43.1"
    }
}
