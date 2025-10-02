package com.aura.substratecryptotest.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad para bitácoras de montañismo por usuario
 * Cada usuario tiene sus propias bitácoras aisladas
 */
@Entity(tableName = "user_mountaineering_logbooks")
data class UserMountaineeringLogbook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID del usuario propietario
    val name: String,
    val club: String,
    val association: String,
    val participantsCount: Int,
    val licenseNumber: String,
    val startDate: Date,
    val endDate: Date,
    val location: String,
    val observations: String,
    val createdAt: Date = Date(),
    val isCompleted: Boolean = false,
    val pdfPath: String? = null,
    val signedByDID: String? = null,
    val walletId: String? = null // Wallet usada para firmar
)

/**
 * Entidad para milestones de expedición por usuario
 */
@Entity(tableName = "user_expedition_milestones")
data class UserExpeditionMilestone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID del usuario propietario
    val logbookId: Long,
    val title: String,
    val description: String,
    val timestamp: Date,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val photos: List<String> = emptyList(), // Rutas de las fotos
    val gpxPath: String? = null,
    val kmzPath: String? = null,
    val duration: Long? = null, // Duración en milisegundos desde el milestone anterior
    val isDraft: Boolean = false // Indica si es un borrador
)

/**
 * Entidad para fotos de expedición por usuario
 */
@Entity(tableName = "user_expedition_photos")
data class UserExpeditionPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID del usuario propietario
    val milestoneId: Long,
    val photoPath: String,
    val photoType: PhotoType, // RECORRIDO, CUMBRE, GENERAL
    val timestamp: Date,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)

enum class PhotoType {
    RECORRIDO, // Fotos del recorrido
    CUMBRE,    // Fotos de cumbre
    GENERAL    // Fotos generales
}

