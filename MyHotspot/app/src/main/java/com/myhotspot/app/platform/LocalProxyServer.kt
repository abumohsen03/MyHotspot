package com.myhotspot.app.platform

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance embedded HTTP & HTTPS CONNECT Proxy Server.
 *
 * Runs locally on the phone (listening on 0.0.0.0:8282).
 * When connected clients route their traffic through this proxy, all requests
 * are originated directly by the phone itself.
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
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress("0.0.0.0", port))
                serverSocket = server
                _isRunning.value = true
                Log.d(tag, "Proxy server bound to 0.0.0.0:$port")

                while (_isRunning.value) {
                    val clientSocket = server.accept() ?: break
                    val clientIp = clientSocket.inetAddress?.hostAddress ?: "Unknown"
                    connectedClientIps.add(clientIp)

                    launch(Dispatchers.IO) {
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
            val clientIn = BufferedInputStream(clientSocket.getInputStream())
            val clientOut = clientSocket.getOutputStream()

            val header = readHeader(clientIn)
            if (header.isEmpty()) {
                clientSocket.close()
                return
            }

            val firstLine = header.lines().firstOrNull()?.trim() ?: ""
            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                clientSocket.close()
                return
            }

            val method = parts[0].uppercase()
            val target = parts[1]

            if (method == "CONNECT") {
                val targetParts = target.split(":")
                val host = targetParts[0]
                val port = if (targetParts.size > 1) targetParts[1].toIntOrNull() ?: 443 else 443

                val remoteSocket = Socket()
                remoteSocket.connect(InetSocketAddress(host, port), 15000)
                remoteSocket.soTimeout = 30000

                val response = "HTTP/1.1 200 Connection Established\r\n\r\n"
                clientOut.write(response.toByteArray(Charsets.ISO_8859_1))
                clientOut.flush()

                pipeSockets(clientSocket, remoteSocket)
            } else {
                val uri = if (target.startsWith("http://", ignoreCase = true)) {
                    target.substring(7)
                } else {
                    target
                }
                val hostPort = uri.substringBefore("/")
                val host = hostPort.substringBefore(":")
                val port = if (hostPort.contains(":")) hostPort.substringAfter(":").toIntOrNull() ?: 80 else 80

                val remoteSocket = Socket()
                remoteSocket.connect(InetSocketAddress(host, port), 15000)
                remoteSocket.soTimeout = 30000

                val remoteOut = remoteSocket.getOutputStream()
                remoteOut.write(header.toByteArray(Charsets.ISO_8859_1))
                remoteOut.flush()

                pipeSockets(clientSocket, remoteSocket)
            }
        } catch (_: Exception) {
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun readHeader(inputStream: InputStream): String {
        val baos = ByteArrayOutputStream()
        var matched = 0

        while (true) {
            val b = inputStream.read()
            if (b == -1) break
            baos.write(b)

            if ((matched == 0 || matched == 2) && b == '\r'.code) {
                matched++
            } else if ((matched == 1 || matched == 3) && b == '\n'.code) {
                matched++
                if (matched == 4) break
            } else if (b == '\n'.code) {
                if (matched >= 2) break
                matched = 2
            } else {
                matched = 0
            }

            if (baos.size() > 16384) break
        }
        return baos.toString(Charsets.ISO_8859_1.name())
    }

    private fun pipeSockets(sockA: Socket, sockB: Socket) {
        val inA = sockA.getInputStream()
        val outA = sockB.getOutputStream()
        val inB = sockB.getInputStream()
        val outB = sockA.getOutputStream()

        val threadA = Thread {
            try {
                copyStream(inA, outA)
            } catch (_: Exception) {
            } finally {
                try { sockB.shutdownOutput() } catch (_: Exception) {}
                try { sockA.close() } catch (_: Exception) {}
            }
        }

        val threadB = Thread {
            try {
                copyStream(inB, outB)
            } catch (_: Exception) {
            } finally {
                try { sockA.shutdownOutput() } catch (_: Exception) {}
                try { sockB.close() } catch (_: Exception) {}
            }
        }

        threadA.isDaemon = true
        threadB.isDaemon = true
        threadA.start()
        threadB.start()

        try { threadA.join() } catch (_: Exception) {}
        try { threadB.join() } catch (_: Exception) {}
        try { sockA.close() } catch (_: Exception) {}
        try { sockB.close() } catch (_: Exception) {}
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead <= 0) break
            output.write(buffer, 0, bytesRead)
            output.flush()
            _totalBytesTransferred.value += bytesRead
        }
    }
}
