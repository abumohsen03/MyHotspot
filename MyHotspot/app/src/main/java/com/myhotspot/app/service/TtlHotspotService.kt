package com.myhotspot.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.myhotspot.app.MainActivity
import com.myhotspot.app.domain.models.TtlSettings
import com.myhotspot.app.platform.root.TtlEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TtlHotspotService : Service() {

    @Inject
    lateinit var ttlEngine: TtlEngine

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TTLHotspot::ServiceWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    ttlEngine.stop(TtlSettings())
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    wakeLock?.let { if (it.isHeld) it.release() }
                    stopSelf()
                }
            }
            ACTION_START -> {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max
                val ttl = intent.getIntExtra(EXTRA_TTL, 65)
                val blockIpv6 = intent.getBooleanExtra(EXTRA_BLOCK_IPV6, false)
                val redirectDns = intent.getBooleanExtra(EXTRA_REDIRECT_DNS, false)

                val settings = TtlSettings(ttlValue = ttl, blockIpv6 = blockIpv6, redirectDns = redirectDns)

                startForeground(NOTIFICATION_ID, createNotification("TTL Hotspot نشط (TTL = $ttl)"))

                scope.launch {
                    ttlEngine.start(settings)
                    startPeriodicRuleVerification(settings)
                }
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification("خدمة TTL Hotspot قيد العمل"))
            }
        }
        return START_STICKY
    }

    private fun startPeriodicRuleVerification(settings: TtlSettings) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(15000)
                // Periodically verify if interfaces changed or rules need reapplication
                if (ttlEngine.engineState.value.isRunning) {
                    val currentUpstream = ttlEngine.detectUpstreamInterface()
                    if (currentUpstream != ttlEngine.engineState.value.upstreamInterface && currentUpstream != "None") {
                        ttlEngine.start(settings)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TTL Hotspot Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تحافظ على استمرار تعديل حزم الـ TTL وتخطي قيود الشبكة"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TtlHotspotService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TTL Hotspot Pro 🛡️")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "ttl_hotspot_service_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.myhotspot.app.service.TTL_START"
        const val ACTION_STOP = "com.myhotspot.app.service.TTL_STOP"
        const val EXTRA_TTL = "extra_ttl"
        const val EXTRA_BLOCK_IPV6 = "extra_block_ipv6"
        const val EXTRA_REDIRECT_DNS = "extra_redirect_dns"
    }
}
