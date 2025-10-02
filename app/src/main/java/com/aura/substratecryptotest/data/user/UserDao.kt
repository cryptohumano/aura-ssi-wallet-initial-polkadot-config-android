package com.aura.substratecryptotest.data.user

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestión de usuarios
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?
    
    @Query("SELECT * FROM users WHERE walletAddress = :walletAddress")
    suspend fun getUserByWalletAddress(walletAddress: String): User?
    
    @Query("SELECT * FROM users WHERE did = :did")
    suspend fun getUserByDID(did: String): User?
    
    @Insert
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("UPDATE users SET lastLogin = :loginTime WHERE id = :id")
    suspend fun updateLastLogin(id: Long, loginTime: java.util.Date)
    
    @Query("UPDATE users SET biometricEnabled = :enabled WHERE id = :id")
    suspend fun updateBiometricEnabled(id: Long, enabled: Boolean)
}

/**
 * DAO para asociaciones usuario-bitácora
 */
@Dao
interface UserLogbookDao {
    
    @Query("SELECT * FROM user_logbooks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getLogbooksByUser(userId: Long): Flow<List<UserLogbook>>
    
    @Query("SELECT * FROM user_logbooks WHERE logbookId = :logbookId")
    suspend fun getUsersByLogbook(logbookId: Long): List<UserLogbook>
    
    @Query("SELECT * FROM user_logbooks WHERE userId = :userId AND logbookId = :logbookId")
    suspend fun getUserLogbookAssociation(userId: Long, logbookId: Long): UserLogbook?
    
    @Insert
    suspend fun insertUserLogbook(userLogbook: UserLogbook): Long
    
    @Update
    suspend fun updateUserLogbook(userLogbook: UserLogbook)
    
    @Delete
    suspend fun deleteUserLogbook(userLogbook: UserLogbook)
    
    @Query("DELETE FROM user_logbooks WHERE logbookId = :logbookId")
    suspend fun deleteAllAssociationsForLogbook(logbookId: Long)
    
    @Query("DELETE FROM user_logbooks WHERE userId = :userId")
    suspend fun deleteAllAssociationsForUser(userId: Long)
}

/**
 * DAO para sesiones de usuario
 */
@Dao
interface UserSessionDao {
    
    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSession(userId: Long): UserSession?
    
    @Query("SELECT * FROM user_sessions WHERE sessionToken = :token AND isActive = 1")
    suspend fun getSessionByToken(token: String): UserSession?
    
    @Insert
    suspend fun insertSession(session: UserSession): Long
    
    @Update
    suspend fun updateSession(session: UserSession)
    
    @Delete
    suspend fun deleteSession(session: UserSession)
    
    @Query("UPDATE user_sessions SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllSessionsForUser(userId: Long)
    
    @Query("UPDATE user_sessions SET biometricVerified = :verified WHERE id = :sessionId")
    suspend fun updateBiometricVerification(sessionId: Long, verified: Boolean)
}
