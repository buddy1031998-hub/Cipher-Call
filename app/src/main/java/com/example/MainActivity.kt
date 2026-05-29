package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.components.CameraPreview
import com.example.database.CallSession
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CallState
import com.example.viewmodel.CallUiState
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.NetworkProfile
import com.example.viewmodel.VideoCallViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val viewModel: VideoCallViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val historyLogs by viewModel.historyLogs.collectAsState()

    // Permissions Controller
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraOk = permissions[Manifest.permission.CAMERA] ?: false
        val audioOk = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        permissionsGranted = cameraOk && audioOk
        if (!permissionsGranted) {
            Toast.makeText(context, "Real video preview defaults to simulations if permissions are restricted.", Toast.LENGTH_LONG).show()
        }
    }

    // Launch permission checks on start
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1C1B1F)) // Elegant Dark Background
        ) {
            when (uiState.currentScreen) {
                AppScreen.Dashboard -> {
                    DashboardScreen(
                        uiState = uiState,
                        historyLogs = historyLogs,
                        permissionsGranted = permissionsGranted,
                        requestPermissionsLauncher = launcher,
                        onUpdateRoomId = viewModel::updateRoomIdInput,
                        onUpdateRoomName = viewModel::updateRoomNameInput,
                        onUpdatePassphrase = viewModel::updatePassphraseInput,
                        onQuickJoin = viewModel::prepareQuickJoin,
                        onCreateRoom = viewModel::createAndStartNewRoom,
                        onJoinRoom = viewModel::joinExistingRoom,
                        onDeleteSession = viewModel::deleteHistorySession,
                        onClearAllHistory = viewModel::clearAllHistory
                    )
                }
                AppScreen.ActiveCall -> {
                    ActiveCallScreen(
                        uiState = uiState,
                        permissionsGranted = permissionsGranted,
                        onToggleMute = viewModel::toggleMute,
                        onToggleVideo = viewModel::toggleVideo,
                        onFlipCamera = viewModel::flipCamera,
                        onChangeNetwork = viewModel::changeNetworkProfile,
                        onVerifyFingerprint = viewModel::verifyFingerprintChecked,
                        onSendMessage = viewModel::sendEncryptedMessage,
                        onEndCall = viewModel::endCall
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    uiState: CallUiState,
    historyLogs: List<CallSession>,
    permissionsGranted: Boolean,
    requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    onUpdateRoomId: (String) -> Unit,
    onUpdateRoomName: (String) -> Unit,
    onUpdatePassphrase: (String) -> Unit,
    onQuickJoin: (CallSession) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onDeleteSession: (CallSession) -> Unit,
    onClearAllHistory: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Create Secure Meeting", "Join Private Room")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Premium Branding Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF49454F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Shield Lock Emblem",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "CipherCall",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD0BCFF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DECENTRALIZED AES-GCM 256B",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Row styling
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF211F26),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFD0BCFF),
                    height = 2.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) Color(0xFFD0BCFF) else Color(0xFFE6E1E5).copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active tab configuration fields
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
            border = borderStrokeDefault()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    // CREATE MEETING MODE
                    Text(
                        text = "Initialize Secure Conference Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "An E2EE cryptographic key fingerprint will be derived automatically to sign and encrypt camera frame packets and in-call messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = uiState.roomNameInput,
                        onValueChange = onUpdateRoomName,
                        label = { Text("Display Room Topic") },
                        placeholder = { Text("Engineering Secure Standup") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_name_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCreateRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_room_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.EnhancedEncryption, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Secured Meet", fontWeight = FontWeight.Bold)
                    }

                } else {
                    // JOIN EXISTING ROOM
                    Text(
                        text = "Enter Room Cryptographic Keys",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = uiState.roomIdInput,
                        onValueChange = onUpdateRoomId,
                        label = { Text("Room ID or Channel Code") },
                        placeholder = { Text("CC-294-814") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_id_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.passphraseInput,
                        onValueChange = onUpdatePassphrase,
                        label = { Text("AES Cryptographic Passphrase") },
                        placeholder = { Text("alpha-bravo-tango") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passphrase_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onJoinRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("join_room_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF381E72))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Handshake & Join", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECURE MEETING LOGS ARCHIVE (Room History logs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "History Logs",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Secure Meeting Logs (${historyLogs.size})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            if (historyLogs.isNotEmpty()) {
                TextButton(onClick = onClearAllHistory) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wipe All Logs", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (historyLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .background(Color(0xFF211F26)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Lock",
                        tint = Color(0xFFE6E1E5).copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History logs are completely ephemeral.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE6E1E5).copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Previous E2EE key sessions will list here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE6E1E5).copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF211F26)),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyLogs, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQuickJoin(session) }
                            .testTag("history_item_${session.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                        border = borderStrokeDefault()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.roomName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Room: ${session.roomId}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD0BCFF),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("${session.roomId} | Key: ${session.passphrase}"))
                                            Toast.makeText(context, "Copied credentials to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy Keys",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteSession(session) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Log Record",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF49454F),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Telemetry Stats Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LabelValueText(
                                    label = "E2EE Hash",
                                    value = session.fingerprint.take(11) + "..."
                                )
                                LabelValueText(
                                    label = "Latency Avg",
                                    value = "${session.avgLatencyMs}ms"
                                )
                                LabelValueText(
                                    label = "Loss Avg",
                                    value = String.format("%.2f%%", session.avgPacketLossPercent)
                                )
                                LabelValueText(
                                    label = "Duration",
                                    value = formatDuration(session.durationSeconds)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Camera Permission Banner Check inside setting page
        if (!permissionsGranted) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF291E1E)),
                border = borderStrokeDefault(Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.NoEncryption, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Camera & audio accesses restricted. Stream reverts to offline secure animations.",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                    Button(
                        onClick = {
                            requestPermissionsLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. ACTIVE VIDEO CALL SCREEN
// ==========================================
@Composable
fun ActiveCallScreen(
    uiState: CallUiState,
    permissionsGranted: Boolean,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onChangeNetwork: (NetworkProfile) -> Unit,
    onVerifyFingerprint: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onEndCall: () -> Unit
) {
    var showChatPanel by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll chat list to bottom whenever new message arrives
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                lazyListState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // A. Header Bar (Room Title & Security locked)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1B1F)) // Elegant Dark Background
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular ArrowBack representing Leave/Exit
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF49454F), CircleShape)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Hang Up / Leave",
                        tint = Color(0xFFE6E1E5),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = uiState.activeRoomName,
                        color = Color(0xFFE6E1E5),
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Encrypted Connection icon",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "END-TO-END ENCRYPTED",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Chat Toggle Trigger Button on Right
            IconButton(
                onClick = { showChatPanel = !showChatPanel },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (showChatPanel) Color(0xFFD0BCFF) else Color(0xFF49454F),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = "Toggle Chat",
                    tint = if (showChatPanel) Color(0xFF381E72) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // B. Main Streaming Area (Adaptive Peer Video Grids)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(32.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Dynamic Video Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Video 1: Simulated Remote Peer Stream Panel
                    RemoteParticipantStreamView(uiState = uiState)

                    // Overlay 1: Low-Latency Signal Bar Graph
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Signal visualizer bar graph
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                modifier = Modifier.height(10.dp)
                            ) {
                                val latency = uiState.activeLatencyMs
                                val filledBars = if (latency < 40) 4 else if (latency < 80) 3 else if (latency < 120) 2 else 1
                                for (barIdx in 1..4) {
                                    val barHeight = 2.dp + (barIdx * 2).dp
                                    val barColor = if (barIdx <= filledBars) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.2f)
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(barHeight)
                                            .background(barColor, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.activeLatencyMs}ms LATENCY",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                        }
                    }

                    // Overlay 2: Participant Info Label Tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${uiState.activeRoomName} • ${uiState.currentNetwork.resolution}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Video 2: Floating PIP Local WebCam (CameraX Feed)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(width = 112.dp, height = 176.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFFD0BCFF), RoundedCornerShape(16.dp))
                            .background(Color(0xFF2B2930))
                    ) {
                        if (permissionsGranted && uiState.isVideoEnabled) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                isFrontCamera = uiState.isFrontCamera
                            )
                        } else {
                            // Offline Camera State
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (!uiState.isVideoEnabled) Icons.Filled.VideocamOff else Icons.Filled.NoEncryption,
                                        contentDescription = "Video Muted",
                                        tint = Color(0xFF94A3B8).copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (!uiState.isVideoEnabled) "Cam Muted" else "No Camera",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Floating mic indicate badge at the top-right corner of PIP
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Local Mic Status",
                            tint = if (uiState.isMuted) Color(0xFFEF4444) else Color(0xFFD0BCFF),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(16.dp)
                        )

                        // Floating Badge indicating LOCAL PREVIEW
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "You",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Panel: Sliding Encrypted Chat Sidebar Panel
                AnimatedVisibility(
                    visible = showChatPanel,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .background(Color(0xFF211F26))
                        .drawBehind {
                            drawLine(
                                color = Color(0xFF49454F),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 2f
                            )
                        }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2B2930))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Secure Channels",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "E2EE Tunnel Chat",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showChatPanel = false }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = "Hide", tint = Color.LightGray)
                            }
                        }

                        // Text listing
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.messages) { message ->
                                ChatMessageItem(message = message)
                            }
                        }

                        // Bottom Typing Field with real AES encryption trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131824))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("E2EE encrypted text...", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("chat_input_text_field"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (chatInput.isNotBlank()) {
                                        onSendMessage(chatInput)
                                        chatInput = ""
                                    }
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color(0xFF49454F),
                                    focusedContainerColor = Color(0xFF1C1B1F),
                                    unfocusedContainerColor = Color(0xFF1C1B1F)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        onSendMessage(chatInput)
                                        chatInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFD0BCFF), CircleShape)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // C. Cryptographic Fingerprint Key Indicator Banner
        E2eeFingerprintBanner(uiState = uiState, onVerify = onVerifyFingerprint)

        // D. Interactive Low-Latency Diagnostics Card (5G/WiFi Profile switcher + Line Chart)
        DiagnosticsControlPanel(uiState = uiState, onChangeNetwork = onChangeNetwork)

        // E. In Call Float Buttons Toolbar (Mute/Video triggers)
        CallControlDock(
            uiState = uiState,
            onToggleMute = onToggleMute,
            onToggleVideo = onToggleVideo,
            onFlipCamera = onFlipCamera,
            onEndCall = onEndCall
        )
    }
}

// ==========================================
// REMOTE PARTICIPANT VIEW
// ==========================================
@Composable
fun RemoteParticipantStreamView(uiState: CallUiState) {
    val infiniteTransition = rememberInfiniteTransition()

    // Smooth wave calculations for sound animation
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative low-latency violet glow pulses
        Box(
            modifier = Modifier
                .size((140f * pulseRatio).dp)
                .background(Color(0xFFD0BCFF).copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size((90f * pulseRatio).dp)
                .background(Color(0xFFD0BCFF).copy(alpha = 0.12f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2930))
                    .border(2.dp, Color(0xFF49454F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = if (uiState.callState == CallState.SECURED) Color(0xFFD0BCFF) else Color(0xFF94A3B8).copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Veritas Secure Node",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (uiState.callState == CallState.SECURED) Color(0xFFD0BCFF) else Color(0xFFEAB308))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.callState == CallState.SECURED) {
                        "DECRYPTED STREAM: ${uiState.currentNetwork.resolution} @ ${uiState.currentNetwork.fps}FPS"
                    } else "SYNCING CRYPTO CHANNELS...",
                    color = Color(0xFFD0BCFF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top right codec badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color(0xE62B2930), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${uiState.currentNetwork.codec.split(" ").first()} | AES-GCM-256",
                color = Color(0xFFD0BCFF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// CHAT MESSAGE ITEM (Showing Hex Transit Block)
// ==========================================
@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isMe = message.sender == "You"
    var showCipherLog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Chat Sender Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = message.sender,
                color = if (isMe) Color(0xFFD0BCFF) else Color(0xFFCCC2DC),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp)),
                color = Color.Gray,
                fontSize = 8.sp
            )
        }

        // Bubble Content Box
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isMe) 8.dp else 0.dp,
                        bottomEnd = if (isMe) 0.dp else 8.dp
                    )
                )
                .background(if (isMe) Color(0xFFD0BCFF) else Color(0xFF49454F))
                .clickable { showCipherLog = !showCipherLog }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.decryptedText,
                    color = if (isMe) Color(0xFF381E72) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Subtle tag suggesting click to view decryption details
                Text(
                    text = "🔒 E2EE Transit Sealed",
                    fontSize = 8.sp,
                    color = if (isMe) Color(0xFF381E72).copy(alpha = 0.6f) else Color(0xFFE6E1E5).copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expanded Transit Cipher details
        AnimatedVisibility(visible = showCipherLog) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                border = borderStrokeDefault(Color(0xFF49454F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = "CIPHERTEXT ON DECENTRALIZED SOCKET:",
                        fontSize = 8.sp,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = message.rawEncrypted,
                        fontSize = 8.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    HorizontalDivider(color = Color(0xFF49454F))

                    Text(
                        text = "DECRYPT INTEGRITY VERIFIED (HMAC/GCM TAG)",
                        fontSize = 8.sp,
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// E2EE FINGERPRINT ARCH KEY EXCHANGES BANNER
// ==========================================
@Composable
fun E2eeFingerprintBanner(uiState: CallUiState, onVerify: (Boolean) -> Unit) {
    if (uiState.callState != CallState.SECURED) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
        border = borderStrokeDefault(if (uiState.isFingerprintVerifiedByMe) Color(0xFFD0BCFF) else Color(0xFF49454F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isFingerprintVerifiedByMe) Icons.Filled.CheckCircle else Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = if (uiState.isFingerprintVerifiedByMe) Color(0xFFD0BCFF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Negotiated Security Fingerprint",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = uiState.activeFingerprint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Verify this key matches your peer's screen to secure against MITM.",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }

            Button(
                onClick = { onVerify(!uiState.isFingerprintVerifiedByMe) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isFingerprintVerifiedByMe) Color(0xFFD0BCFF) else Color(0xFF49454F)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(34.dp).testTag("verify_fingerprint_btn")
            ) {
                Text(
                    text = if (uiState.isFingerprintVerifiedByMe) "Verified" else "Verify",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isFingerprintVerifiedByMe) Color(0xFF381E72) else Color.White
                )
            }
        }
    }
}

// ==========================================
// LOW LATENCY COMPOSABLE LINE DIAGNOSTICS
// ==========================================
@Composable
fun DiagnosticsControlPanel(uiState: CallUiState, onChangeNetwork: (NetworkProfile) -> Unit) {
    if (uiState.callState != CallState.SECURED) return

    val currentProfile = uiState.currentNetwork
    
    // Maintain a small history of latencies to draw a real heartbeat wave
    val latencyPoints = remember(currentProfile) {
        val list = mutableStateListOf<Int>()
        list.addAll(listOf(
            uiState.activeLatencyMs,
            (uiState.activeLatencyMs - 1).coerceAtLeast(1),
            (uiState.activeLatencyMs + 2).coerceAtLeast(1),
            uiState.activeLatencyMs,
            (uiState.activeLatencyMs - 2).coerceAtLeast(1),
            (uiState.activeLatencyMs + 1).coerceAtLeast(1)
        ))
        list
    }

    // Accumulate or shift points on real VM telemetry updates
    LaunchedEffect(uiState.activeLatencyMs) {
        if (latencyPoints.size > 8) {
            latencyPoints.removeAt(0)
        }
        latencyPoints.add(uiState.activeLatencyMs)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
        border = borderStrokeDefault()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // A. Row showing Profile triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CellTower,
                        contentDescription = "Network Signal",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Evaluate Connection Latencies",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF49454F), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentProfile.resolution,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // B. Network Preset Selector row (Interactive triggers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NetworkProfile.values().forEach { profile ->
                    val isSelected = profile == currentProfile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930))
                            .clickable { onChangeNetwork(profile) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = if (isSelected) Color(0xFF381E72) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // C. Live Latency stats and Dynamic Heartbeat Canvas plot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(130.dp)) {
                    Text(
                        text = "${uiState.activeLatencyMs} ms Ping",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (uiState.activeLatencyMs < 40) Color(0xFFD0BCFF) else if (uiState.activeLatencyMs < 100) Color(0xFFEAB308) else Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${currentProfile.bitrateMbps} Mbps | Loss: ${String.format("%.2f%%", uiState.activePacketLoss)}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Diagnostic Pulse Plot Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1C1B1F))
                ) {
                    val lineColor = if (uiState.activeLatencyMs < 40) Color(0xFFD0BCFF) else if (uiState.activeLatencyMs < 100) Color(0xFFEAB308) else Color(0xFFEF4444)
                    val pointsCount = latencyPoints.size

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (pointsCount > 1) {
                            val path = Path()
                            val spacing = size.width / (pointsCount - 1)
                            
                            val maxVal = 130f
                            val minVal = 5f

                            latencyPoints.forEachIndexed { i, point ->
                                val x = i * spacing
                                val ratio = ((point.toFloat() - minVal) / (maxVal - minVal)).coerceIn(0.1f, 0.9f)
                                val y = size.height - (ratio * size.height)

                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Little overlay explaining Target
                    Text(
                        text = "LIVE LATENCY jitter FEED",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// CENTRALIZED BOTTOM CONTROL BAR
// ==========================================
@Composable
fun CallControlDock(
    uiState: CallUiState,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    if (uiState.callState == CallState.IDLE) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1B1F))
            .padding(vertical = 12.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Mic Button (Audio capture)
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(46.dp)
                    .background(if (uiState.isMuted) Color(0xFFEF4444) else Color(0xFF49454F), CircleShape)
                    .testTag("toggle_mic_btn")
            ) {
                Icon(
                    imageVector = if (uiState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Mute Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. Video Camera Button
            IconButton(
                onClick = onToggleVideo,
                modifier = Modifier
                    .size(46.dp)
                    .background(if (!uiState.isVideoEnabled) Color(0xFFEF4444) else Color(0xFF49454F), CircleShape)
                    .testTag("toggle_video_btn")
            ) {
                Icon(
                    imageVector = if (!uiState.isVideoEnabled) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                    contentDescription = "Disable Camera",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3. Swap Front/Back Camera Lens Button
            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF49454F), CircleShape)
                    .testTag("flip_camera_btn"),
                enabled = uiState.isVideoEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.FlipCameraAndroid,
                    contentDescription = "Flip Lens",
                    tint = if (uiState.isVideoEnabled) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 4. Hang up Destructive End Button
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFEF4444), CircleShape)
                    .testTag("end_call_control_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End Call Connection",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// HELPERS & CONSTANTS
// ==========================================
fun borderStrokeDefault(color: Color = Color(0xFF49454F)) =
    androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFD0BCFF),
    unfocusedBorderColor = Color(0xFF49454F),
    focusedLabelColor = Color(0xFFD0BCFF),
    unfocusedLabelColor = Color(0xFFE6E1E5).copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF1C1B1F),
    unfocusedContainerColor = Color(0xFF211F26)
)

@Composable
fun LabelValueText(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
