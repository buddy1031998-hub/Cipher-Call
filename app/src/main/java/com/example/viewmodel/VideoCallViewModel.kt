package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.CallSession
import com.example.database.CallSessionRepository
import com.example.security.E2eeEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

// Sealed UI screens
sealed interface AppScreen {
    object Dashboard : AppScreen
    object ActiveCall : AppScreen
}

// Call state lifecycle
enum class CallState {
    IDLE,
    CONNECTING,
    HANDSHAKING,
    SECURED
}

// Network profiles
enum class NetworkProfile(
    val label: String,
    val latencyMs: Int,
    val bitrateMbps: Double,
    val fps: Int,
    val resolution: String,
    val packetLossPercent: Double,
    val codec: String
) {
    FIVE_G("5G (Ultra-Low Latency)", 12, 5.2, 60, "1080p (FHD)", 0.04, "AV1 Video / Opus Audio"),
    WI_FI("Wi-Fi (Standard)", 22, 3.8, 30, "1080p (HD)", 0.08, "VP9 Video / Opus Audio"),
    FOUR_G("4G / LTE", 38, 2.1, 30, "720p (HD)", 0.35, "H.264 Video / AMR Audio"),
    THREE_G("3G (Congested)", 115, 0.65, 15, "360p (SD)", 2.8, "H.264 Video / AMR Audio")
}

// Message data model
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val rawEncrypted: String, // Ciphertext in transit (hex/base64)
    val decryptedText: String, // Decrypted plaintext
    val timestamp: Long = System.currentTimeMillis()
)

data class CallUiState(
    // Routing/Screen state
    val currentScreen: AppScreen = AppScreen.Dashboard,
    
    // Room Configuration
    val roomIdInput: String = "",
    val roomNameInput: String = "Engineering Secure Sync",
    val passphraseInput: String = "",
    
    // Active Room State
    val activeRoomId: String = "",
    val activeRoomName: String = "",
    val activePassphrase: String = "",
    val activeFingerprint: String = "",
    val sessionKey: ByteArray = ByteArray(0),
    val isFingerprintVerifiedByMe: Boolean = false,
    
    // active hardware preferences
    val callState: CallState = CallState.IDLE,
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    
    // Diagnostics & Stream Specs
    val currentNetwork: NetworkProfile = NetworkProfile.FIVE_G,
    val activeLatencyMs: Int = 12,
    val activePacketLoss: Double = 0.04,
    val durationSeconds: Int = 0,
    
    // Chat logs
    val messages: List<ChatMessage> = emptyList()
)

class VideoCallViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CallSessionRepository
    val historyLogs: StateFlow<List<CallSession>>

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var telemetryJob: Job? = null
    private var peerSimulationJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CallSessionRepository(database.callSessionDao())
        
        historyLogs = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Input handlers
    fun updateRoomIdInput(v: String) {
        _uiState.update { it.copy(roomIdInput = v) }
    }

    fun updateRoomNameInput(v: String) {
        _uiState.update { it.copy(roomNameInput = v) }
    }

    fun updatePassphraseInput(v: String) {
        _uiState.update { it.copy(passphraseInput = v) }
    }

    // Pre-populate join configuration for quick user action
    fun prepareQuickJoin(historySession: CallSession) {
        _uiState.update {
            it.copy(
                roomIdInput = historySession.roomId,
                roomNameInput = historySession.roomName,
                passphraseInput = historySession.passphrase
            )
        }
    }

    // Call start actions
    fun createAndStartNewRoom() {
        val randomId = "CC-${Random.nextInt(100, 999)}-${Random.nextInt(100, 999)}"
        val wordList = listOf("alpha", "bravo", "cipher", "delta", "echo", "foxtrot", "gcm", "halo", "ion", "jet", "kilo", "lima", "modem", "nexus")
        val randomPass = "${wordList.random()}-${wordList.random()}-${wordList.random()}"
        
        _uiState.update {
            it.copy(
                activeRoomId = randomId,
                activeRoomName = it.roomNameInput.ifBlank { "Secure Workspace Room" },
                activePassphrase = randomPass
            )
        }
        
        commenceHandshakeFlow()
    }

    fun joinExistingRoom() {
        val state = _uiState.value
        val joinedId = state.roomIdInput.trim().ifBlank { 
            "CC-${Random.nextInt(100, 999)}-${Random.nextInt(100, 999)}" 
        }
        val joinedPass = state.passphraseInput.trim().ifBlank { "secure-crypto-vault-auth" }
        
        _uiState.update {
            it.copy(
                activeRoomId = joinedId,
                activeRoomName = state.roomNameInput.ifBlank { "Private Encrypted Room" },
                activePassphrase = joinedPass
            )
        }
        
        commenceHandshakeFlow()
    }

    private fun commenceHandshakeFlow() {
        // Stop any old jobs
        stopAllJobs()

        val passphrase = _uiState.value.activePassphrase
        // Standard session key derivation using SHA-256
        val sessionKeyBytes = E2eeEngine.deriveKeyFromPassphrase(passphrase)
        val fingerprint = E2eeEngine.getFingerprint(sessionKeyBytes)

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.ActiveCall,
                callState = CallState.CONNECTING,
                activeFingerprint = fingerprint,
                sessionKey = sessionKeyBytes,
                isFingerprintVerifiedByMe = false,
                durationSeconds = 0,
                messages = emptyList()
            )
        }

        // Start connection simulation
        viewModelScope.launch {
            // Step 1: Connecting low latency signal
            delay(1200)
            _uiState.update { it.copy(callState = CallState.HANDSHAKING) }
            
            // Step 2: Crypto handshake (Key derivation exchange verification)
            delay(1500)
            _uiState.update { it.copy(callState = CallState.SECURED) }
            
            // Start active session timers and simulators
            startTimer()
            startTelemetryFluctuation()
            startMockPeerConversation()
        }
    }

    // Timer management
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    // Network & Quality Telemetry Fluctuation Simulation (Real-time latency charts feed)
    private fun startTelemetryFluctuation() {
        telemetryJob = viewModelScope.launch {
            while (true) {
                delay(1800)
                val profile = _uiState.value.currentNetwork
                // Introduce organic tiny fluctuations to standard network readings (+/- 2ms or minor packet losses)
                val marginMs = Random.nextInt(-2, 3)
                val finalLatency = (profile.latencyMs + marginMs).coerceAtLeast(3)
                
                val lossFluct = Random.nextDouble(-0.02, 0.03)
                val finalLoss = (profile.packetLossPercent + lossFluct).coerceIn(0.0, 10.0)

                _uiState.update {
                    it.copy(
                        activeLatencyMs = finalLatency,
                        activePacketLoss = finalLoss
                    )
                }
            }
        }
    }

    // Simulate conversational messaging through our symmetric cryptographic tunnel
    private fun startMockPeerConversation() {
        peerSimulationJob = viewModelScope.launch {
            delay(5000) // Initial welcome peer
            incomingEncryptedPeerMessage("Connection secured. Audio delay is around 12ms. High fidelity connection established successfully.")

            delay(12000)
            incomingEncryptedPeerMessage("Yes, AES-256 encryption is running on the hardware channels. My fingerprint indicator says: ${_uiState.value.activeFingerprint}. Matches yours?")

            delay(15000)
            incomingEncryptedPeerMessage("Tested switching to mobile LTE network. Low-latency streaming specs still averaging under 40ms!")

            delay(18000)
            incomingEncryptedPeerMessage("Let me know when you approve the fingerprint key verification so helper audio codecs can unlock maximum bitrates.")
        }
    }

    private fun incomingEncryptedPeerMessage(plainText: String) {
        val state = _uiState.value
        if (state.sessionKey.isEmpty()) return

        // Undergo real AES-GCM encryption modeling a peer sending us payload in transit
        val encryptedPayload = E2eeEngine.encrypt(plainText, state.sessionKey)
        // Decrypt on arrival using the exact same derived symmetric key
        val decryptedResult = E2eeEngine.decrypt(encryptedPayload, state.sessionKey)

        val peerMsg = ChatMessage(
            sender = "Veritas Remote",
            rawEncrypted = encryptedPayload,
            decryptedText = decryptedResult
        )

        _uiState.update {
            it.copy(messages = it.messages + peerMsg)
        }
    }

    // Toggle Network Profiles
    fun changeNetworkProfile(profile: NetworkProfile) {
        _uiState.update {
            it.copy(
                currentNetwork = profile,
                activeLatencyMs = profile.latencyMs,
                activePacketLoss = profile.packetLossPercent
            )
        }
    }

    // UI actions
    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleVideo() {
        _uiState.update { it.copy(isVideoEnabled = !it.isVideoEnabled) }
    }

    fun flipCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun verifyFingerprintChecked(verified: Boolean) {
        _uiState.update { it.copy(isFingerprintVerifiedByMe = verified) }
        
        // Notify user about peer sync
        if (verified) {
            viewModelScope.launch {
                delay(1200)
                incomingEncryptedPeerMessage("Fingerprint verified and matched! Cryptographic tunnel is now status: LOCKED & VERIFIED.")
            }
        }
    }

    // Encrypted Chat messaging
    fun sendEncryptedMessage(plainText: String) {
        if (plainText.isBlank()) return
        val state = _uiState.value
        if (state.sessionKey.isEmpty()) return

        // 1. Perform authentic AES encryption on user's client device
        val encryptedPayload = E2eeEngine.encrypt(plainText, state.sessionKey)
        
        // 2. Perform authentic description check so the UI can log payload correctness
        val decryptedResult = E2eeEngine.decrypt(encryptedPayload, state.sessionKey)

        val localMsg = ChatMessage(
            sender = "You",
            rawEncrypted = encryptedPayload,
            decryptedText = decryptedResult
        )

        _uiState.update {
            it.copy(messages = it.messages + localMsg)
        }

        // Optional responsive remote peer reply to chat messages
        triggerAutoReply(plainText)
    }

    private fun triggerAutoReply(userMessage: String) {
        viewModelScope.launch {
            delay(2000)
            val lower = userMessage.lowercase()
            val reply = when {
                lower.contains("hello") || lower.contains("hi") -> "Hey there! Secure AES tunnel is running smoothly. Low-latency packets flow normally."
                lower.contains("key") || lower.contains("fingerprint") -> "Perfect! My client also verified key fingerprint fingerprint matches ${_uiState.value.activeFingerprint}."
                lower.contains("latency") || lower.contains("delay") || lower.contains("test") -> "Averaging ultra low audio/video capture latency. Jitter bounds under 2.5ms."
                else -> "Message received securely over decentralized E2EE socket pipeline."
            }
            incomingEncryptedPeerMessage(reply)
        }
    }

    // Call End Action
    fun endCall() {
        val state = _uiState.value
        if (state.callState != CallState.IDLE) {
            // Save this call session log to Room historical database
            viewModelScope.launch {
                val duration = state.durationSeconds
                // Capture average metrics from telemetry values
                val sessionRecord = CallSession(
                    roomId = state.activeRoomId,
                    roomName = state.activeRoomName,
                    passphrase = state.activePassphrase,
                    fingerprint = state.activeFingerprint,
                    durationSeconds = duration,
                    avgLatencyMs = state.activeLatencyMs,
                    avgPacketLossPercent = state.activePacketLoss,
                    codecUsed = state.currentNetwork.codec
                )
                repository.insert(sessionRecord)
            }
        }

        stopAllJobs()
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.Dashboard,
                callState = CallState.IDLE,
                durationSeconds = 0,
                messages = emptyList(),
                isFingerprintVerifiedByMe = false
            )
        }
    }

    fun deleteHistorySession(session: CallSession) {
        viewModelScope.launch {
            repository.delete(session)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private fun stopAllJobs() {
        timerJob?.cancel()
        telemetryJob?.cancel()
        peerSimulationJob?.cancel()
        timerJob = null
        telemetryJob = null
        peerSimulationJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs()
    }
}
