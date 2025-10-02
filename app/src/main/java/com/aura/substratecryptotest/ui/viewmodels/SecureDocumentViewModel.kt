package com.aura.substratecryptotest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.data.UserDocument
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel seguro para gestión de documentos
 * Usa SecureUserRepository con autenticación biométrica
 */
class SecureDocumentViewModel(
    private val secureUserRepository: SecureUserRepository,
    private val userManager: UserManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "SecureDocumentViewModel"
    }
    
    private val _uiState = MutableStateFlow(SecureDocumentUiState())
    val uiState: StateFlow<SecureDocumentUiState> = _uiState.asStateFlow()
    
    /**
     * Crea un documento de forma segura
     */
    fun createSecureDocument(
        walletId: String,
        documentHash: String,
        documentType: String,
        blockchainTimestamp: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Logger.debug(TAG, "Creando documento seguro", "Hash: ${documentHash.take(20)}...")
                
                val result = secureUserRepository.createUserDocument(
                    walletId = walletId,
                    documentHash = documentHash,
                    documentType = documentType,
                    blockchainTimestamp = blockchainTimestamp,
                    metadata = metadata
                )
                
                if (result.isSuccess) {
                    val document = result.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        documents = _uiState.value.documents + document!!
                    )
                    Logger.success(TAG, "Documento seguro creado", "ID: ${document.id.take(8)}...")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                    Logger.error(TAG, "Error creando documento seguro", error, null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Error desconocido"
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                Logger.error(TAG, "Excepción creando documento seguro", error, e)
            }
        }
    }
    
    /**
     * Carga los documentos del usuario actual
     */
    fun loadUserDocuments() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Observar documentos desde SecureUserRepository
                secureUserRepository.currentUserDocuments.observeForever { documents ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        documents = documents
                    )
                }
                
                Logger.success(TAG, "Documentos cargados", "Observando cambios...")
            } catch (e: Exception) {
                val error = e.message ?: "Error desconocido"
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                Logger.error(TAG, "Error cargando documentos", error, e)
            }
        }
    }
}

/**
 * Estado de la UI para SecureDocumentViewModel
 */
data class SecureDocumentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val documents: List<UserDocument> = emptyList()
)
