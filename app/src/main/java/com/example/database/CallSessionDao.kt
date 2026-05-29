package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallSessionDao {

    @Query("SELECT * FROM call_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<CallSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CallSession)

    @Delete
    suspend fun deleteSession(session: CallSession)

    @Query("DELETE FROM call_sessions")
    suspend fun clearAll()
}
