package com.aura.substratecryptotest.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad para usuarios del sistema
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String? = null,
    val walletAddress: String? = null, // Dirección de la wallet asociada
    val did: String? = null, // DID asociado
    val profileImagePath: String? = null, // Ruta de la imagen de perfil
    val createdAt: Date = Date(),
    val isActive: Boolean = true,
    val biometricEnabled: Boolean = false,
    val lastLogin: Date? = null
)

/**
 * Entidad para asociar bitácoras con usuarios
 */
@Entity(tableName = "user_logbooks")
data class UserLogbook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val logbookId: Long,
    val role: LogbookRole = LogbookRole.OWNER,
    val createdAt: Date = Date(),
    val canEdit: Boolean = true,
    val canDelete: Boolean = false,
    val canExport: Boolean = true
)

/**
 * Roles de usuario en una bitácora
 */
enum class LogbookRole {
    OWNER,      // Propietario de la bitácora
    EDITOR,     // Puede editar la bitácora
    VIEWER,     // Solo puede ver la bitácora
    COLLABORATOR // Puede agregar milestones pero no editar la bitácora
}

/**
 * Entidad para sesiones de usuario
 */
@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val sessionToken: String,
    val createdAt: Date = Date(),
    val expiresAt: Date,
    val isActive: Boolean = true,
    val biometricVerified: Boolean = false
)
