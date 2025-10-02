package com.aura.substratecryptotest.data.user

import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID

/**
 * Repository para gestión de usuarios
 */
class UserRepository(
    private val userDao: UserDao,
    private val userLogbookDao: UserLogbookDao,
    private val userSessionDao: UserSessionDao
) {
    
    // ===== USER OPERATIONS =====
    
    suspend fun createUser(name: String, walletAddress: String? = null, did: String? = null): User {
        val user = User(
            name = name,
            walletAddress = walletAddress,
            did = did
        )
        val userId = userDao.insertUser(user)
        return user.copy(id = userId)
    }
    
    fun getAllActiveUsers(): Flow<List<User>> {
        return userDao.getAllActiveUsers()
    }
    
    suspend fun getUserById(id: Long): User? {
        return userDao.getUserById(id)
    }
    
    suspend fun getUserByWalletAddress(walletAddress: String): User? {
        return userDao.getUserByWalletAddress(walletAddress)
    }
    
    suspend fun getUserByDID(did: String): User? {
        return userDao.getUserByDID(did)
    }
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
    
    suspend fun updateLastLogin(userId: Long) {
        userDao.updateLastLogin(userId, Date())
    }
    
    suspend fun enableBiometric(userId: Long) {
        userDao.updateBiometricEnabled(userId, true)
    }
    
    suspend fun disableBiometric(userId: Long) {
        userDao.updateBiometricEnabled(userId, false)
    }
    
    // ===== USER-LOGBOOK ASSOCIATIONS =====
    
    suspend fun associateUserWithLogbook(
        userId: Long, 
        logbookId: Long, 
        role: LogbookRole = LogbookRole.OWNER
    ): UserLogbook {
        val userLogbook = UserLogbook(
            userId = userId,
            logbookId = logbookId,
            role = role,
            canEdit = role in listOf(LogbookRole.OWNER, LogbookRole.EDITOR),
            canDelete = role == LogbookRole.OWNER,
            canExport = role in listOf(LogbookRole.OWNER, LogbookRole.EDITOR, LogbookRole.VIEWER)
        )
        val associationId = userLogbookDao.insertUserLogbook(userLogbook)
        return userLogbook.copy(id = associationId)
    }
    
    fun getLogbooksByUser(userId: Long): Flow<List<UserLogbook>> {
        return userLogbookDao.getLogbooksByUser(userId)
    }
    
    suspend fun getUsersByLogbook(logbookId: Long): List<UserLogbook> {
        return userLogbookDao.getUsersByLogbook(logbookId)
    }
    
    suspend fun getUserLogbookAssociation(userId: Long, logbookId: Long): UserLogbook? {
        return userLogbookDao.getUserLogbookAssociation(userId, logbookId)
    }
    
    suspend fun updateUserLogbookRole(userId: Long, logbookId: Long, newRole: LogbookRole) {
        val association = getUserLogbookAssociation(userId, logbookId)
        if (association != null) {
            val updatedAssociation = association.copy(
                role = newRole,
                canEdit = newRole in listOf(LogbookRole.OWNER, LogbookRole.EDITOR),
                canDelete = newRole == LogbookRole.OWNER,
                canExport = newRole in listOf(LogbookRole.OWNER, LogbookRole.EDITOR, LogbookRole.VIEWER)
            )
            userLogbookDao.updateUserLogbook(updatedAssociation)
        }
    }
    
    suspend fun removeUserFromLogbook(userId: Long, logbookId: Long) {
        val association = getUserLogbookAssociation(userId, logbookId)
        if (association != null) {
            userLogbookDao.deleteUserLogbook(association)
        }
    }
    
    suspend fun deleteAllAssociationsForLogbook(logbookId: Long) {
        userLogbookDao.deleteAllAssociationsForLogbook(logbookId)
    }
    
    // ===== SESSION MANAGEMENT =====
    
    suspend fun createSession(userId: Long, expiresInHours: Int = 24): UserSession {
        val sessionToken = UUID.randomUUID().toString()
        val expiresAt = Date(System.currentTimeMillis() + (expiresInHours * 60 * 60 * 1000L))
        
        val session = UserSession(
            userId = userId,
            sessionToken = sessionToken,
            expiresAt = expiresAt
        )
        val sessionId = userSessionDao.insertSession(session)
        return session.copy(id = sessionId)
    }
    
    suspend fun getActiveSession(userId: Long): UserSession? {
        return userSessionDao.getActiveSession(userId)
    }
    
    suspend fun getSessionByToken(token: String): UserSession? {
        return userSessionDao.getSessionByToken(token)
    }
    
    suspend fun updateBiometricVerification(sessionId: Long, verified: Boolean) {
        userSessionDao.updateBiometricVerification(sessionId, verified)
    }
    
    suspend fun deactivateAllSessionsForUser(userId: Long) {
        userSessionDao.deactivateAllSessionsForUser(userId)
    }
    
    suspend fun isSessionValid(sessionToken: String): Boolean {
        val session = getSessionByToken(sessionToken)
        return session != null && session.isActive && session.expiresAt.after(Date())
    }
    
    // ===== UTILITY METHODS =====
    
    suspend fun getUserLogbooksWithDetails(userId: Long): List<UserLogbookWithDetails> {
        val userLogbooks = userLogbookDao.getLogbooksByUser(userId)
        // Esta función necesitaría ser implementada con un JOIN query más complejo
        // Por ahora retornamos una lista vacía
        return emptyList()
    }
}

/**
 * Data class para representar una bitácora con detalles del usuario
 */
data class UserLogbookWithDetails(
    val userLogbook: UserLogbook,
    val user: User,
    val logbookName: String,
    val logbookStatus: String
)
