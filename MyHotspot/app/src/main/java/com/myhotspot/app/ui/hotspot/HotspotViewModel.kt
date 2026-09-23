package com.myhotspot.app.ui.hotspot

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhotspot.app.domain.models.CapabilityReport
import com.myhotspot.app.domain.models.ConnectedClient
import com.myhotspot.app.domain.models.HotspotState
import com.myhotspot.app.platform.CapabilityDetector
import com.myhotspot.app.platform.ClientScanner
import com.myhotspot.app.platform.LocalHotspotManager
import com.myhotspot.app.platform.LocalProxyServer
import com.myhotspot.app.service.HotspotService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HotspotViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hotspotManager: LocalHotspotManager,
    private val capabilityDetector: CapabilityDetector,
    private val clientScanner: ClientScanner,
    private val proxyServer: LocalProxyServer
) : ViewModel() {

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
    }

    fun toggleHotspot() {
        val current = hotspotState.value
        if (current is HotspotState.Active || current is HotspotState.Starting) {
            stopHotspot()
        } else {
            startHotspot()
        }
    }

    private fun startHotspot() {
        hotspotManager.startHotspot()

        // Start Foreground Service
        val intent = Intent(context, HotspotService::class.java).apply {
            action = HotspotService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopHotspot() {
        hotspotManager.stopHotspot()

        val intent = Intent(context, HotspotService::class.java).apply {
            action = HotspotService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun refreshCapabilities() {
        viewModelScope.launch {
            _capabilities.value = capabilityDetector.detectCapabilities()
        }
    }

    private fun startClientPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (hotspotState.value is HotspotState.Active) {
                    _connectedClients.value = clientScanner.getConnectedClients()
                } else {
                    _connectedClients.value = emptyList()
                }
                delay(3000)
            }
        }
    }

    fun openSystemTetheringSettings() {
        try {
            val intent = Intent("android.settings.TETHER_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
