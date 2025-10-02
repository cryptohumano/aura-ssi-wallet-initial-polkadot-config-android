package com.aura.substratecryptotest.data.services

import com.aura.substratecryptotest.data.mountaineering.*
import com.aura.substratecryptotest.data.models.*
import com.aura.substratecryptotest.data.location.LocationData
import com.aura.substratecryptotest.data.camera.PhotoMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Servicio unificado para manejar milestones con todos sus detalles
 * Centraliza la lógica de conversión entre diferentes tipos de datos
 */
class MilestoneDetailsService(
    private val repository: MountaineeringRepository
) {
    
    /**
     * Convierte LocationData a GpsDetails
     */
    fun convertLocationToGps(locationData: LocationData?): GpsDetails? {
        return locationData?.let {
            GpsDetails(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracy = it.accuracy,
                timestamp = it.timestamp,
                source = GpsSource.GPS_MANAGER
            )
        }
    }
    
    /**
     * Convierte PhotoMetadata a PhotoDetails
     */
    fun convertPhotoMetadataToDetails(
        photoMetadata: PhotoMetadata,
        milestoneId: Long,
        photoType: PhotoType
    ): PhotoDetails {
        val file = File(photoMetadata.filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("El archivo de foto no existe: ${photoMetadata.filePath}")
        }
        
        val gpsData = photoMetadata.location?.let { location ->
            GpsDetails(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy,
                timestamp = location.timestamp,
                source = GpsSource.PHOTO_EXIF
            )
        }
        
        return PhotoDetails(
            id = 0, // Se asignará al guardar en BD
            filePath = photoMetadata.filePath,
            photoType = photoType,
            timestamp = photoMetadata.timestamp,
            gpsData = gpsData,
            dimensions = PhotoDimensions(
                width = photoMetadata.width,
                height = photoMetadata.height
            ),
            fileSize = photoMetadata.fileSize
        )
    }
    
    /**
     * Obtiene detalles completos de milestones para una bitácora
     */
    fun getMilestoneDetailsByLogbook(logbookId: Long): Flow<List<MilestoneDetails>> {
        return repository.getMilestonesByLogbook(logbookId).map { milestones ->
            milestones.map { milestone ->
                MilestoneDetails(
                    milestone = milestone,
                    photos = getPhotosForMilestone(milestone.id),
                    gpsData = if (milestone.latitude != null && milestone.longitude != null) {
                        GpsDetails(
                            latitude = milestone.latitude,
                            longitude = milestone.longitude,
                            altitude = milestone.altitude,
                            accuracy = 0f, // No tenemos accuracy en milestone
                            timestamp = milestone.timestamp,
                            source = GpsSource.GPS_MANAGER
                        )
                    } else null,
                    metadata = MilestoneMetadata(
                        durationFromPrevious = milestone.duration,
                        weatherConditions = null, // TODO: Agregar campo
                        difficulty = null, // TODO: Agregar campo
                        notes = null // TODO: Agregar campo
                    )
                )
            }
        }
    }
    
    /**
     * Obtiene fotos para un milestone específico
     */
    private suspend fun getPhotosForMilestone(milestoneId: Long): List<PhotoDetails> {
        return try {
            // Convertir Flow a List usando first()
            repository.getPhotosByMilestone(milestoneId).first().map { photo ->
                PhotoDetails(
                    id = photo.id,
                    filePath = photo.photoPath,
                    photoType = photo.photoType,
                    timestamp = photo.timestamp,
                    gpsData = if (photo.latitude != null && photo.longitude != null) {
                        GpsDetails(
                            latitude = photo.latitude,
                            longitude = photo.longitude,
                            altitude = photo.altitude,
                            accuracy = 0f,
                            timestamp = photo.timestamp,
                            source = GpsSource.PHOTO_EXIF
                        )
                    } else null,
                    dimensions = PhotoDimensions(0, 0), // TODO: Leer de archivo
                    fileSize = File(photo.photoPath).length()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Guarda un milestone con sus fotos asociadas
     */
    suspend fun saveMilestoneWithPhotos(
        logbookId: Long,
        title: String,
        description: String,
        locationData: LocationData?,
        photoMetadataList: List<PhotoMetadata>,
        photoTypes: List<PhotoType>,
        isDraft: Boolean = false
    ): Long {
        // 1. Crear y guardar el milestone
        val milestone = ExpeditionMilestone(
            logbookId = logbookId,
            title = title,
            description = description,
            timestamp = java.util.Date(),
            latitude = locationData?.latitude,
            longitude = locationData?.longitude,
            altitude = locationData?.altitude,
            isDraft = isDraft
        )
        
        val milestoneId = repository.addMilestone(milestone)
        
        // 2. Guardar las fotos asociadas
        photoMetadataList.forEachIndexed { index, photoMetadata ->
            val photoType = photoTypes.getOrElse(index) { PhotoType.GENERAL }
            val photo = ExpeditionPhoto(
                milestoneId = milestoneId,
                photoPath = photoMetadata.filePath,
                photoType = photoType,
                timestamp = photoMetadata.timestamp,
                latitude = photoMetadata.location?.latitude,
                longitude = photoMetadata.location?.longitude,
                altitude = photoMetadata.location?.altitude
            )
            repository.addPhoto(photo)
        }
        
        return milestoneId
    }
}
