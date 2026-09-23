package com.myhotspot.app.ui.hotspot

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhotspot.app.domain.models.ConnectedClient
import com.myhotspot.app.domain.models.HotspotState
import com.myhotspot.app.ui.theme.Primary
import com.myhotspot.app.ui.theme.Success
import com.myhotspot.app.utils.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotDashboardScreen(
    viewModel: HotspotViewModel,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val hotspotState by viewModel.hotspotState.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()
    val clients by viewModel.connectedClients.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "تم نسخ $label بنجاح", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MyHotspot",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "بث فوري وتلقائي للإنترنت • مشاركة سريعة بدون تعقيد",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Status & Hero Toggle Section ---
            HeroHotspotToggle(
                state = hotspotState,
                onToggle = {
                    onRequestPermissions()
                    viewModel.toggleHotspot()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Direct Internet Hotspot Quick Action Button ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بث الإنترنت التلقائي المباشر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary
                        ) {
                            Text(
                                text = "تلقائي بدون وكيل",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "لبث الإنترنت فوراً لجميع الأجهزة (كمبيوتر، آيفون، شاشات) بدون أي شروط وبدون إدخال أي بروكسي يدوي، اضغط الزر أدناه لتفعيل نقطة الاتصال المباشرة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.openSystemTetheringSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تشغيل بث الإنترنت المباشر الآن 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // --- Active Hotspot Credentials Card ---
            AnimatedVisibility(visible = hotspotState is HotspotState.Active) {
                val active = hotspotState as? HotspotState.Active
                if (active != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Success
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "بيانات الشبكة الحالية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showQrDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رمز QR")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Network SSID Row
                            CredentialRow(
                                label = "اسم نقطة الاتصال (Network Name)",
                                value = active.ssid,
                                onCopy = { copyToClipboard("اسم الشبكة", active.ssid) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password Row
                            CredentialRow(
                                label = "كلمة المرور (Password)",
                                value = active.passphrase.ifEmpty { "(شبكة مفتوحة بدون كلمة مرور)" },
                                onCopy = { copyToClipboard("كلمة المرور", active.passphrase) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الشبكة جاهزة للبث والاتصال التلقائي",
                                    fontSize = 13.sp,
                                    color = Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // --- Connected Clients Section ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الأجهزة المتصلة (${clients.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (clients.isEmpty()) {
                        Text(
                            text = if (hotspotState is HotspotState.Active)
                                "لا توجد أجهزة متصلة حالياً. بانتظار اتصال الأجهزة..."
                            else
                                "قم بتشغيل نقطة الاتصال لمراقبة الأجهزة المتصلة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        clients.forEach { client ->
                            ClientItem(client = client)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // --- Hardware & Diagnostics Section ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تشخيص قدرات عتاد الهاتف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    capabilities?.let { cap ->
                        DiagnosticItem(
                            title = "دعم تردد 5GHz المزدوج",
                            isSupported = cap.is5GHzSupported
                        )
                        DiagnosticItem(
                            title = "دعم Wi-Fi Direct (P2P)",
                            isSupported = cap.isWifiDirectSupported
                        )
                        DiagnosticItem(
                            title = "دعم بث نقطة اتصال محلية",
                            isSupported = cap.isLocalOnlyHotspotSupported
                        )
                        DiagnosticItem(
                            title = "دعم مشاركة الواي فاي (STA/AP Concurrency)",
                            isSupported = cap.isStaApConcurrencySupported
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النظام: ${cap.androidVersion} • ${cap.deviceModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // --- QR Code Dialog ---
    if (showQrDialog && hotspotState is HotspotState.Active) {
        val active = hotspotState as HotspotState.Active
        val qrBitmap = remember(active.ssid, active.passphrase) {
            QrCodeGenerator.generateWifiQrCode(active.ssid, active.passphrase, 600)
        }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = "الاتصال الفوري عبر رمز QR",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "امسح الرمز بكاميرا أي هاتف (أندرويد أو آيفون) للاتصال بالشبكة مباشرة دون كتابة كلمة المرور:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Wi-Fi QR Code",
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "الشبكة: ${active.ssid}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showQrDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun HeroHotspotToggle(
    state: HotspotState,
    onToggle: () -> Unit
) {
    val isActive = state is HotspotState.Active
    val isStarting = state is HotspotState.Starting

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Success.copy(alpha = 0.2f))
                )
            }

            Surface(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isStarting, onClick = onToggle),
                color = when {
                    isActive -> Success
                    isStarting -> Primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isStarting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(46.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle Hotspot",
                            tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when (state) {
                is HotspotState.Active -> "نقطة الاتصال نشطة ومحمية"
                is HotspotState.Starting -> "جاري بدء تشغيل البث..."
                is HotspotState.Error -> "حدث خطأ: ${state.message}"
                HotspotState.Idle -> "نقطة الاتصال متوقفة"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = when (state) {
                is HotspotState.Active -> Success
                is HotspotState.Error -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isActive) "اضغط لإيقاف البث" else "اضغط هنا لبدء البث الفوري للإنترنت",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CredentialRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ClientItem(client: ConnectedClient) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Success)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = client.ipAddress,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (client.macAddress.isNotEmpty() && client.macAddress != "N/A") {
                    Text(
                        text = "MAC: ${client.macAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    title: String,
    isSupported: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (isSupported) "مدعوم ✓" else "غير متوفر ✕",
            fontWeight = FontWeight.Bold,
            color = if (isSupported) Success else Color.Gray,
            fontSize = 13.sp
        )
    }
}
