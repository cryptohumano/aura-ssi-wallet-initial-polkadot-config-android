package com.aura.substratecryptotest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import com.aura.substratecryptotest.data.UserDatabaseManager
import com.aura.substratecryptotest.data.UserMountaineeringLogbook
import com.aura.substratecryptotest.data.UserExpeditionMilestone
import com.aura.substratecryptotest.data.UserExpeditionPhoto
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.ui.screens.mountaineering.LogbookData
import java.util.Date

/**
 * ViewModel para gestión de documentos por usuario
 */
class UserDocumentsViewModel : ViewModel() {
    
    private var _userLogbooks = MutableStateFlow<List<UserMountaineeringLogbook>>(emptyList())
    val userLogbooks: StateFlow<List<UserMountaineeringLogbook>> = _userLogbooks.asStateFlow()
    
    private var _userMilestones = MutableStateFlow<List<UserExpeditionMilestone>>(emptyList())
    val userMilestones: StateFlow<List<UserExpeditionMilestone>> = _userMilestones.asStateFlow()
    
    private var _userPhotos = MutableStateFlow<List<UserExpeditionPhoto>>(emptyList())
    val userPhotos: StateFlow<List<UserExpeditionPhoto>> = _userPhotos.asStateFlow()
    
    private var _milestoneCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val milestoneCounts: StateFlow<Map<Long, Int>> = _milestoneCounts.asStateFlow()
    
    private var _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()
    
    private var userDatabaseManager: UserDatabaseManager? = null
    private var userManager: UserManager? = null
    
    fun initialize(context: Context) {
        userManager = UserManager(context)
        userDatabaseManager = UserDatabaseManager(context, userManager!!)
        
        // Obtener usuario actual
        val currentUser = userManager?.getCurrentUser()
        if (currentUser != null) {
            _currentUser.value = currentUser.id
            loadUserData(currentUser.id)
        }
    }
    
    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                val userDb = userDatabaseManager?.getUserDatabase(userId)
                if (userDb != null) {
                    // Cargar bitácoras del usuario
                    val logbooks = userDb.userMountaineeringLogbookDao().getLogbooksByUser(userId)
                    _userLogbooks.value = logbooks
                    
                    // Cargar milestones del usuario
                    val milestones = userDb.userExpeditionMilestoneDao().getMilestonesByLogbook(0, userId) // 0 = todos
                    _userMilestones.value = milestones
                    
                    // Cargar fotos del usuario
                    val photos = userDb.userExpeditionPhotoDao().getPhotosByMilestone(0, userId) // 0 = todas
                    _userPhotos.value = photos
                    
                    // Calcular conteos de milestones por bitácora
                    val counts = logbooks.associate { logbook ->
                        logbook.id to milestones.count { it.logbookId == logbook.id }
                    }
                    _milestoneCounts.value = counts
                    
                    android.util.Log.d("UserDocumentsViewModel", "=== DATOS CARGADOS PARA USUARIO $userId ===")
                    android.util.Log.d("UserDocumentsViewModel", "Bitácoras: ${logbooks.size}")
                    android.util.Log.d("UserDocumentsViewModel", "Milestones: ${milestones.size}")
                    android.util.Log.d("UserDocumentsViewModel", "Fotos: ${photos.size}")
                }
            } catch (e: Exception) {
                android.util.Log.e("UserDocumentsViewModel", "Error cargando datos del usuario", e)
            }
        }
    }
    
    fun createLogbook(logbookData: LogbookData) {
        val userId = _currentUser.value ?: return
        
        viewModelScope.launch {
            try {
                val userDb = userDatabaseManager?.getUserDatabase(userId)
                if (userDb != null) {
                    val newLogbook = UserMountaineeringLogbook(
                        userId = userId,
                        name = logbookData.name,
                        club = logbookData.club,
                        association = logbookData.association,
                        participantsCount = logbookData.participantsCount,
                        licenseNumber = logbookData.licenseNumber,
                        startDate = logbookData.startDate.time,
                        endDate = logbookData.endDate.time,
                        location = logbookData.location,
                        observations = logbookData.observations,
                        createdAt = System.currentTimeMillis(),
                        isCompleted = false
                    )
                    
                    val logbookId = userDb.userMountaineeringLogbookDao().insertLogbook(newLogbook)
                    android.util.Log.d("UserDocumentsViewModel", "Bitácora creada con ID: $logbookId")
                    
                    // Recargar datos
                    loadUserData(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserDocumentsViewModel", "Error creando bitácora", e)
            }
        }
    }
    
    fun deleteLogbook(logbook: UserMountaineeringLogbook) {
        val userId = _currentUser.value ?: return
        
        viewModelScope.launch {
            try {
                val userDb = userDatabaseManager?.getUserDatabase(userId)
                if (userDb != null) {
                    // Eliminar fotos asociadas
                    val milestones = userDb.userExpeditionMilestoneDao().getMilestonesByLogbook(logbook.id, userId)
                    milestones.forEach { milestone ->
                        userDb.userExpeditionPhotoDao().deletePhotosByMilestone(milestone.id, userId)
                    }
                    
                    // Eliminar milestones
                    userDb.userExpeditionMilestoneDao().deleteMilestonesByLogbook(logbook.id, userId)
                    
                    // Eliminar bitácora
                    userDb.userMountaineeringLogbookDao().deleteLogbook(logbook)
                    
                    android.util.Log.d("UserDocumentsViewModel", "Bitácora eliminada: ${logbook.name}")
                    
                    // Recargar datos
                    loadUserData(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserDocumentsViewModel", "Error eliminando bitácora", e)
            }
        }
    }
    
    fun completeLogbook(logbookId: Long, pdfPath: String) {
        val userId = _currentUser.value ?: return
        
        viewModelScope.launch {
            try {
                val userDb = userDatabaseManager?.getUserDatabase(userId)
                if (userDb != null) {
                    userDb.userMountaineeringLogbookDao().completeLogbook(logbookId, pdfPath, userId)
                    android.util.Log.d("UserDocumentsViewModel", "Bitácora completada: $logbookId")
                    
                    // Recargar datos
                    loadUserData(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserDocumentsViewModel", "Error completando bitácora", e)
            }
        }
    }
    
    fun finalizeDraftMilestones(logbookId: Long) {
        val userId = _currentUser.value ?: return
        
        viewModelScope.launch {
            try {
                val userDb = userDatabaseManager?.getUserDatabase(userId)
                if (userDb != null) {
                    userDb.userExpeditionMilestoneDao().finalizeDraftMilestones(logbookId, userId)
                    android.util.Log.d("UserDocumentsViewModel", "Milestones finalizados para bitácora: $logbookId")
                    
                    // Recargar datos
                    loadUserData(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserDocumentsViewModel", "Error finalizando milestones", e)
            }
        }
    }
    
    fun generateLogbookPreview(logbookId: Long): java.io.File? {
        // TODO: Implementar generación de PDF usando las nuevas entidades
        android.util.Log.d("UserDocumentsViewModel", "Generando preview para bitácora: $logbookId")
        return null
    }
    
    fun exportLogbookToPDF(logbookId: Long): java.io.File? {
        // TODO: Implementar exportación de PDF usando las nuevas entidades
        android.util.Log.d("UserDocumentsViewModel", "Exportando PDF para bitácora: $logbookId")
        return null
    }
}
