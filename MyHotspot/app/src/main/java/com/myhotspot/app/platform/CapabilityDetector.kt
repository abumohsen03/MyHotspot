package com.myhotspot.app.platform

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import com.myhotspot.app.domain.models.CapabilityReport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hotspotManager: LocalHotspotManager
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun detectCapabilities(): CapabilityReport {
        val is5GHz = try {
            wifiManager.is5GHzBandSupported
        } catch (_: Exception) {
            false
        }

        val isWifiDirect = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
        val isLocalOnlySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        val isStaAp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                wifiManager.isStaApConcurrencySupported
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }

        val ip = hotspotManager.getHotspotIpAddress()

        return CapabilityReport(
            is5GHzSupported = is5GHz,
            isWifiDirectSupported = isWifiDirect,
            isLocalOnlyHotspotSupported = isLocalOnlySupported,
            isStaApConcurrencySupported = isStaAp,
            localIpAddress = ip,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }
}
