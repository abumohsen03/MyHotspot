package com.myhotspot.app.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object QrCodeGenerator {
    fun generateWifiQrCode(ssid: String, password: String, size: Int = 512): Bitmap? {
        if (ssid.isEmpty()) return null
        // Wi-Fi QR Code standard format: WIFI:T:WPA;S:MyNetwork;P:MyPassword;;
        val wifiPayload = "WIFI:T:WPA;S:$ssid;P:$password;;"

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            val bitMatrix = QRCodeWriter().encode(
                wifiPayload,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
