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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhotspot.app.domain.models.TtlEngineMode
import com.myhotspot.app.ui.theme.CyberBlue
import com.myhotspot.app.ui.theme.CyberCyan
import com.myhotspot.app.ui.theme.NeonGreen
import com.myhotspot.app.ui.theme.Primary
import com.myhotspot.app.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtlHomeScreen(
    viewModel: TtlViewModel,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val state by viewModel.engineState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val isDiagnosing by viewModel.isDiagnosing.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val logs by viewModel.commandLogs.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showDiagnosticsSection by remember { mutableStateOf(true) }

    fun copyText(label: String, text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "تم نسخ $label بنجاح", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TTL Hotspot Pro",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "معدل الـ TTL الجذري لتخطي حظر الشبكات",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runDiagnostics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "فحص التوافق", tint = CyberCyan)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = CyberCyan)
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

            // ==============================================================
            // 1. HERO STATUS & BIG GLOWING TOGGLE
            // ==============================================================
            TtlHeroToggle(
                isRunning = state.isRunning,
                ttlValue = state.ttlValue,
                activeMode = state.activeMode,
                onToggle = {
                    onRequestPermissions()
                    viewModel.toggleEngine()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Interface & Mode Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Upstream
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("الإنترنت (Upstream)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = state.upstreamInterface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (state.upstreamInterface != "None") CyberCyan else Color.Gray
                        )
                    }
                }

                // Hotspot Iface
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("بث الهوتسبوت (AP)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = state.hotspotInterface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (state.hotspotInterface != "None") NeonGreen else Color.Gray
                        )
                    }
                }

                // Active Method
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("الوضع النشط", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = when (state.activeMode) {
                                TtlEngineMode.IPTABLES_TTL -> "iptables TTL"
                                TtlEngineMode.NFTABLES -> "nftables"
                                TtlEngineMode.SYSCTL -> "sysctl"
                                TtlEngineMode.NONE -> "غير نشط"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (state.isRunning) Success else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==============================================================
            // 2. ONE-TAP SYSTEM HOTSPOT LAUNCHER
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.5.dp, Primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نقطة اتصال الهاتف (Mobile Hotspot)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "قم بتشغيل نقطة الاتصال في الهاتف. القواعد الجذرية (Root TTL) ستعدل كافة الحزم الصادرة من أي جهاز متصل تلقائياً لتبدو قادمة من هاتفك فقط!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.openSystemHotspotSettings() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح إعدادات الهوتسبوت في النظام 🚀", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ==============================================================
            // 3. PRE-FLIGHT COMPATIBILITY CHECKLIST (فحص التوافق وصلاحيات الروت)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فحص توافق الروت والنواة (Diagnostics)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        IconButton(onClick = { showDiagnosticsSection = !showDiagnosticsSection }) {
                            Icon(
                                imageVector = if (showDiagnosticsSection) Icons.Default.Close else Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = showDiagnosticsSection) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            if (isDiagnosing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("جاري فحص توافق النظام والنواة...", fontSize = 12.sp)
                                }
                            } else {
                                diagnostics.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (item.isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (item.isPassed) Success else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(item.detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // 4. CONNECTED CLIENTS (الأجهزة المتصلة - MyPublicWiFi Style)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الأجهزة المتصلة (${clients.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Surface(shape = CircleShape, color = Primary.copy(alpha = 0.15f)) {
                            Text(
                                text = "${clients.size} أجهزة",
                                color = Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (clients.isEmpty()) {
                        Text(
                            text = "لا توجد أجهزة متصلة بالهوتسبوت حالياً. فور اتصال أي كمبيوتر أو هاتف سيظهر هنا تلقائياً مع عنوان الـ IP والـ MAC.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        clients.forEach { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dev.hostname, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("IP: ${dev.ip}  •  MAC: ${dev.mac}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Success.copy(alpha = 0.2f)) {
                                    Text("محمي بـ TTL 65 ✓", color = Success, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // 5. LIVE COMMAND CONSOLE & MAGISK TOOLS
            // ==============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showLogsDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سجل الأوامر (${logs.size})")
                }

                Button(
                    onClick = { viewModel.installMagiskBootScript() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تثبيت سكريبت Magisk")
                }
            }
        }
    }

    // ==============================================================
    // SETTINGS DIALOG
    // ==============================================================
    if (showSettingsDialog) {
        var tempTtl by remember { mutableStateOf(settings.ttlValue.toString()) }
        var tempBlockIpv6 by remember { mutableStateOf(settings.blockIpv6) }
        var tempRedirectDns by remember { mutableStateOf(settings.redirectDns) }
        var tempAutoStart by remember { mutableStateOf(settings.autoStartOnHotspot) }
        var tempStartOnBoot by remember { mutableStateOf(settings.startOnBoot) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات TTL Hotspot", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // TTL Field
                    OutlinedTextField(
                        value = tempTtl,
                        onValueChange = { tempTtl = it.filter { char -> char.isDigit() } },
                        label = { Text("قيمة الـ TTL المطلوبة (الافتراضي: 65)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Block IPv6
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حظر IPv6 لعملاء الهوتسبوت", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("يمنع تسريب الحزم عبر IPv6 إذا كانت شركة الاتصالات تحظرها", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tempBlockIpv6,
                            onCheckedChange = { tempBlockIpv6 = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Redirect DNS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إعادة توجيه DNS إلى 8.8.8.8", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تجاوز حظر الـ DNS ومنع فحص استعلامات النطاق", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tempRedirectDns,
                            onCheckedChange = { tempRedirectDns = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Start on Boot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("التشغيل التلقائي عند إقلاع النظام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تفعيل قواعد الـ TTL فور إعادة تشغيل الهاتف", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tempStartOnBoot,
                            onCheckedChange = { tempStartOnBoot = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Primary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ttl = tempTtl.toIntOrNull() ?: 65
                        viewModel.updateTtlValue(ttl)
                        viewModel.updateBlockIpv6(tempBlockIpv6)
                        viewModel.updateRedirectDns(tempRedirectDns)
                        viewModel.updateAutoStart(tempAutoStart)
                        viewModel.updateStartOnBoot(tempStartOnBoot)
                        showSettingsDialog = false
                    }
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSettingsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ==============================================================
    // COMMAND LOGS DIALOG (TERMINAL)
    // ==============================================================
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سجل أوامر الروت (Shell Console)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = {
                        val allLogs = logs.joinToString("\n") { "[${it.command}] -> Code: ${it.exitCode}\n${it.output}" }
                        copyText("سجل الأوامر", allLogs)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(20.dp))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (logs.isEmpty()) {
                        Text("لا توجد أوامر منفذة حتى الآن.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        logs.takeLast(50).forEach { log ->
                            Text(
                                text = "# ${log.command}",
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (log.output.isNotEmpty()) {
                                Text(
                                    text = log.output,
                                    color = if (log.exitCode == 0) Color.LightGray else Color(0xFFEF4444),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLogsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun TtlHeroToggle(
    isRunning: Boolean,
    ttlValue: Int,
    activeMode: TtlEngineMode,
    onToggle: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.2f))
                )
            }

            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle),
                color = if (isRunning) Success else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
                border = BorderStroke(2.dp, if (isRunning) NeonGreen else Color.Gray)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isRunning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) "TTL نشط" else "بدء الـ TTL",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isRunning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "TTL = $ttlValue",
                        fontSize = 11.sp,
                        color = if (isRunning) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
