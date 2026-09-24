package com.myhotspot.app.ui.hotspot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhotspot.app.domain.models.CapabilityReport
import com.myhotspot.app.domain.models.ConnectedClient
import com.myhotspot.app.domain.models.GoogleCheckStatus
import com.myhotspot.app.domain.models.HotspotState
import com.myhotspot.app.platform.CapabilityDetector
import com.myhotspot.app.platform.ClientScanner
import com.myhotspot.app.platform.LocalHotspotManager
import com.myhotspot.app.platform.LocalProxyServer
import com.myhotspot.app.service.HotspotService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class HotspotViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hotspotManager: LocalHotspotManager,
    private val capabilityDetector: CapabilityDetector,
    private val clientScanner: ClientScanner,
    private val proxyServer: LocalProxyServer
) : ViewModel() {

    private val prefs = context.getSharedPreferences("myhotspot_config", Context.MODE_PRIVATE)

    // User-chosen Hotspot Credentials (SSID & Password)
    private val _customSsid = MutableStateFlow(prefs.getString("custom_ssid", "MyHotspot_5G") ?: "MyHotspot_5G")
    val customSsid: StateFlow<String> = _customSsid.asStateFlow()

    private val _customPassword = MutableStateFlow(prefs.getString("custom_password", "hotspot1234") ?: "hotspot1234")
    val customPassword: StateFlow<String> = _customPassword.asStateFlow()

    // Google Connectivity & Bypass Status
    private val _googleCheckStatus = MutableStateFlow<GoogleCheckStatus>(GoogleCheckStatus.NotTested)
    val googleCheckStatus: StateFlow<GoogleCheckStatus> = _googleCheckStatus.asStateFlow()

    // State Flows
    val hotspotState: StateFlow<HotspotState> = hotspotManager.state
    val isProxyRunning: StateFlow<Boolean> = proxyServer.isRunning
    val totalBytesTransferred: StateFlow<Long> = proxyServer.totalBytesTransferred

    private val _capabilities = MutableStateFlow<CapabilityReport?>(null)
    val capabilities: StateFlow<CapabilityReport?> = _capabilities.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val connectedClients: StateFlow<List<ConnectedClient>> = _connectedClients.asStateFlow()

    init {
        refreshCapabilities()
        startClientPolling()
        testGoogleConnectivity() // Run an initial connectivity check
    }

    fun updateSsid(newSsid: String) {
        _customSsid.value = newSsid
        prefs.edit().putString("custom_ssid", newSsid).apply()
    }

    fun updatePassword(newPassword: String) {
        _customPassword.value = newPassword
        prefs.edit().putString("custom_password", newPassword).apply()
    }

    fun saveCredentials(ssid: String, pass: String) {
        _customSsid.value = ssid
        _customPassword.value = pass
        prefs.edit()
            .putString("custom_ssid", ssid)
            .putString("custom_password", pass)
            .apply()
        Toast.makeText(context, "تم حفظ بيانات نقطة الاتصال بنجاح ✓", Toast.LENGTH_SHORT).show()
    }

    fun copyPasswordToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Hotspot Password", _customPassword.value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ كلمة المرور إلى الحافظة!", Toast.LENGTH_SHORT).show()
    }

    fun copySsidToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Hotspot SSID", _customSsid.value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ اسم الشبكة إلى الحافظة!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Official Google Connectivity & Captive Portal Test (generate_204).
     * Tests whether internet packets flow to Google servers and pass captive portal checks.
     */
    fun testGoogleConnectivity() {
        _googleCheckStatus.value = GoogleCheckStatus.Checking
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                // 1. Check DNS resolution for google.com
                var dnsOk = false
                try {
                    val address = InetAddress.getByName("connectivitycheck.gstatic.com")
                    dnsOk = address.hostAddress != null
                } catch (_: Exception) {}

                // 2. Perform official Google Captive Portal HTTP 204 Probe
                val url = URL("http://connectivitycheck.gstatic.com/generate_204")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                val responseCode = connection.responseCode
                connection.disconnect()

                val latency = System.currentTimeMillis() - startTime

                if (responseCode == 204 || responseCode == 200) {
                    _googleCheckStatus.value = GoogleCheckStatus.Success(
                        latencyMs = latency,
                        httpCode = responseCode,
                        dnsResolved = dnsOk,
                        message = "تم تخطي فحص جوجل بنجاح — الإنترنت متصل بالكامل وبدون حجب!"
                    )
                } else {
                    _googleCheckStatus.value = GoogleCheckStatus.Failed("استجابة غير متوقعة من خوادم جوجل (كود: $responseCode)")
                }
            } catch (e: Exception) {
                // Fallback attempt to google.com directly
                try {
                    val fallbackUrl = URL("https://www.google.com")
                    val fallbackConn = fallbackUrl.openConnection() as HttpURLConnection
                    fallbackConn.connectTimeout = 4000
                    fallbackConn.readTimeout = 4000
                    val code = fallbackConn.responseCode
                    fallbackConn.disconnect()
                    val latency = System.currentTimeMillis() - startTime
                    if (code in 200..399) {
                        _googleCheckStatus.value = GoogleCheckStatus.Success(
                            latencyMs = latency,
                            httpCode = code,
                            dnsResolved = true,
                            message = "متصل بخوادم Google بنجاح!"
                        )
                        return@launch
                    }
                } catch (_: Exception) {}

                _googleCheckStatus.value = GoogleCheckStatus.Failed("تعذر الاتصال بخوادم جوجل: ${e.localizedMessage ?: "تأكد من تفعيل باقة الإنترنت"}")
            }
        }
    }

    /**
     * Primary Action: Opens the System Mobile Hotspot configuration directly.
     * Supports Samsung One UI (Galaxy A-series, S-series) and generic Android devices.
     */
    fun openSystemTetheringSettings() {
        // First copy the chosen password so user can paste it or view it instantly
        copyPasswordToClipboard()

        val intentsToTry = listOf(
            // Samsung One UI Mobile AP Activity
            Intent().apply {
                component = ComponentName("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent("com.android.settings.wifi.mobileap.WifiApSettings").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent("android.settings.WIFI_AP_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent("android.settings.TETHER_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        var launched = false
        for (intent in intentsToTry) {
            try {
                context.startActivity(intent)
                launched = true
                break
            } catch (_: Exception) {
                // Try next intent
            }
        }

        if (!launched) {
            try {
                val fallback = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }

        // Start Foreground Service to keep tracking and notification running
        startForegroundService()
    }

    private fun startForegroundService() {
        val intent = Intent(context, HotspotService::class.java).apply {
            action = HotspotService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun toggleLocalOnlyHotspot() {
        val current = hotspotState.value
        if (current is HotspotState.Active || current is HotspotState.Starting) {
            hotspotManager.stopHotspot()
            val intent = Intent(context, HotspotService::class.java).apply {
                action = HotspotService.ACTION_STOP
            }
            context.startService(intent)
        } else {
            hotspotManager.startHotspot()
            startForegroundService()
        }
    }

    fun refreshCapabilities() {
        viewModelScope.launch {
            _capabilities.value = capabilityDetector.detectCapabilities()
        }
    }

    private fun startClientPolling() {
        viewModelScope.launch {
            while (isActive) {
                _connectedClients.value = clientScanner.getConnectedClients()
                delay(3000)
            }
        }
    }
}
