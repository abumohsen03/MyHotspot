package com.myhotspot.app.platform

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded HTTP & HTTPS CONNECT Proxy Server.
 *
 * Runs locally on the phone (default port 8282).
 * When connected clients route their traffic through this proxy, all requests
 * are originated directly by the phone itself.
 *
 * This effectively BYPASSES:
 * 1. Carrier tethering/hotspot blocking and throttling.
 * 2. Carrier APN restriction (TTL remains untouched).
 * 3. Wi-Fi hotspot sharing restrictions (enables Wi-Fi repeater mode).
 */
@Singleton
class LocalProxyServer @Inject constructor() {

    private val tag = "LocalProxyServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _totalBytesTransferred = MutableStateFlow(0L)
    val totalBytesTransferred: StateFlow<Long> = _totalBytesTransferred.asStateFlow()

    val connectedClientIps = ConcurrentHashMap.newKeySet<String>()

    fun start(port: Int = 8282) {
        if (_isRunning.value) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _isRunning.value = true
                Log.d(tag, "Proxy server started on port $port")

                while (_isRunning.value) {
                    val clientSocket = serverSocket?.accept() ?: break
                    val clientIp = clientSocket.inetAddress?.hostAddress ?: "Unknown"
                    connectedClientIps.add(clientIp)

                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (_isRunning.value) {
                    Log.e(tag, "Proxy server error: ${e.message}", e)
                }
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        connectedClientIps.clear()
        Log.d(tag, "Proxy server stopped")
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 30000
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()

            val header = readHeader(clientIn)
            if (header.isEmpty()) {
                clientSocket.close()
                return
            }

            val firstLine = header.lines().firstOrNull() ?: ""
            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                clientSocket.close()
                return
            }

            val method = parts[0].uppercase()
            val target = parts[1]

            if (method == "CONNECT") {
                // HTTPS CONNECT Tunneling
                val targetParts = target.split(":")
                val host = targetParts[0]
                val port = if (targetParts.size > 1) targetParts[1].toIntOrNull() ?: 443 else 443

                val remoteSocket = Socket(host, port)
                remoteSocket.soTimeout = 30000

                // Acknowledge connection to client
                val response = "HTTP/1.1 200 Connection Established\r\n\r\n"
                clientOut.write(response.toByteArray(Charsets.ISO_8859_1))
                clientOut.flush()

                // Bidirectional pipe
                pipeSockets(clientSocket, remoteSocket)
            } else {
                // Standard HTTP Proxying
                val uri = if (target.startsWith("http://", ignoreCase = true)) {
                    target.substring(7)
                } else {
                    target
                }
                val hostPort = uri.substringBefore("/")
                val host = hostPort.substringBefore(":")
                val port = if (hostPort.contains(":")) hostPort.substringAfter(":").toIntOrNull() ?: 80 else 80

                val remoteSocket = Socket(host, port)
                val remoteOut = remoteSocket.getOutputStream()
                val remoteIn = remoteSocket.getInputStream()

                // Forward request headers
                remoteOut.write(header.toByteArray(Charsets.ISO_8859_1))
                remoteOut.flush()

                // Forward response back to client
                pipeSockets(clientSocket, remoteSocket)
            }
        } catch (e: Exception) {
            // Expected socket disconnects
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun readHeader(inputStream: InputStream): String {
        val buffer = StringBuilder()
        val byteBuf = ByteArray(1)
        while (true) {
            val read = inputStream.read(byteBuf)
            if (read == -1) break
            buffer.append(byteBuf[0].toInt().toChar())
            if (buffer.endsWith("\r\n\r\n") || buffer.endsWith("\n\n")) {
                break
            }
            if (buffer.length > 8192) break // Prevent header overflow
        }
        return buffer.toString()
    }

    private fun pipeSockets(sockA: Socket, sockB: Socket) {
        val inA = sockA.getInputStream()
        val outA = sockA.getOutputStream()
        val inB = sockB.getInputStream()
        val outB = sockB.getOutputStream()

        val threadA = Thread {
            copyStream(inA, outB)
            try { sockB.shutdownOutput() } catch (_: Exception) {}
        }
        val threadB = Thread {
            copyStream(inB, outA)
            try { sockA.shutdownOutput() } catch (_: Exception) {}
        }

        threadA.start()
        threadB.start()

        try { threadA.join() } catch (_: Exception) {}
        try { threadB.join() } catch (_: Exception) {}
        try { sockA.close() } catch (_: Exception) {}
        try { sockB.close() } catch (_: Exception) {}
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        try {
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
                output.flush()
                _totalBytesTransferred.value += bytesRead
            }
        } catch (_: Exception) {}
    }
}
