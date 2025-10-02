package com.aura.substratecryptotest.data.mountaineering

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad principal para la bitácora de alpinismo
 */
@Entity(tableName = "mountaineering_logbooks")
data class MountaineeringLogbook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val signedByDID: String? = null
)

/**
 * Entidad para los milestones de la expedición
 */
@Entity(tableName = "expedition_milestones")
data class ExpeditionMilestone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
 * Entidad para las fotos de la expedición
 */
@Entity(tableName = "expedition_photos")
data class ExpeditionPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
