package com.aura.substratecryptotest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.models.MilestoneDetails
import com.aura.substratecryptotest.data.services.MilestoneDetailsService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel especializado para manejar detalles completos de milestones
 * Incluye fotos, GPS y metadata
 */
class MilestoneDetailsViewModel(
    private val milestoneDetailsService: MilestoneDetailsService
) : ViewModel() {
    
    private val _milestoneDetails = MutableStateFlow<List<MilestoneDetails>>(emptyList())
    val milestoneDetails: StateFlow<List<MilestoneDetails>> = _milestoneDetails.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Carga detalles completos de milestones para una bitácora
     */
    fun loadMilestoneDetails(logbookId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                milestoneDetailsService.getMilestoneDetailsByLogbook(logbookId).collect { details ->
                    android.util.Log.d("MilestoneDetailsViewModel", "Cargando detalles de ${details.size} milestones")
                    details.forEach { detail ->
                        android.util.Log.d("MilestoneDetailsViewModel", 
                            "Milestone: ${detail.milestone.title} - Fotos: ${detail.photos.size} - GPS: ${detail.gpsData != null}")
                    }
                    _milestoneDetails.value = details
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MilestoneDetailsViewModel", "Error cargando detalles: ${e.message}", e)
                _error.value = "Error cargando detalles: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Obtiene detalles de un milestone específico
     */
    fun getMilestoneDetails(milestoneId: Long): MilestoneDetails? {
        return _milestoneDetails.value.find { it.milestone.id == milestoneId }
    }
    
    /**
     * Obtiene todas las fotos de una bitácora
     */
    fun getAllPhotosForLogbook(logbookId: Long): List<com.aura.substratecryptotest.data.models.PhotoDetails> {
        return _milestoneDetails.value
            .filter { it.milestone.logbookId == logbookId }
            .flatMap { it.photos }
    }
    
    /**
     * Obtiene estadísticas de la bitácora
     */
    fun getLogbookStats(logbookId: Long): LogbookStats {
        val milestones = _milestoneDetails.value.filter { it.milestone.logbookId == logbookId }
        val totalPhotos = milestones.sumOf { it.photos.size }
        val milestonesWithGps = milestones.count { it.gpsData != null }
        val milestonesWithPhotos = milestones.count { it.photos.isNotEmpty() }
        
        return LogbookStats(
            totalMilestones = milestones.size,
            totalPhotos = totalPhotos,
            milestonesWithGps = milestonesWithGps,
            milestonesWithPhotos = milestonesWithPhotos,
            completionPercentage = if (milestones.isNotEmpty()) {
                (milestones.count { !it.milestone.isDraft } * 100) / milestones.size
            } else 0
        )
    }
}

/**
 * Estadísticas de una bitácora
 */
data class LogbookStats(
    val totalMilestones: Int,
    val totalPhotos: Int,
    val milestonesWithGps: Int,
    val milestonesWithPhotos: Int,
    val completionPercentage: Int
)



