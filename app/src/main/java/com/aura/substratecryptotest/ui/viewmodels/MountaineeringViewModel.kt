package com.aura.substratecryptotest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.mountaineering.*
import com.aura.substratecryptotest.data.location.GPSManager
import com.aura.substratecryptotest.data.location.LocationData
import com.aura.substratecryptotest.data.pdf.PDFManager
import com.aura.substratecryptotest.data.user.UserManagementService
import com.aura.substratecryptotest.data.user.LogbookRole
import com.aura.substratecryptotest.data.models.MilestoneDetails
import com.aura.substratecryptotest.data.models.GpsDetails
import com.aura.substratecryptotest.data.models.PhotoDetails
import com.aura.substratecryptotest.data.models.PhotoDimensions
import com.aura.substratecryptotest.data.models.GpsSource
import com.aura.substratecryptotest.data.models.MilestoneMetadata
import com.aura.substratecryptotest.ui.screens.mountaineering.LogbookData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.io.File

/**
 * ViewModel para gestionar las bitácoras de alpinismo
 */
class MountaineeringViewModel(
    private val repository: MountaineeringRepository,
    private val gpsManager: GPSManager,
    private val pdfManager: PDFManager,
    private val userManagementService: UserManagementService
) : ViewModel() {
    
    // ===== STATE =====
    private val _logbooks = MutableStateFlow<List<MountaineeringLogbook>>(emptyList())
    val logbooks: StateFlow<List<MountaineeringLogbook>> = _logbooks.asStateFlow()
    
    private val _activeLogbooks = MutableStateFlow<List<MountaineeringLogbook>>(emptyList())
    val activeLogbooks: StateFlow<List<MountaineeringLogbook>> = _activeLogbooks.asStateFlow()
    
    private val _allLogbooks = MutableStateFlow<List<MountaineeringLogbook>>(emptyList())
    val allLogbooks: StateFlow<List<MountaineeringLogbook>> = _allLogbooks.asStateFlow()
    
    private val _currentLogbook = MutableStateFlow<MountaineeringLogbook?>(null)
    val currentLogbook: StateFlow<MountaineeringLogbook?> = _currentLogbook.asStateFlow()
    
    private val _milestones = MutableStateFlow<List<ExpeditionMilestone>>(emptyList())
    val milestones: StateFlow<List<ExpeditionMilestone>> = _milestones.asStateFlow()
    
    private val _milestoneCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val milestoneCounts: StateFlow<Map<Long, Int>> = _milestoneCounts.asStateFlow()
    
    private val _draftMilestones = MutableStateFlow<List<ExpeditionMilestone>>(emptyList())
    val draftMilestones: StateFlow<List<ExpeditionMilestone>> = _draftMilestones.asStateFlow()
    
    private val _photos = MutableStateFlow<List<ExpeditionPhoto>>(emptyList())
    val photos: StateFlow<List<ExpeditionPhoto>> = _photos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadLogbooks()
        loadActiveLogbooks()
        loadAllLogbooks()
        loadMilestoneCounts()
    }
    
    // ===== LOGBOOK OPERATIONS =====
    fun createLogbook(logbookData: LogbookData) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MountaineeringViewModel", "Iniciando creación de bitácora: ${logbookData.name}")
                _isLoading.value = true
                _error.value = null
                
                val logbook = MountaineeringLogbook(
                    name = logbookData.name,
                    club = logbookData.club,
                    association = logbookData.association,
                    participantsCount = logbookData.participantsCount,
                    licenseNumber = logbookData.licenseNumber,
                    startDate = logbookData.startDate,
                    endDate = logbookData.endDate,
                    location = logbookData.location,
                    observations = logbookData.observations,
                    isCompleted = false
                )
                
                android.util.Log.d("MountaineeringViewModel", "Creando bitácora en repositorio...")
                val logbookId = repository.createLogbook(logbook)
                android.util.Log.d("MountaineeringViewModel", "Bitácora creada con ID: $logbookId")
                
                // Asociar la bitácora con el usuario actual
                try {
                    val currentUser = userManagementService.getCurrentUser()
                    if (currentUser != null) {
                        val success = userManagementService.associateLogbookWithCurrentUser(
                            logbookId = logbookId,
                            role = LogbookRole.OWNER
                        )
                        if (success) {
                            android.util.Log.d("MountaineeringViewModel", "Bitácora $logbookId asociada con usuario ${currentUser.name}")
                        } else {
                            android.util.Log.w("MountaineeringViewModel", "No se pudo asociar bitácora con usuario")
                        }
                    } else {
                        android.util.Log.w("MountaineeringViewModel", "No hay usuario actual para asociar bitácora")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MountaineeringViewModel", "Error asociando bitácora con usuario: ${e.message}", e)
                }
                
                // Recargar bitácoras del usuario actual
                val currentUser = userManagementService.getCurrentUser()
                loadUserLogbooks(currentUser?.id?.toString())
                loadActiveLogbooks()
                
            } catch (e: Exception) {
                android.util.Log.e("MountaineeringViewModel", "Error al crear bitácora: ${e.message}", e)
                _error.value = "Error al crear bitácora: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectLogbook(logbookId: Long) {
        viewModelScope.launch {
            try {
                val logbook = repository.getLogbookById(logbookId)
                _currentLogbook.value = logbook
                if (logbook != null) {
                    loadMilestones(logbookId)
                    loadPhotos(logbookId)
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar bitácora: ${e.message}"
            }
        }
    }
    
    fun completeLogbook(logbookId: Long, pdfPath: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MountaineeringViewModel", "Completando bitácora ID: $logbookId con PDF: $pdfPath")
                repository.completeLogbook(logbookId, pdfPath)
                android.util.Log.d("MountaineeringViewModel", "Bitácora completada exitosamente")
                loadLogbooks()
                loadActiveLogbooks()
            } catch (e: Exception) {
                android.util.Log.e("MountaineeringViewModel", "Error al completar bitácora: ${e.message}", e)
                _error.value = "Error al completar bitácora: ${e.message}"
            }
        }
    }
    
    fun signLogbook(logbookId: Long, did: String) {
        viewModelScope.launch {
            try {
                repository.signLogbook(logbookId, did)
                loadLogbooks()
            } catch (e: Exception) {
                _error.value = "Error al firmar bitácora: ${e.message}"
            }
        }
    }
    
    fun deleteLogbook(logbook: MountaineeringLogbook) {
        viewModelScope.launch {
            try {
                repository.deleteLogbook(logbook)
                loadLogbooks()
                loadActiveLogbooks()
                
                // Si era la bitácora actual, limpiarla
                if (_currentLogbook.value?.id == logbook.id) {
                    _currentLogbook.value = null
                    _milestones.value = emptyList()
                    _photos.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar bitácora: ${e.message}"
            }
        }
    }
    
    // ===== MILESTONE OPERATIONS =====
    fun addMilestone(
        logbookId: Long,
        title: String,
        description: String,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null,
        isDraft: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MountaineeringViewModel", "Agregando milestone: $title para bitácora $logbookId")
                
                val milestone = ExpeditionMilestone(
                    logbookId = logbookId,
                    title = title,
                    description = description,
                    timestamp = Date(),
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    isDraft = isDraft
                )
                
                android.util.Log.d("MountaineeringViewModel", "Milestone creado: ${milestone.title}")
                repository.addMilestone(milestone)
                android.util.Log.d("MountaineeringViewModel", "Milestone guardado en repositorio")
                
                loadMilestones(logbookId)
                loadDraftMilestones(logbookId)
                loadMilestoneCounts() // Recargar conteos
                
                android.util.Log.d("MountaineeringViewModel", "Milestone agregado exitosamente")
                
            } catch (e: Exception) {
                android.util.Log.e("MountaineeringViewModel", "Error al agregar milestone: ${e.message}", e)
                _error.value = "Error al agregar milestone: ${e.message}"
            }
        }
    }
    
    fun saveDraftMilestone(
        logbookId: Long,
        title: String,
        description: String,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null
    ) {
        addMilestone(logbookId, title, description, latitude, longitude, altitude, isDraft = true)
    }
    
    fun finalizeDraftMilestones(logbookId: Long) {
        viewModelScope.launch {
            try {
                repository.finalizeDraftMilestones(logbookId)
                loadMilestones(logbookId)
                loadDraftMilestones(logbookId)
            } catch (e: Exception) {
                _error.value = "Error al finalizar borradores: ${e.message}"
            }
        }
    }
    
    fun deleteDraftMilestones(logbookId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteDraftMilestones(logbookId)
                loadDraftMilestones(logbookId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar borradores: ${e.message}"
            }
        }
    }
    
    fun updateMilestone(milestone: ExpeditionMilestone) {
        viewModelScope.launch {
            try {
                repository.updateMilestone(milestone)
                loadMilestones(milestone.logbookId)
            } catch (e: Exception) {
                _error.value = "Error al actualizar milestone: ${e.message}"
            }
        }
    }
    
    fun deleteMilestone(milestone: ExpeditionMilestone) {
        viewModelScope.launch {
            try {
                repository.deleteMilestone(milestone)
                loadMilestones(milestone.logbookId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar milestone: ${e.message}"
            }
        }
    }
    
    // ===== PHOTO OPERATIONS =====
    fun addPhoto(
        milestoneId: Long,
        photoPath: String,
        photoType: PhotoType,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val photo = ExpeditionPhoto(
                    milestoneId = milestoneId,
                    photoPath = photoPath,
                    photoType = photoType,
                    timestamp = Date(),
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude
                )
                
                repository.addPhoto(photo)
                loadPhotos(milestoneId)
                
            } catch (e: Exception) {
                _error.value = "Error al agregar foto: ${e.message}"
            }
        }
    }
    
    fun deletePhoto(photo: ExpeditionPhoto) {
        viewModelScope.launch {
            try {
                repository.deletePhoto(photo)
                loadPhotos(photo.milestoneId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar foto: ${e.message}"
            }
        }
    }
    
    // ===== LOADING OPERATIONS =====
    private fun loadLogbooks() {
        viewModelScope.launch {
            repository.getAllLogbooks().collect { logbooks ->
                _logbooks.value = logbooks
            }
        }
    }
    
    /**
     * Carga bitácoras específicas del usuario actual
     * Solo muestra bitácoras asociadas al usuario autenticado
     */
    fun loadUserLogbooks(currentUserId: String?) {
        viewModelScope.launch {
            try {
                if (currentUserId != null) {
                    // Obtener bitácoras del usuario actual usando UserManagementService
                    val userLogbooksFlow = userManagementService.getCurrentUserLogbooks()
                    userLogbooksFlow.collect { userLogbooks ->
                        // Obtener detalles de las bitácoras del usuario
                        val userLogbookDetails = userLogbooks.mapNotNull { userLogbook ->
                            repository.getLogbookById(userLogbook.logbookId)
                        }
                        _logbooks.value = userLogbookDetails
                        android.util.Log.d("MountaineeringViewModel", "Bitácoras del usuario cargadas: ${userLogbookDetails.size}")
                    }
                } else {
                    // Si no hay usuario, mostrar lista vacía
                    _logbooks.value = emptyList()
                    android.util.Log.w("MountaineeringViewModel", "No hay usuario activo, lista vacía")
                }
            } catch (e: Exception) {
                android.util.Log.e("MountaineeringViewModel", "Error cargando bitácoras del usuario: ${e.message}", e)
                _error.value = "Error cargando bitácoras del usuario: ${e.message}"
            }
        }
    }
    
    private fun loadActiveLogbooks() {
        viewModelScope.launch {
            android.util.Log.d("MountaineeringViewModel", "Cargando bitácoras activas...")
            repository.getActiveLogbooks().collect { logbooks ->
                android.util.Log.d("MountaineeringViewModel", "Bitácoras activas cargadas: ${logbooks.size}")
                logbooks.forEach { logbook ->
                    android.util.Log.d("MountaineeringViewModel", "Bitácora activa: ${logbook.name} (ID: ${logbook.id}) - Completada: ${logbook.isCompleted}")
                }
                _activeLogbooks.value = logbooks
            }
        }
    }
    
    private fun loadAllLogbooks() {
        viewModelScope.launch {
            android.util.Log.d("MountaineeringViewModel", "Cargando todas las bitácoras...")
            repository.getAllLogbooks().collect { logbooks ->
                android.util.Log.d("MountaineeringViewModel", "Todas las bitácoras cargadas: ${logbooks.size}")
                logbooks.forEach { logbook ->
                    android.util.Log.d("MountaineeringViewModel", "Bitácora: ${logbook.name} (ID: ${logbook.id}) - Completada: ${logbook.isCompleted}")
                }
                _allLogbooks.value = logbooks
            }
        }
    }
    
    fun loadMilestones(logbookId: Long) {
        viewModelScope.launch {
            android.util.Log.d("MountaineeringViewModel", "Cargando milestones para bitácora $logbookId")
            repository.getMilestonesByLogbook(logbookId).collect { milestones ->
                android.util.Log.d("MountaineeringViewModel", "Milestones cargados: ${milestones.size} para bitácora $logbookId")
                milestones.forEach { milestone ->
                    android.util.Log.d("MountaineeringViewModel", "Milestone: ${milestone.title} - Draft: ${milestone.isDraft}")
                }
                _milestones.value = milestones
            }
        }
    }
    
    private fun loadDraftMilestones(logbookId: Long) {
        viewModelScope.launch {
            repository.getDraftMilestonesByLogbook(logbookId).collect { draftMilestones ->
                _draftMilestones.value = draftMilestones
            }
        }
    }
    
    private fun loadPhotos(logbookId: Long) {
        viewModelScope.launch {
            repository.getPhotosByLogbook(logbookId).collect { photos ->
                _photos.value = photos
            }
        }
    }
    
    private fun loadMilestoneCounts() {
        viewModelScope.launch {
            repository.getAllLogbooks().collect { logbooks ->
                val counts = mutableMapOf<Long, Int>()
                logbooks.forEach { logbook ->
                    try {
                        repository.getCompletedMilestonesByLogbook(logbook.id).take(1).collect { milestones ->
                            counts[logbook.id] = milestones.size
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MountaineeringViewModel", "Error cargando milestones para logbook ${logbook.id}: ${e.message}")
                        counts[logbook.id] = 0
                    }
                }
                _milestoneCounts.value = counts
                android.util.Log.d("MountaineeringViewModel", "Conteos de milestones actualizados: $counts")
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // ===== GPS OPERATIONS =====
    suspend fun getCurrentLocation(): LocationData? {
        return try {
            gpsManager.getCurrentLocation()
        } catch (e: Exception) {
            _error.value = "Error al obtener ubicación: ${e.message}"
            null
        }
    }
    
    suspend fun getLastKnownLocation(): LocationData? {
        return try {
            gpsManager.getLastKnownLocation()
        } catch (e: Exception) {
            _error.value = "Error al obtener última ubicación conocida: ${e.message}"
            null
        }
    }
    
    fun hasLocationPermission(): Boolean {
        return gpsManager.hasLocationPermission()
    }
    
    // ===== PDF EXPORT =====
    /**
     * Genera una vista previa temporal de una bitácora en PDF (no se guarda permanentemente)
     * Esta función es para mostrar una vista previa antes de guardar definitivamente
     */
    suspend fun generateLogbookPreview(logbookId: Long): File? {
        return try {
            val logbook = _allLogbooks.value.find { it.id == logbookId }
            if (logbook == null) {
                _error.value = "Bitácora no encontrada"
                return null
            }
            
            android.util.Log.d("MountaineeringViewModel", "📄 Generando vista previa de PDF para bitácora ${logbookId}")
            val milestones = repository.getMilestonesByLogbook(logbookId).first()
            val photos = repository.getPhotosByLogbook(logbookId).first()
            
            pdfManager.generateLogbookPreview(logbook, milestones, photos)
        } catch (e: Exception) {
            _error.value = "Error al generar vista previa: ${e.message}"
            null
        }
    }
    
    /**
     * Exporta una bitácora a PDF y lo guarda permanentemente
     * Si ya existe un PDF exportado, lo retorna sin generar uno nuevo
     */
    suspend fun exportLogbookToPDF(logbookId: Long): File? {
        return try {
            val logbook = _allLogbooks.value.find { it.id == logbookId }
            if (logbook == null) {
                _error.value = "Bitácora no encontrada"
                return null
            }
            
            // Verificar si ya existe un PDF exportado
            val existingPDF = pdfManager.hasExportedPDF(logbookId, logbook.name)
            if (existingPDF != null) {
                android.util.Log.d("MountaineeringViewModel", "📄 Retornando PDF existente: ${existingPDF.name}")
                return existingPDF
            }
            
            android.util.Log.d("MountaineeringViewModel", "📄 Generando nuevo PDF para bitácora ${logbookId}")
            val milestones = repository.getMilestonesByLogbook(logbookId).first()
            val photos = repository.getPhotosByLogbook(logbookId).first()
            
            pdfManager.generateLogbookPDF(logbook, milestones, photos)
        } catch (e: Exception) {
            _error.value = "Error al exportar PDF: ${e.message}"
            null
        }
    }
    
    /**
     * Regenera un PDF de bitácora (útil para actualizar contenido)
     */
    suspend fun regenerateLogbookPDF(logbookId: Long): File? {
        return try {
            val logbook = _allLogbooks.value.find { it.id == logbookId }
            if (logbook == null) {
                _error.value = "Bitácora no encontrada"
                return null
            }
            
            android.util.Log.d("MountaineeringViewModel", "🔄 Regenerando PDF para bitácora ${logbookId}")
            val milestones = repository.getMilestonesByLogbook(logbookId).first()
            val photos = repository.getPhotosByLogbook(logbookId).first()
            
            pdfManager.regenerateLogbookPDF(logbook, milestones, photos)
        } catch (e: Exception) {
            _error.value = "Error al regenerar PDF: ${e.message}"
            null
        }
    }
    
    /**
     * Verifica si ya existe un PDF exportado para una bitácora
     */
    fun hasExportedPDF(logbookId: Long): File? {
        val logbook = _allLogbooks.value.find { it.id == logbookId }
        return if (logbook != null) {
            pdfManager.hasExportedPDF(logbookId, logbook.name)
        } else {
            null
        }
    }
}
