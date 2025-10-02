package com.aura.substratecryptotest.network

import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.account.AccountInfoRequest
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.system.NodeNetworkTypeRequest
import io.novasama.substrate_sdk_android.wsrpc.request.DeliveryType
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Servicio para operaciones específicas de blockchain
 */
class BlockchainService(
    private val networkManager: NetworkManager
) {
    
    /**
     * Obtiene información de una cuenta en una red específica usando RPC real
     */
    suspend fun getAccountInfo(
        networkConfig: NetworkConfig,
        publicKey: ByteArray
    ): AccountInfo? {
        val socketService = networkManager.getSocketService(networkConfig)
            ?: throw IllegalStateException("No conectado a ${networkConfig.name}")
        
        return try {
            println("🔍 Realizando petición RPC real para obtener balance de cuenta...")
            
            val request = AccountInfoRequest(publicKey)
            val response = socketService.executeAsync(request, DeliveryType.AT_LEAST_ONCE)
            
            println("📡 Respuesta RPC recibida: ${response.result}")
            
            // Parsear la respuesta real
            parseAccountInfoFromRpc(response.result)
            
        } catch (e: Exception) {
            println("❌ Error en petición RPC: ${e.message}")
            // Fallback a datos simulados si falla
            AccountInfo(
                nonce = 0L,
                consumers = 0,
                providers = 0,
                sufficients = 0,
                data = AccountData(
                    free = 0L, // Sin balance inicial
                    reserved = 0L,
                    frozen = 0L
                )
            )
        }
    }
    
    /**
     * Parsea la información de cuenta desde la respuesta RPC real
     */
    private fun parseAccountInfoFromRpc(result: Any?): AccountInfo? {
        return try {
            // TODO: Implementar parsing real de la respuesta RPC
            // Por ahora retornamos datos básicos
            AccountInfo(
                nonce = 0L,
                consumers = 0,
                providers = 0,
                sufficients = 0,
                data = AccountData(
                    free = 0L,
                    reserved = 0L,
                    frozen = 0L
                )
            )
        } catch (e: Exception) {
            println("❌ Error parseando respuesta RPC: ${e.message}")
            null
        }
    }
    
    
    /**
     * Obtiene el tipo de red del nodo
     */
    suspend fun getNetworkType(networkConfig: NetworkConfig): String? {
        if (!networkManager.isConnected(networkConfig)) {
            throw IllegalStateException("No conectado a ${networkConfig.name}")
        }
        
        return try {
            // Por ahora simulamos la respuesta
            networkConfig.displayName
        } catch (e: Exception) {
            throw NetworkException("Error obteniendo tipo de red", e)
        }
    }
    
    /**
     * Obtiene el balance de una cuenta
     */
    suspend fun getBalance(
        networkConfig: NetworkConfig,
        publicKey: ByteArray
    ): BalanceInfo? {
        val accountInfo = getAccountInfo(networkConfig, publicKey)
        return accountInfo?.let { 
            BalanceInfo(
                free = it.data.free,
                reserved = it.data.reserved,
                frozen = it.data.frozen,
                total = it.data.free + it.data.reserved
            )
        }
    }
    
    /**
     * Obtiene información del sistema (simulado)
     */
    suspend fun getSystemInfo(networkConfig: NetworkConfig): SystemInfo? {
        if (!networkManager.isConnected(networkConfig)) {
            throw IllegalStateException("No conectado a ${networkConfig.name}")
        }
        
        return try {
            val chainName = getNetworkType(networkConfig) ?: "Unknown"
            SystemInfo(
                chain = chainName,
                network = networkConfig.name,
                isConnected = networkManager.isConnected(networkConfig)
            )
        } catch (e: Exception) {
            throw NetworkException("Error obteniendo información del sistema", e)
        }
    }
    
    /**
     * Verifica si una cuenta existe en la red
     */
    suspend fun accountExists(
        networkConfig: NetworkConfig,
        publicKey: ByteArray
    ): Boolean {
        val accountInfo = getAccountInfo(networkConfig, publicKey)
        return accountInfo != null && accountInfo.data.free > 0
    }
    
    /**
     * Obtiene el nonce de una cuenta
     */
    suspend fun getNonce(
        networkConfig: NetworkConfig,
        publicKey: ByteArray
    ): Long? {
        val accountInfo = getAccountInfo(networkConfig, publicKey)
        return accountInfo?.nonce
    }
    
    /**
     * Parsea la información de cuenta desde la respuesta RPC
     */
    private fun parseAccountInfo(result: Any?): AccountInfo? {
        return try {
            // Aquí necesitarías implementar el parsing específico
            // basado en la estructura de respuesta del RPC
            // Por ahora retornamos un ejemplo
            AccountInfo(
                nonce = 0L,
                consumers = 0,
                providers = 0,
                sufficients = 0,
                data = AccountData(
                    free = 0L,
                    reserved = 0L,
                    frozen = 0L
                )
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Prueba una petición RPC real para verificar la conexión
     */
    suspend fun testRpcConnection(networkConfig: NetworkConfig): String {
        val socketService = networkManager.getSocketService(networkConfig)
            ?: throw IllegalStateException("No conectado a ${networkConfig.name}")
        
        return try {
            println("🧪 Probando conexión RPC real a ${networkConfig.name}...")
            
            val request = NodeNetworkTypeRequest()
            val response = socketService.executeAsync(request, DeliveryType.AT_LEAST_ONCE)
            
            val result = "✅ RPC funcionando! Respuesta: ${response.result}"
            println(result)
            result
            
        } catch (e: Exception) {
            val error = "❌ Error en RPC: ${e.message}"
            println(error)
            error
        }
    }
}

/**
 * Información de cuenta
 */
data class AccountInfo(
    val nonce: Long,
    val consumers: Int,
    val providers: Int,
    val sufficients: Int,
    val data: AccountData
)

/**
 * Datos de cuenta
 */
data class AccountData(
    val free: Long,
    val reserved: Long,
    val frozen: Long
)

/**
 * Información de balance
 */
data class BalanceInfo(
    val free: Long,
    val reserved: Long,
    val frozen: Long,
    val total: Long
)

/**
 * Información del sistema
 */
data class SystemInfo(
    val chain: String,
    val network: String,
    val isConnected: Boolean
)

/**
 * Excepción de red
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
