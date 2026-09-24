package com.myhotspot.app.ui.hotspot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhotspot.app.domain.models.CommandLog
import com.myhotspot.app.domain.models.ConnectedDevice
import com.myhotspot.app.domain.models.DiagnosticItem
import com.myhotspot.app.domain.models.TtlEngineState
import com.myhotspot.app.domain.models.TtlSettings
import com.myhotspot.app.platform.root.CompatibilityChecker
import com.myhotspot.app.platform.root.MagiskScriptExporter
import com.myhotspot.app.platform.root.RootClientScanner
import com.myhotspot.app.platform.root.RootShellManager
import com.myhotspot.app.platform.root.TtlEngine
import com.myhotspot.app.service.TtlHotspotService
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
import javax.inject.Inject

@HiltViewModel
class TtlViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttlEngine: TtlEngine,
    private val shellManager: RootShellManager,
    private val compatibilityChecker: CompatibilityChecker,
    private val clientScanner: RootClientScanner,
    private val magiskExporter: MagiskScriptExporter
) : ViewModel() {

    private val prefs = context.getSharedPreferences("ttl_hotspot_settings", Context.MODE_PRIVATE)

    val engineState: StateFlow<TtlEngineState> = ttlEngine.engineState
    val commandLogs: StateFlow<List<CommandLog>> = shellManager.logs

    private val _diagnostics = MutableStateFlow<List<DiagnosticItem>>(emptyList())
    val diagnostics: StateFlow<List<DiagnosticItem>> = _diagnostics.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    private val _clients = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val clients: StateFlow<List<ConnectedDevice>> = _clients.asStateFlow()

    private val _settings = MutableStateFlow(
        TtlSettings(
            ttlValue = prefs.getInt("ttl_value", 65),
            blockIpv6 = prefs.getBoolean("block_ipv6", false),
            redirectDns = prefs.getBoolean("redirect_dns", false),
            autoStartOnHotspot = prefs.getBoolean("auto_start", true),
            startOnBoot = prefs.getBoolean("start_on_boot", false)
        )
    )
    val settings: StateFlow<TtlSettings> = _settings.asStateFlow()

    init {
        runDiagnostics()
        startClientPolling()
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _isDiagnosing.value = true
            _diagnostics.value = compatibilityChecker.runDiagnostics()
            _isDiagnosing.value = false
        }
    }

    fun toggleEngine() {
        if (engineState.value.isRunning) {
            stopEngine()
        } else {
            startEngine()
        }
    }

    fun startEngine() {
        viewModelScope.launch {
            val s = _settings.value
            val success = ttlEngine.start(s)
            if (success) {
                startForegroundService(s)
                Toast.makeText(context, "تم تفعيل حزم الـ TTL ($s.ttlValue) بنجاح 🚀", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "فشل تفعيل الـ TTL! تأكد من منح صلاحيات الروت (Magisk)", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun stopEngine() {
        viewModelScope.launch {
            ttlEngine.stop(_settings.value)
            stopForegroundService()
            Toast.makeText(context, "تم إيقاف قواعد الـ TTL واستعادة الافتراضي", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startForegroundService(s: TtlSettings) {
        val intent = Intent(context, TtlHotspotService::class.java).apply {
            action = TtlHotspotService.ACTION_START
            putExtra(TtlHotspotService.EXTRA_TTL, s.ttlValue)
            putExtra(TtlHotspotService.EXTRA_BLOCK_IPV6, s.blockIpv6)
            putExtra(TtlHotspotService.EXTRA_REDIRECT_DNS, s.redirectDns)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(context, TtlHotspotService::class.java).apply {
            action = TtlHotspotService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun updateTtlValue(value: Int) {
        val newSettings = _settings.value.copy(ttlValue = value)
        _settings.value = newSettings
        prefs.edit().putInt("ttl_value", value).apply()
        if (engineState.value.isRunning) {
            startEngine() // Reapply with new TTL
        }
    }

    fun updateBlockIpv6(enabled: Boolean) {
        val newSettings = _settings.value.copy(blockIpv6 = enabled)
        _settings.value = newSettings
        prefs.edit().putBoolean("block_ipv6", enabled).apply()
    }

    fun updateRedirectDns(enabled: Boolean) {
        val newSettings = _settings.value.copy(redirectDns = enabled)
        _settings.value = newSettings
        prefs.edit().putBoolean("redirect_dns", enabled).apply()
    }

    fun updateAutoStart(enabled: Boolean) {
        val newSettings = _settings.value.copy(autoStartOnHotspot = enabled)
        _settings.value = newSettings
        prefs.edit().putBoolean("auto_start", enabled).apply()
    }

    fun updateStartOnBoot(enabled: Boolean) {
        val newSettings = _settings.value.copy(startOnBoot = enabled)
        _settings.value = newSettings
        prefs.edit().putBoolean("start_on_boot", enabled).apply()
    }

    fun installMagiskBootScript() {
        viewModelScope.launch {
            val s = _settings.value
            val success = magiskExporter.installToMagiskServiceD(s.ttlValue, s.blockIpv6, s.redirectDns)
            if (success) {
                Toast.makeText(context, "تم تثبيت سكريبت Magisk في /data/adb/service.d بنجاح!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "فشل تثبيت السكريبت، تأكد من صلاحيات الروت", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportMagiskScriptToDownloads() {
        viewModelScope.launch {
            val s = _settings.value
            val path = magiskExporter.exportToDownloads(s.ttlValue, s.blockIpv6, s.redirectDns)
            if (path != null) {
                Toast.makeText(context, "تم تصدير السكريبت إلى: $path", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "تعذر التصدير إلى التنزيلات", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun tryStartHotspotRoot() {
        viewModelScope.launch {
            // Try root commands to enable softap
            val r1 = shellManager.execute("cmd wifi start-softap")
            if (!r1.isSuccess) {
                shellManager.execute("service call connectivity 33 i32 1")
            }
            openSystemHotspotSettings()
        }
    }

    fun openSystemHotspotSettings() {
        val intents = listOf(
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
        for (intent in intents) {
            try {
                context.startActivity(intent)
                break
            } catch (_: Exception) {}
        }
    }

    private fun startClientPolling() {
        viewModelScope.launch {
            while (isActive) {
                val iface = engineState.value.hotspotInterface
                _clients.value = clientScanner.getConnectedDevices(iface)
                delay(3000)
            }
        }
    }
}
