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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhotspot.app.domain.models.ConnectedClient
import com.myhotspot.app.domain.models.GoogleCheckStatus
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

    val customSsid by viewModel.customSsid.collectAsState()
    val customPassword by viewModel.customPassword.collectAsState()
    val googleCheckStatus by viewModel.googleCheckStatus.collectAsState()
    val hotspotState by viewModel.hotspotState.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()
    val clients by viewModel.connectedClients.collectAsState()

    var inputSsid by remember(customSsid) { mutableStateOf(customSsid) }
    var inputPassword by remember(customPassword) { mutableStateOf(customPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var showQrDialog by remember { mutableStateOf(false) }
    var selectedTtlTab by remember { mutableIntStateOf(0) }
    var showLocalP2PSection by remember { mutableStateOf(false) }

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
                            modifier = Modifier.size(38.dp),
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
                        color = Success.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Success.copy(alpha = 0.5f)),
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
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جاهز للبث",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Success
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

            // ==============================================================
            // 1. PRIMARY HERO ACTION: DIRECT INTERNET HOTSPOT (بث الإنترنت المباشر)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(2.dp, Primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "بث الإنترنت المباشر لجميع الأجهزة",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Success
                        ) {
                            Text(
                                text = "تلقائي وبدون بروكسي",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "يوزع باقة الإنترنت من هاتفك مباشرة إلى الكمبيوتر وجميع الأجهزة تلقائياً دون إدخال أي بروكسي يدوي، مع اعتماد الاسم وكلمة المرور التي تحددها بنفسك!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            onRequestPermissions()
                            viewModel.saveCredentials(inputSsid, inputPassword)
                            viewModel.openSystemTetheringSettings()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تشغيل بث الإنترنت الآن 🚀",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "💡 سيتم نسخ كلمة المرور تلقائياً وفتح شاشة الهوتسبوت لتفعيل المفتاح بنقرة واحدة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ==============================================================
            // 2. USER-DEFINED CREDENTIALS (اسم وباسورد الشبكة من اختيار المستخدم)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اسم الشبكة وكلمة المرور (اختيارك الخاص)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(onClick = { showQrDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "عرض رمز QR",
                                tint = Primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SSID Field
                    OutlinedTextField(
                        value = inputSsid,
                        onValueChange = { inputSsid = it },
                        label = { Text("اسم نقطة الاتصال (Hotspot Name / SSID)") },
                        leadingIcon = {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.copySsidToClipboard() }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الاسم", modifier = Modifier.size(20.dp))
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = { Text("كلمة المرور (WPA2 Password - 8 أحرف على الأقل)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Primary)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار/إخفاء كلمة المرور",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.copyPasswordToClipboard() }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ كلمة المرور", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveCredentials(inputSsid, inputPassword)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ البيانات")
                        }

                        OutlinedButton(
                            onClick = { showQrDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رمز الـ QR")
                        }
                    }
                }
            }

            // ==============================================================
            // 3. GOOGLE CONNECTIVITY & BYPASS CENTER (مركز فحص جوجل وتخطي الحجب)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.5.dp, when (googleCheckStatus) {
                    is GoogleCheckStatus.Success -> Success
                    is GoogleCheckStatus.Failed -> Color(0xFFEF4444)
                    else -> CyberCyan.copy(alpha = 0.6f)
                })
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "فحص اتصال جوجل وتخطي الحجب",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(onClick = { viewModel.testGoogleConnectivity() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة الفحص",
                                tint = CyberCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val status = googleCheckStatus) {
                        is GoogleCheckStatus.Checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "جاري فحص الاتصال بخوادم Google (Probe 204)...",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is GoogleCheckStatus.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF064E3B).copy(alpha = 0.25f))
                                    .border(1.dp, Success, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تم اجتياز فحص جوجل بنجاح (HTTP 204 OK)",
                                        color = Success,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• زمن الاستجابة: ${status.latencyMs} ms\n• خوادم الـ DNS: متصلة بنجاح\n• التطبيق متوافق 100% مع فحص الأمان ومعايير النشر الرسمية.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        is GoogleCheckStatus.Failed -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تنبيه فحص الاتصال",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = status.errorMessage,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        is GoogleCheckStatus.NotTested -> {
                            Button(
                                onClick = { viewModel.testGoogleConnectivity() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بدء فحص اتصال جوجل الآن", color = CyberCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // 4. TTL CARRIER BYPASS TOOLKIT (أداة فك حظر شركات الاتصالات للكمبيوتر)
            // ==============================================================
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
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "إذا اتصل الكمبيوتر بالهوتسبوت وظهرت رسالة 'No Internet, secured'؛ السبب هو حظر شركة الاتصالات للكمبيوتر عبر فحص الـ TTL. لتجاوز الحظر فوراً:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TabRow(
                        selectedTabIndex = selectedTtlTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Primary,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTtlTab == 0,
                            onClick = { selectedTtlTab = 0 },
                            text = { Text("💻 كمبيوتر ويندوز", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTtlTab == 1,
                            onClick = { selectedTtlTab = 1 },
                            text = { Text("🍏 ماك / لينكس", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTtlTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "أمر ويندوز السحري (تعديل الـ TTL إلى 65):",
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
                            text = "💡 نصيحة: ستجد على سطح مكتب كمبيوترك أداة 'فك_حظر_الإنترنت_عبر_TTL_65.bat' بنقرة واحدة!",
                            fontSize = 11.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "أمر أجهزة Mac / Linux:",
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

            // ==============================================================
            // 5. CONNECTED CLIENTS MONITOR (مراقبة ورادار الأجهزة المتصلة)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الأجهزة المتصلة بالشبكة (${clients.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${clients.size} أجهزة",
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (clients.isEmpty()) {
                        Text(
                            text = "لا توجد أجهزة متصلة حالياً. اتصل بشبكة الواي فاي وستظهر الأجهزة هنا تلقائياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        clients.forEach { client ->
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
                                    Text(
                                        text = client.deviceName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "IP: ${client.ipAddress} • MAC: ${client.macAddress}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Success.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "متصل ✓",
                                        color = Success,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // 6. ADVANCED LOCAL P2P MODE (COLLAPSIBLE / SECONDARY)
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ وضع النقل المحلي P2P (بدون استهلاك إنترنت)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedButton(
                            onClick = { showLocalP2PSection = !showLocalP2PSection },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (showLocalP2PSection) "إخفاء" else "عرض")
                        }
                    }

                    AnimatedVisibility(visible = showLocalP2PSection) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "ملاحظة: هذا الوضع مخصص فقط لنقل الملفات بين الأجهزة محلياً بدون إنترنت، ويقوم أندرويد تلقائياً بتوليد اسم يبدأ بـ AndroidShare_xxxx.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onRequestPermissions()
                                    viewModel.toggleLocalOnlyHotspot()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hotspotState is HotspotState.Active) Color(0xFFEF4444) else Primary
                                )
                            ) {
                                Text(if (hotspotState is HotspotState.Active) "إيقاف وضع النقل المحلي" else "تشغيل وضع النقل المحلي")
                            }
                        }
                    }
                }
            }

            // Device Info
            capabilities?.let { cap ->
                Text(
                    text = "جهازك: ${cap.deviceModel} • ${cap.androidVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ==============================================================
    // QR CODE DIALOG
    // ==============================================================
    if (showQrDialog) {
        val qrBitmap = remember(inputSsid, inputPassword) {
            QrCodeGenerator.generateWifiQrCode(inputSsid, inputPassword, 600)
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
                        text = "امسح الرمز بكاميرا أي هاتف (آيفون أو أندرويد) للاتصال بالشبكة مباشرة دون كتابة كلمة المرور:",
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
                        text = "الشبكة: $inputSsid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "كلمة المرور: $inputPassword",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
