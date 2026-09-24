package com.myhotspot.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("ttl_hotspot_settings", Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean("start_on_boot", false)

            if (startOnBoot) {
                val ttl = prefs.getInt("ttl_value", 65)
                val blockIpv6 = prefs.getBoolean("block_ipv6", false)
                val redirectDns = prefs.getBoolean("redirect_dns", false)

                val serviceIntent = Intent(context, TtlHotspotService::class.java).apply {
                    action = TtlHotspotService.ACTION_START
                    putExtra(TtlHotspotService.EXTRA_TTL, ttl)
                    putExtra(TtlHotspotService.EXTRA_BLOCK_IPV6, blockIpv6)
                    putExtra(TtlHotspotService.EXTRA_REDIRECT_DNS, redirectDns)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
