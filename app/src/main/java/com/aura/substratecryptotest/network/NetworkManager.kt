package com.aura.substratecryptotest.network

import android.content.Context
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.recovery.Reconnector
import io.novasama.substrate_sdk_android.wsrpc.recovery.ConstantReconnectStrategy
import io.novasama.substrate_sdk_android.wsrpc.request.RequestExecutor
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import com.google.gson.Gson
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Manager para gestionar conexiones WebSocket a diferentes parachains
 */
class NetworkManager(private val context: Context) {
    
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
    private val requestExecutor = RequestExecutor(Executors.newCachedThreadPool())
    private val gson = Gson()
    private val webSocketFactory = WebSocketFactory()
    
    // Estado de conexiones por red
    private val _connectionStates = MutableStateFlow<Map<NetworkConfig, ConnectionState>>(emptyMap())
    val connectionStates: StateFlow<Map<NetworkConfig, ConnectionState>> = _connectionStates.asStateFlow()
    
    // Servicios de socket por red
    private val socketServices = mutableMapOf<NetworkConfig, SocketService>()
    
    // Logger personalizado
    private val logger = object : Logger {
        override fun log(message: String?) {
            android.util.Log.d("WebSocketRPC", message ?: "")
            println("🔗 WebSocketRPC: ${message ?: ""}")
        }
        
        override fun log(throwable: Throwable?) {
            android.util.Log.e("WebSocketRPC", "Error", throwable)
            println("❌ WebSocketRPC Error: ${throwable?.message}")
        }
    }
    
    /**
     * Conecta a una red específica usando WebSocket real
     */
    suspend fun connectToNetwork(networkConfig: NetworkConfig) {
        try {
            logger.log("🚀 Iniciando conexión a ${networkConfig.name} (${networkConfig.wsUrl})")
            
            // Crear servicio de socket
            val socketService = createSocketService(networkConfig)
            socketServices[networkConfig] = socketService
            
            // Actualizar estado de conexión
            updateConnectionState(networkConfig, ConnectionState.CONNECTING)
            logger.log("📡 Estado: CONNECTING para ${networkConfig.name}")
            
            // Conectar usando el SDK real
            logger.log("🔌 Iniciando WebSocket a ${networkConfig.wsUrl}")
            socketService.start(networkConfig.wsUrl)
            
            // Esperar a que la conexión se establezca
            logger.log("⏳ Esperando establecimiento de conexión...")
            kotlinx.coroutines.delay(3000) // Aumentamos el tiempo de espera
            
            // Verificar si la conexión fue exitosa
            val isConnected = socketService.started()
            logger.log("🔍 Verificando conexión: started() = $isConnected")
            
            if (isConnected) {
                updateConnectionState(networkConfig, ConnectionState.CONNECTED)
                logger.log("✅ CONECTADO exitosamente a ${networkConfig.name}")
            } else {
                updateConnectionState(networkConfig, ConnectionState.DISCONNECTED)
                logger.log("❌ FALLO: No se pudo conectar a ${networkConfig.name}")
            }
            
        } catch (e: Exception) {
            updateConnectionState(networkConfig, ConnectionState.DISCONNECTED)
            logger.log("💥 ERROR conectando a ${networkConfig.name}: ${e.message}")
            logger.log(e)
            throw e
        }
    }
    
    /**
     * Desconecta de una red específica
     */
    suspend fun disconnectFromNetwork(networkConfig: NetworkConfig) {
        logger.log("🔌 Desconectando de ${networkConfig.name}")
        socketServices[networkConfig]?.stop()
        socketServices.remove(networkConfig)
        updateConnectionState(networkConfig, ConnectionState.DISCONNECTED)
        logger.log("✅ Desconectado de ${networkConfig.name}")
    }
    
    /**
     * Conecta a múltiples redes
     */
    suspend fun connectToNetworks(networkConfigs: List<NetworkConfig>) {
        networkConfigs.forEach { config ->
            try {
                connectToNetwork(config)
            } catch (e: Exception) {
                logger.log("Error conectando a ${config.name}: ${e.message}")
            }
        }
    }
    
    /**
     * Obtiene el servicio de socket para una red específica
     */
    fun getSocketService(networkConfig: NetworkConfig): SocketService? {
        return socketServices[networkConfig]
    }
    
    /**
     * Verifica si una red está conectada
     */
    fun isConnected(networkConfig: NetworkConfig): Boolean {
        return getConnectionState(networkConfig) == ConnectionState.CONNECTED
    }
    
    /**
     * Obtiene el estado de conexión de una red
     */
    fun getConnectionState(networkConfig: NetworkConfig): ConnectionState {
        return _connectionStates.value[networkConfig] ?: ConnectionState.DISCONNECTED
    }
    
    /**
     * Crea un servicio de socket para una configuración de red
     */
    private fun createSocketService(networkConfig: NetworkConfig): SocketService {
        val reconnector = createReconnector(networkConfig)
        
        return SocketService(
            jsonMapper = gson,
            logger = logger,
            webSocketFactory = webSocketFactory,
            reconnector = reconnector,
            requestExecutor = requestExecutor
        )
    }
    
    /**
     * Crea un reconnector para la red
     */
    private fun createReconnector(networkConfig: NetworkConfig): Reconnector {
        val reconnectStrategy = ConstantReconnectStrategy(1000L) // 1 segundo
        
        return Reconnector(reconnectStrategy, executor)
    }
    
    /**
     * Actualiza el estado de conexión de una red
     */
    private fun updateConnectionState(networkConfig: NetworkConfig, state: ConnectionState) {
        val currentStates = _connectionStates.value.toMutableMap()
        currentStates[networkConfig] = state
        _connectionStates.value = currentStates
    }
    
    /**
     * Limpia todos los recursos
     */
    fun cleanup() {
        socketServices.values.forEach { it.stop() }
        socketServices.clear()
        executor.shutdown()
    }
}

/**
 * Configuración de red para una parachain
 */
data class NetworkConfig(
    val name: String,
    val displayName: String,
    val wsUrl: String,
    val rpcUrl: String,
    val ss58Prefix: Int,
    val isTestnet: Boolean = false,
    val maxReconnectAttempts: Int = 5,
    val baseReconnectDelayMs: Long = 1000,
    val maxReconnectDelayMs: Long = 30000,
    val description: String = ""
)

/**
 * Estados de conexión
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}
