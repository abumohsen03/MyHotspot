package com.myhotspot.app.ui.hotspot

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhotspot.app.domain.models.ConnectedClient
import com.myhotspot.app.domain.models.HotspotState
import com.myhotspot.app.ui.theme.CyberBlue
import com.myhotspot.app.ui.theme.CyberCyan
import com.myhotspot.app.ui.theme.NeonGreen
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
    val totalBytes by viewModel.totalBytesTransferred.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var selectedTtlTab by remember { mutableIntStateOf(0) }

    fun copyToClipboard(label: String, text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "تم نسخ $label بنجاح", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MyHotspot Pro",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "محطة بث الإنترنت وتخطي قيود الـ TTL",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hotspotState is HotspotState.Active) Success.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (hotspotState is HotspotState.Active) Success else Color.Transparent),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (hotspotState is HotspotState.Active) NeonGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hotspotState is HotspotState.Active) "بث نشط" else "متوقف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hotspotState is HotspotState.Active) Success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

            // --- Futuristic Hero Hotspot Toggle ---
            FuturisticHeroToggle(
                state = hotspotState,
                onToggle = {
                    onRequestPermissions()
                    viewModel.toggleHotspot()
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- Primary Action: Native Direct Internet Sharing (بث الإنترنت المباشر التلقائي) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.5.dp, Primary)
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
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بث الإنترنت المباشر (Direct Hotspot)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary
                        ) {
                            Text(
                                text = "بدون بروكسي",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "لبث إنترنت الهاتف وتوزيعه مباشرة على الكمبيوتر وجميع الأجهزة تلقائياً دون أي إدخال يدوي لوكيل، اضغط الزر أدناه لتفعيل نقطة اتصال النظام:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.openSystemTetheringSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تشغيل هوتسبوت الإنترنت المباشر 🌐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // --- The Killer Feature: TTL Carrier Bypass Toolkit (أداة فك حظر الـ TTL لشركات الاتصالات) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مركز فك حظر الـ TTL (Carrier Bypass)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "هل اتصل الكمبيوتر بالشبكة وظهرت عبارة 'No Internet'؟\n" +
                                "السبب هو أن شركة الاتصالات تفحص قيمة حزم الـ TTL لكشف البث وحظره. لحل المشكلة فوراً وخداع الشركة لتظن أنك تتصفح من هاتفك فقط:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // OS Tabs: Windows / Mac
                    TabRow(
                        selectedTabIndex = selectedTtlTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Primary,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTtlTab == 0,
                            onClick = { selectedTtlTab = 0 },
                            text = { Text("💻 كمبيوتر ويندوز (Windows)", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTtlTab == 1,
                            onClick = { selectedTtlTab = 1 },
                            text = { Text("🍏 ماك / لينكس (Mac/Linux)", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTtlTab == 0) {
                        // Windows TTL Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "الأمر السحري لويندوز (تعديل TTL إلى 65):",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "netsh int ipv4 set glob defaultcurhoplimit=65",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    copyToClipboard("أمر فك الحظر لويندوز", "netsh int ipv4 set glob defaultcurhoplimit=65 && netsh int ipv6 set glob defaultcurhoplimit=65")
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخ الأمر لتشغيله في CMD كمسؤول")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 نصيحة: ستجد أيضاً على سطح مكتب كمبيوترك أداة 'فك_حظر_الإنترنت_عبر_TTL_65.bat' جاهزة بنقرة واحدة!",
                            fontSize = 11.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        // Mac / Linux TTL Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "الأمر السحري لأجهزة Mac / Linux:",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "sudo sysctl -w net.inet.ip.ttl=65",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    copyToClipboard("أمر فك الحظر لماك", "sudo sysctl -w net.inet.ip.ttl=65")
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخ الأمر لتشغيله في Terminal")
                            }
                        }
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
                        shape = RoundedCornerShape(22.dp),
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
                                        text = "بيانات نقطة الاتصال النشطة",
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
                                label = "اسم الشبكة (SSID)",
                                value = active.ssid,
                                onCopy = { copyToClipboard("اسم الشبكة", active.ssid) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password Row
                            CredentialRow(
                                label = "كلمة المرور (Password)",
                                value = active.passphrase.ifEmpty { "(شبكة مفتوحة)" },
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
                                    text = "البث نشط ومحمي بتشفير WPA2-PSK",
                                    fontSize = 13.sp,
                                    color = Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // --- Connected Clients Section (MyPublicWiFi Style) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
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

                        if (clients.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Success.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${clients.size} أجهزة نشطة",
                                    color = Success,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (clients.isEmpty()) {
                        Text(
                            text = if (hotspotState is HotspotState.Active)
                                "لا توجد أجهزة متصلة حالياً. بانتظار اتصال حاسوبك أو أجهزتك..."
                            else
                                "قم بتشغيل نقطة الاتصال لمراقبة الأجهزة المتصلة وحركة البيانات.",
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

            // --- Hardware Diagnostics Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
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
                            title = "دعم تردد 5GHz فائق السرعة",
                            isSupported = cap.is5GHzSupported
                        )
                        DiagnosticItem(
                            title = "دعم تقنية Wi-Fi Direct (P2P)",
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
                            text = "الجهاز: ${cap.deviceModel} • ${cap.androidVersion}",
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
private fun FuturisticHeroToggle(
    state: HotspotState,
    onToggle: () -> Unit
) {
    val isActive = state is HotspotState.Active
    val isStarting = state is HotspotState.Starting

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale1"
    )
    val pulseScale2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale2"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            if (isActive) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale1)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                )
                // Inner Pulse Ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale2)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.25f))
                )
            }

            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isStarting, onClick = onToggle),
                color = when {
                    isActive -> Success
                    isStarting -> Primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shadowElevation = 8.dp,
                border = BorderStroke(
                    width = 3.dp,
                    brush = if (isActive) Brush.sweepGradient(listOf(CyberCyan, NeonGreen, CyberCyan)) else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Transparent))
                )
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
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = when (state) {
                is HotspotState.Active -> Success
                is HotspotState.Error -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isActive) "اضغط لإيقاف البث" else "اضغط هنا لبدء البث الفوري وتجاوز قيود الشبكات",
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
            .clip(RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Success)
            )
            Spacer(modifier = Modifier.width(10.dp))
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

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Success.copy(alpha = 0.12f)
        ) {
            Text(
                text = "متصل",
                color = Success,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
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
