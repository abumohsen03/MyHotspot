package com.myhotspot.app.platform.root

import com.myhotspot.app.domain.models.ConnectedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootClientScanner @Inject constructor(
    private val shell: RootShellManager
) {

    suspend fun getConnectedDevices(hotspotIface: String): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<ConnectedDevice>()
        val seenIps = mutableSetOf<String>()

        if (!shell.isRootAvailable()) return@withContext emptyList()

        // 1. Run "ip neigh"
        val filterDev = if (hotspotIface != "None") "dev $hotspotIface" else ""
        val neighResult = shell.execute("ip neigh show $filterDev")
        if (neighResult.isSuccess) {
            for (line in neighResult.out) {
                // Example format: 192.168.43.105 dev swlan0 lladdr 3a:4b:5c:6d:7e:8f REACHABLE
                val tokens = line.trim().split("\\s+".toRegex())
                if (tokens.size >= 4) {
                    val ip = tokens[0]
                    val devIndex = tokens.indexOf("dev")
                    val lladdrIndex = tokens.indexOf("lladdr")
                    val iface = if (devIndex != -1 && devIndex + 1 < tokens.size) tokens[devIndex + 1] else hotspotIface
                    val mac = if (lladdrIndex != -1 && lladdrIndex + 1 < tokens.size) tokens[lladdrIndex + 1] else "Unknown"
                    val state = tokens.lastOrNull() ?: "ACTIVE"

                    if (ip.contains(".") && mac.contains(":") && !seenIps.contains(ip)) {
                        seenIps.add(ip)
                        devices.add(
                            ConnectedDevice(
                                ip = ip,
                                mac = mac,
                                hostname = resolveHostname(ip),
                                state = state,
                                interfaceName = iface
                            )
                        )
                    }
                }
            }
        }

        // 2. Fallback: Parse /proc/net/arp
        if (devices.isEmpty()) {
            val arpResult = shell.execute("cat /proc/net/arp")
            if (arpResult.isSuccess) {
                for (line in arpResult.out.drop(1)) {
                    val tokens = line.trim().split("\\s+".toRegex())
                    if (tokens.size >= 6) {
                        val ip = tokens[0]
                        val flags = tokens[2]
                        val mac = tokens[3]
                        val dev = tokens[5]

                        if (flags != "0x0" && mac != "00:00:00:00:00:00" && !seenIps.contains(ip)) {
                            seenIps.add(ip)
                            devices.add(
                                ConnectedDevice(
                                    ip = ip,
                                    mac = mac,
                                    hostname = resolveHostname(ip),
                                    state = "ACTIVE",
                                    interfaceName = dev
                                )
                            )
                        }
                    }
                }
            }
        }

        devices
    }

    private fun resolveHostname(ip: String): String {
        return when {
            ip.endsWith(".1") -> "Hotspot Gateway"
            else -> "Client (${ip.substringAfterLast('.')})"
        }
    }
}
