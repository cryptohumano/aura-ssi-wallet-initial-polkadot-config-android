package com.aura.substratecryptotest.data.models

import com.aura.substratecryptotest.data.mountaineering.ExpeditionMilestone
import com.aura.substratecryptotest.data.mountaineering.PhotoType
import java.util.Date

/**
 * Modelo completo de detalles de un milestone
 * Incluye toda la información necesaria para mostrar en el preview
 */
data class MilestoneDetails(
    val milestone: ExpeditionMilestone,
    val photos: List<PhotoDetails>,
    val gpsData: GpsDetails?,
    val metadata: MilestoneMetadata
)

/**
 * Detalles completos de una foto
 */
data class PhotoDetails(
    val id: Long,
    val filePath: String,
    val photoType: PhotoType,
    val timestamp: Date,
    val gpsData: GpsDetails?,
    val dimensions: PhotoDimensions,
    val fileSize: Long,
    val thumbnailPath: String? = null
)

/**
 * Datos GPS unificados
 */
data class GpsDetails(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float,
    val timestamp: Date,
    val source: GpsSource
)

enum class GpsSource {
    GPS_MANAGER,    // Obtenido del GPSManager
    PHOTO_EXIF,     // Extraído de EXIF de la foto
    MANUAL          // Ingresado manualmente
}

/**
 * Dimensiones de foto
 */
data class PhotoDimensions(
    val width: Int,
    val height: Int
)

/**
 * Metadata adicional del milestone
 */
data class MilestoneMetadata(
    val durationFromPrevious: Long?, // Duración desde el milestone anterior
    val weatherConditions: String?,
    val difficulty: String?,
    val notes: String?
)
