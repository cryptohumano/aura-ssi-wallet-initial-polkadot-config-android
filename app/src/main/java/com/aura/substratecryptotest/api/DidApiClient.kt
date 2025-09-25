package com.aura.substratecryptotest.api

import android.content.Context
import com.aura.substratecryptotest.api.models.*
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente API para comunicación con el servidor DID
 * Basado en los endpoints del servidor Beta/backend
 */
class DidApiClient(private val context: Context) {
    
    companion object {
        private const val TAG = "DidApiClient"
        
        // Configuración del servidor (ajustar según el entorno)
        private const val BASE_URL = "http://192.168.100.150:4001/api/did-sign"
        private const val TIMEOUT_SECONDS = 30L
        
        // Headers
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val HEADER_ACCEPT = "Accept"
        
        // Content types
        private const val CONTENT_TYPE_JSON = "application/json"
        private const val CONTENT_TYPE_MULTIPART = "multipart/form-data"
    }
    
    private var httpClient: OkHttpClient
    private var authToken: String? = null
    
    init {
        httpClient = createHttpClient()
        Logger.debug(TAG, "Cliente API inicializado", "Base URL: $BASE_URL")
    }
    
    /**
     * Configura el token de autenticación
     */
    fun setAuthToken(token: String) {
        authToken = token
        Logger.debug(TAG, "Token de autenticación configurado", "Token: ${token.take(20)}...")
    }
    
    /**
     * Limpia el token de autenticación
     */
    fun clearAuthToken() {
        authToken = null
        Logger.debug(TAG, "Token de autenticación limpiado", "Sesión cerrada")
    }
    
    // ===== ENDPOINTS PRINCIPALES =====
    
    /**
     * GET /health - Verificar estado del servidor
     */
    suspend fun checkHealth(): Result<HealthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Verificando estado del servidor", "GET /health")
                
                val request = Request.Builder()
                    .url("$BASE_URL/health")
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val healthResponse = HealthResponse(
                        status = jsonObject.getString("status"),
                        message = jsonObject.getString("message"),
                        timestamp = jsonObject.getString("timestamp"),
                        routes = jsonObject.getJSONArray("routes").let { array ->
                            (0 until array.length()).map { array.getString(it) }
                        }
                    )
                    
                    Logger.success(TAG, "Estado del servidor verificado", "Status: ${healthResponse.status}")
                    Result.success(healthResponse)
                } else {
                    Logger.error(TAG, "Error verificando estado del servidor", "Code: ${response.code}, Body: $responseBody", null)
                    Result.failure(ApiException(ApiError(response.code, "Error verificando estado del servidor")))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción verificando estado del servidor", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * POST /contracts/:contractId/signature - Almacenar firma DID
     */
    suspend fun storeSignature(
        contractId: String,
        userDid: String,
        signatureFile: File,
        fileName: String? = null
    ): Result<StoreSignatureResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Almacenando firma DID", "Contract: $contractId, DID: ${userDid.take(20)}...")
                
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("userDid", userDid)
                    .addFormDataPart("fileName", fileName ?: signatureFile.name)
                    .addFormDataPart(
                        "didSignFile",
                        signatureFile.name,
                        signatureFile.asRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()
                
                val request = Request.Builder()
                    .url("$BASE_URL/contracts/$contractId/signature")
                    .post(requestBody)
                    .addHeader(HEADER_AUTHORIZATION, "Bearer $authToken")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataObject = jsonObject.getJSONObject("data")
                    
                    val storeResponse = StoreSignatureResponse(
                        signatureStored = dataObject.getBoolean("signatureStored"),
                        contractCompleted = dataObject.getBoolean("contractCompleted"),
                        consolidatedSignature = if (dataObject.has("consolidatedSignature")) {
                            // Parsear consolidated signature si existe
                            null // TODO: Implementar parsing completo
                        } else null
                    )
                    
                    Logger.success(TAG, "Firma DID almacenada", "Stored: ${storeResponse.signatureStored}, Completed: ${storeResponse.contractCompleted}")
                    Result.success(storeResponse)
                } else {
                    Logger.error(TAG, "Error almacenando firma DID", "Code: ${response.code}, Body: $responseBody", null)
                    Result.failure(ApiException(ApiError(response.code, "Error almacenando firma DID")))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción almacenando firma DID", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * POST /verify - Verificar firma DID
     */
    suspend fun verifySignature(signatureFile: File): Result<VerifySignatureResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Verificando firma DID", "File: ${signatureFile.name}")
                
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "didSignFile",
                        signatureFile.name,
                        signatureFile.asRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()
                
                val request = Request.Builder()
                    .url("$BASE_URL/verify")
                    .post(requestBody)
                    .addHeader(HEADER_AUTHORIZATION, "Bearer $authToken")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataObject = jsonObject.getJSONObject("data")
                    val detailsObject = dataObject.getJSONObject("details")
                    
                    val verifyResponse = VerifySignatureResponse(
                        isValid = dataObject.getBoolean("isValid"),
                        fileName = dataObject.getString("fileName"),
                        details = SignatureDetails(
                            did = detailsObject.getString("did"),
                            timestamp = detailsObject.getString("timestamp"),
                            credentials = detailsObject.getJSONArray("credentials").let { array ->
                                (0 until array.length()).map { array.getString(it) }
                            },
                            blockchainTimestamp = if (detailsObject.has("blockchainTimestamp") && !detailsObject.isNull("blockchainTimestamp")) {
                                detailsObject.getString("blockchainTimestamp")
                            } else null
                        )
                    )
                    
                    Logger.success(TAG, "Firma DID verificada", "Valid: ${verifyResponse.isValid}, DID: ${verifyResponse.details.did}")
                    Result.success(verifyResponse)
                } else {
                    Logger.error(TAG, "Error verificando firma DID", "Code: ${response.code}, Body: $responseBody", null)
                    Result.failure(ApiException(ApiError(response.code, "Error verificando firma DID")))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción verificando firma DID", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * GET /stats - Obtener estadísticas de firmas
     */
    suspend fun getSignatureStats(): Result<SignatureStatsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Obteniendo estadísticas de firmas", "GET /stats")
                
                val request = Request.Builder()
                    .url("$BASE_URL/stats")
                    .get()
                    .addHeader(HEADER_AUTHORIZATION, "Bearer $authToken")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataObject = jsonObject.getJSONObject("data")
                    
                    val statsResponse = SignatureStatsResponse(
                        contractsWithDidSignatures = dataObject.getInt("contractsWithDidSignatures"),
                        userDidSignatures = dataObject.getInt("userDidSignatures"),
                        totalContracts = dataObject.getInt("totalContracts")
                    )
                    
                    Logger.success(TAG, "Estadísticas obtenidas", "Contracts: ${statsResponse.contractsWithDidSignatures}, Signatures: ${statsResponse.userDidSignatures}")
                    Result.success(statsResponse)
                } else {
                    Logger.error(TAG, "Error obteniendo estadísticas", "Code: ${response.code}, Body: $responseBody", null)
                    Result.failure(ApiException(ApiError(response.code, "Error obteniendo estadísticas")))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción obteniendo estadísticas", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * GET /contracts/:contractId/status - Obtener estado de firmas
     */
    suspend fun getSignatureStatus(contractId: String): Result<SignatureStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Obteniendo estado de firmas", "Contract: $contractId")
                
                val request = Request.Builder()
                    .url("$BASE_URL/contracts/$contractId/status")
                    .get()
                    .addHeader(HEADER_AUTHORIZATION, "Bearer $authToken")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataObject = jsonObject.getJSONObject("data")
                    
                    val signaturesArray = dataObject.getJSONArray("signatures")
                    val signatures = (0 until signaturesArray.length()).map { index ->
                        val sigObject = signaturesArray.getJSONObject(index)
                        SignatureInfo(
                            did = sigObject.getString("did"),
                            signedAt = sigObject.getString("signedAt"),
                            fileName = sigObject.getString("fileName")
                        )
                    }
                    
                    val statusResponse = SignatureStatusResponse(
                        contractId = dataObject.getString("contractId"),
                        contractName = dataObject.getString("contractName"),
                        totalSigners = dataObject.getInt("totalSigners"),
                        signedCount = dataObject.getInt("signedCount"),
                        isComplete = dataObject.getBoolean("isComplete"),
                        signatures = signatures
                    )
                    
                    Logger.success(TAG, "Estado de firmas obtenido", "Contract: ${statusResponse.contractName}, Complete: ${statusResponse.isComplete}")
                    Result.success(statusResponse)
                } else {
                    Logger.error(TAG, "Error obteniendo estado de firmas", "Code: ${response.code}, Body: $responseBody", null)
                    Result.failure(ApiException(ApiError(response.code, "Error obteniendo estado de firmas")))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción obteniendo estado de firmas", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * POST /timestamp - Generar timestamp blockchain
     */
    suspend fun generateTimestamp(
        contractId: String,
        userDid: String,
        documentHash: String
    ): Result<TimestampResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Generando timestamp blockchain", "Contract: $contractId, Hash: ${documentHash.take(20)}...")
                
                val requestBody = JSONObject().apply {
                    put("contractId", contractId)
                    put("userDid", userDid)
                    put("documentHash", documentHash)
                }.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType())
                
                val request = Request.Builder()
                    .url("$BASE_URL/timestamp")
                    .post(requestBody)
                    .addHeader(HEADER_AUTHORIZATION, "Bearer $authToken")
                    .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataObject = jsonObject.getJSONObject("data")
                    
                    val timestampResponse = TimestampResponse(
                        timestamp = dataObject.getString("timestamp"),
                        contractId = dataObject.getString("contractId"),
                        userDid = dataObject.getString("userDid"),
                        documentHash = dataObject.getString("documentHash")
                    )
                    
                    Logger.success(TAG, "Timestamp blockchain generado", "Timestamp: ${timestampResponse.timestamp}")
                    Result.success(timestampResponse)
                } else {
                    Logger.error(TAG, "Error generando timestamp blockchain", "Code: ${response.code}, Body: $responseBody", null)
                    Result.failure(ApiException(ApiError(response.code, "Error generando timestamp blockchain")))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción generando timestamp blockchain", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Crea el cliente HTTP con configuración
     */
    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Logger.debug(TAG, "HTTP Request/Response", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
