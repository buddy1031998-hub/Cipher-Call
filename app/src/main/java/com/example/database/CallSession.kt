package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_sessions")
data class CallSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roomId: String,
    val roomName: String,
    val passphrase: String,
    val fingerprint: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val maxParticipants: Int = 2,
    val avgLatencyMs: Int,
    val avgPacketLossPercent: Double,
    val codecUsed: String = "AV1 Video / Opus Audio"
)
