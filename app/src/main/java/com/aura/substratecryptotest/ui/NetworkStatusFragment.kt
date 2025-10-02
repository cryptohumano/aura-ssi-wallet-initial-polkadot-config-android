package com.aura.substratecryptotest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.substratecryptotest.MainActivity
import com.aura.substratecryptotest.databinding.FragmentNetworkStatusBinding
import com.aura.substratecryptotest.network.NetworkConfig
import com.aura.substratecryptotest.network.NetworkConfigs
import com.aura.substratecryptotest.network.ConnectionState
import com.aura.substratecryptotest.network.BlockchainService
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.launch

class NetworkStatusFragment : Fragment() {
    
    private var _binding: FragmentNetworkStatusBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var networkManager: com.aura.substratecryptotest.network.NetworkManager
    private lateinit var blockchainService: BlockchainService
    private lateinit var networkAdapter: NetworkStatusAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworkStatusBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener NetworkManager desde la actividad principal
        networkManager = (activity as? MainActivity)?.networkManager 
            ?: throw IllegalArgumentException("NetworkManager es requerido")
        
        blockchainService = BlockchainService(networkManager)
        
        setupUI()
        observeNetworkStates()
        loadNetworkStatus()
    }
    
    private fun setupUI() {
        // Configurar RecyclerView
        networkAdapter = NetworkStatusAdapter { networkConfig ->
            toggleNetworkConnection(networkConfig)
        }
        
        binding.recyclerViewNetworks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = networkAdapter
        }
        
        // Botones de acción
        binding.btnConnectAll.setOnClickListener {
            connectToAllNetworks()
        }
        
        binding.btnDisconnectAll.setOnClickListener {
            disconnectFromAllNetworks()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadNetworkStatus()
        }
        
        binding.btnTestRpc.setOnClickListener {
            testRpcConnections()
        }
    }
    
    /**
     * Observa los cambios en los estados de conexión
     */
    private fun observeNetworkStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkManager.connectionStates.collect { connectionStates ->
                updateNetworkStatus(connectionStates)
            }
        }
    }
    
    /**
     * Carga el estado actual de las redes
     */
    private fun loadNetworkStatus() {
        val recommendedNetworks = NetworkConfigs.getRecommendedNetworks()
        val connectionStates = networkManager.connectionStates.value
        
        val networkItems = recommendedNetworks.map { networkConfig ->
            val connectionState = connectionStates[networkConfig] ?: ConnectionState.DISCONNECTED
            NetworkStatusItem(
                networkConfig = networkConfig,
                connectionState = connectionState,
                isConnected = networkManager.isConnected(networkConfig)
            )
        }
        
        networkAdapter.submitList(networkItems)
        updateConnectionSummary(connectionStates)
    }
    
    /**
     * Actualiza el estado de las redes
     */
    private fun updateNetworkStatus(connectionStates: Map<NetworkConfig, ConnectionState>) {
        val currentItems = networkAdapter.currentList.toMutableList()
        
        connectionStates.forEach { (networkConfig, state) ->
            val index = currentItems.indexOfFirst { it.networkConfig == networkConfig }
            if (index != -1) {
                currentItems[index] = currentItems[index].copy(
                    connectionState = state,
                    isConnected = state == ConnectionState.CONNECTED
                )
            }
        }
        
        networkAdapter.submitList(currentItems)
        updateConnectionSummary(connectionStates)
    }
    
    /**
     * Actualiza el resumen de conexiones
     */
    private fun updateConnectionSummary(connectionStates: Map<NetworkConfig, ConnectionState>) {
        val total = connectionStates.size
        val connected = connectionStates.values.count { it == ConnectionState.CONNECTED }
        val connecting = connectionStates.values.count { it == ConnectionState.CONNECTING }
        val disconnected = connectionStates.values.count { it == ConnectionState.DISCONNECTED }
        
        binding.textConnectionSummary.text = "Conectadas: $connected/$total | Conectando: $connecting | Desconectadas: $disconnected"
        
        // Mostrar estado general
        when {
            connected == total -> {
                binding.textOverallStatus.text = "✅ Todas las redes conectadas"
                binding.textOverallStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            }
            connected > 0 -> {
                binding.textOverallStatus.text = "⚠️ Algunas redes conectadas"
                binding.textOverallStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
            }
            else -> {
                binding.textOverallStatus.text = "❌ Ninguna red conectada"
                binding.textOverallStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            }
        }
    }
    
    /**
     * Alterna la conexión a una red específica
     */
    private fun toggleNetworkConnection(networkConfig: NetworkConfig) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (networkManager.isConnected(networkConfig)) {
                    networkManager.disconnectFromNetwork(networkConfig)
                    Logger.debug("NetworkStatusFragment", "Desconectado de ${networkConfig.name}", "Desconexión exitosa")
                } else {
                    networkManager.connectToNetwork(networkConfig)
                    Logger.debug("NetworkStatusFragment", "Conectado a ${networkConfig.name}", "Conexión exitosa")
                }
            } catch (e: Exception) {
                Logger.error("NetworkStatusFragment", "Error alternando conexión", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    /**
     * Conecta a todas las redes recomendadas
     */
    private fun connectToAllNetworks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recommendedNetworks = NetworkConfigs.getRecommendedNetworks()
                networkManager.connectToNetworks(recommendedNetworks)
                Logger.debug("NetworkStatusFragment", "Conectando a todas las redes recomendadas", "Iniciando conexiones múltiples")
            } catch (e: Exception) {
                Logger.error("NetworkStatusFragment", "Error conectando a todas las redes", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    /**
     * Desconecta de todas las redes
     */
    private fun disconnectFromAllNetworks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recommendedNetworks = NetworkConfigs.getRecommendedNetworks()
                recommendedNetworks.forEach { networkConfig ->
                    if (networkManager.isConnected(networkConfig)) {
                        networkManager.disconnectFromNetwork(networkConfig)
                    }
                }
                Logger.debug("NetworkStatusFragment", "Desconectado de todas las redes", "Desconexión masiva completada")
            } catch (e: Exception) {
                Logger.error("NetworkStatusFragment", "Error desconectando de todas las redes", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    /**
     * Prueba las conexiones RPC en todas las redes conectadas
     */
    private fun testRpcConnections() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recommendedNetworks = NetworkConfigs.getRecommendedNetworks()
                val connectedNetworks = recommendedNetworks.filter { networkManager.isConnected(it) }
                
                if (connectedNetworks.isEmpty()) {
                    Logger.debug("NetworkStatusFragment", "No hay redes conectadas para probar RPC", "Conecta algunas redes primero")
                    return@launch
                }
                
                Logger.debug("NetworkStatusFragment", "🧪 Probando RPC en ${connectedNetworks.size} redes conectadas...", "Iniciando pruebas RPC")
                
                connectedNetworks.forEach { networkConfig ->
                    try {
                        val result = blockchainService.testRpcConnection(networkConfig)
                        Logger.debug("NetworkStatusFragment", "📡 ${networkConfig.name}: $result", "Resultado RPC")
                    } catch (e: Exception) {
                        Logger.error("NetworkStatusFragment", "❌ Error probando RPC en ${networkConfig.name}", e.message ?: "Error desconocido", e)
                    }
                }
                
            } catch (e: Exception) {
                Logger.error("NetworkStatusFragment", "Error probando conexiones RPC", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): NetworkStatusFragment {
            return NetworkStatusFragment()
        }
    }
}

/**
 * Item de estado de red
 */
data class NetworkStatusItem(
    val networkConfig: NetworkConfig,
    val connectionState: ConnectionState,
    val isConnected: Boolean
)
