package com.myhotspot.app.domain.models

sealed interface GoogleCheckStatus {
    data object NotTested : GoogleCheckStatus
    data object Checking : GoogleCheckStatus
    data class Success(
        val latencyMs: Long,
        val httpCode: Int = 204,
        val dnsResolved: Boolean = true,
        val message: String = "تم اجتياز فحص اتصال جوجل بنجاح (Generate 204 OK)"
    ) : GoogleCheckStatus
    data class Failed(
        val errorMessage: String
    ) : GoogleCheckStatus
}
