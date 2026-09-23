package com.myhotspot.app.platform

import com.myhotspot.app.domain.models.ConnectedClient
import java.io.BufferedReader
import java.io.FileReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientScanner @Inject constructor(
    private val proxyServer: LocalProxyServer
) {
    fun getConnectedClients(): List<ConnectedClient> {
        val clients = mutableListOf<ConnectedClient>()
        val detectedIps = mutableSetOf<String>()

        // 1. Scan ARP cache if accessible
        try {
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            reader.readLine() // Skip header
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.trim().split("\\s+".toRegex())
                if (tokens.size >= 4) {
                    val ip = tokens[0]
                    val flags = tokens[2]
                    val mac = tokens[3]

                    // Flags 0x2 means reachable/valid ARP entry
                    if (flags != "0x0" && mac != "00:00:00:00:00:00" && !detectedIps.contains(ip)) {
                        detectedIps.add(ip)
                        clients.add(
                            ConnectedClient(
                                ipAddress = ip,
                                macAddress = mac,
                                deviceName = "Connected Device",
                                isBypassActive = proxyServer.connectedClientIps.contains(ip)
                            )
                        )
                    }
                }
            }
            reader.close()
        } catch (_: Exception) {
            // Android 10+ restricts direct /proc/net/arp access for non-system apps
        }

        // 2. Add any clients communicating via the bypass proxy
        for (proxyIp in proxyServer.connectedClientIps) {
            if (!detectedIps.contains(proxyIp)) {
                detectedIps.add(proxyIp)
                clients.add(
                    ConnectedClient(
                        ipAddress = proxyIp,
                        macAddress = "N/A",
                        deviceName = "Device (via Bypass Proxy)",
                        isBypassActive = true
                    )
                )
            }
        }

        return clients
    }
}
