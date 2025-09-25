package com.aura.substratecryptotest.api.models

/**
 * Modelos de datos para comunicación con el servidor DID
 * Basados en los schemas del servidor Beta/backend
 */

// ===== RESPONSE MODELS =====

/**
 * Respuesta base de la API
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

/**
 * Respuesta de health check
 */
data class HealthResponse(
    val status: String,
    val message: String,
    val timestamp: String,
    val routes: List<String>
)

/**
 * Respuesta de almacenamiento de firma
 */
data class StoreSignatureResponse(
    val signatureStored: Boolean,
    val contractCompleted: Boolean,
    val consolidatedSignature: ConsolidatedSignature? = null
)

/**
 * Respuesta de estado de firmas
 */
data class SignatureStatusResponse(
    val contractId: String,
    val contractName: String,
    val totalSigners: Int,
    val signedCount: Int,
    val isComplete: Boolean,
    val signatures: List<SignatureInfo>
)

/**
 * Información de una firma individual
 */
data class SignatureInfo(
    val did: String,
    val signedAt: String,
    val fileName: String
)

/**
 * Respuesta de verificación de firma
 */
data class VerifySignatureResponse(
    val isValid: Boolean,
    val fileName: String,
    val details: SignatureDetails
)

/**
 * Detalles de una firma verificada
 */
data class SignatureDetails(
    val did: String,
    val timestamp: String,
    val credentials: List<String>,
    val blockchainTimestamp: String?
)

/**
 * Respuesta de estadísticas
 */
data class SignatureStatsResponse(
    val contractsWithDidSignatures: Int,
    val userDidSignatures: Int,
    val totalContracts: Int
)

/**
 * Respuesta de timestamp blockchain
 */
data class TimestampResponse(
    val timestamp: String,
    val contractId: String,
    val userDid: String,
    val documentHash: String
)

/**
 * Firma consolidada
 */
data class ConsolidatedSignature(
    val contractId: String,
    val signatures: List<SignatureInfo>,
    val consolidatedAt: String,
    val hash: String
)

// ===== REQUEST MODELS =====

/**
 * Request para almacenar firma
 */
data class StoreSignatureRequest(
    val contractId: String,
    val userDid: String,
    val fileName: String? = null
)

/**
 * Request para verificar firma
 */
data class VerifySignatureRequest(
    val fileName: String? = null
)

/**
 * Request para generar timestamp
 */
data class TimestampRequest(
    val contractId: String,
    val userDid: String,
    val documentHash: String
)

// ===== ERROR MODELS =====

/**
 * Error de la API
 */
data class ApiError(
    val code: Int,
    val message: String,
    val details: String? = null
)

/**
 * Excepción personalizada de la API
 */
class ApiException(
    val error: ApiError,
    message: String? = null
) : Exception(message ?: error.message)
