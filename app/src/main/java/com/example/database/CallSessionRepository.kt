package com.example.database

import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting DB queries as reactive Flows, protecting main thread.
 */
class CallSessionRepository(private val callSessionDao: CallSessionDao) {

    val allSessions: Flow<List<CallSession>> = callSessionDao.getAllSessions()

    suspend fun insert(session: CallSession) {
        callSessionDao.insertSession(session)
    }

    suspend fun delete(session: CallSession) {
        callSessionDao.deleteSession(session)
    }

    suspend fun clearAll() {
        callSessionDao.clearAll()
    }
}
