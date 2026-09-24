package com.myhotspot.app.platform.root

import android.util.Log
import com.myhotspot.app.domain.models.CommandLog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootShellManager @Inject constructor() {

    private val tag = "RootShellManager"
    private val mutex = Mutex()

    private val _logs = MutableStateFlow<List<CommandLog>>(emptyList())
    val logs: StateFlow<List<CommandLog>> = _logs.asStateFlow()

    init {
        // Configure libsu
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_MOUNT_MASTER)
                .setTimeout(15)
        )
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val granted = Shell.isAppGrantedRoot()
            if (granted != null) return@withContext granted
            // Force shell creation to prompt user or test su binary
            val result = Shell.cmd("id").exec()
            result.isSuccess && result.out.any { it.contains("uid=0") }
        } catch (e: Exception) {
            Log.e(tag, "Root check failed: ${e.message}")
            false
        }
    }

    suspend fun execute(command: String): Shell.Result = withContext(Dispatchers.IO) {
        val result = Shell.cmd(command).exec()
        val outText = (result.out + result.err).joinToString("\n").trim()
        
        mutex.withLock {
            val newLog = CommandLog(
                command = command,
                exitCode = result.code,
                output = outText
            )
            val current = _logs.value.toMutableList()
            if (current.size >= 100) {
                current.removeAt(0)
            }
            current.add(newLog)
            _logs.value = current
        }

        Log.d(tag, "[$command] -> Code: ${result.code}, Out: $outText")
        result
    }

    suspend fun executeBatch(commands: List<String>): List<Shell.Result> = withContext(Dispatchers.IO) {
        commands.map { execute(it) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
