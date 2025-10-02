package com.aura.substratecryptotest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aura.substratecryptotest.MainActivity
import com.aura.substratecryptotest.databinding.FragmentSs58ToolsBinding
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SS58ToolsFragment : Fragment() {
    
    private var _binding: FragmentSs58ToolsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var ss58Encoder: SS58Encoder
    
    companion object {
        fun newInstance(): SS58ToolsFragment {
            return SS58ToolsFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSs58ToolsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar SS58Encoder
        ss58Encoder = SS58Encoder()
        
        setupUI()
    }
    
    private fun setupUI() {
        // Botones de validación
        binding.btnValidateAddress.setOnClickListener {
            validateAddress()
        }
        
        binding.btnConvertAddress.setOnClickListener {
            convertAddress()
        }
        
        binding.btnGenerateTestAddresses.setOnClickListener {
            generateTestAddresses()
        }
        
        binding.btnShowNetworkInfo.setOnClickListener {
            showNetworkInfo()
        }
        
        binding.btnValidateChecksum.setOnClickListener {
            validateChecksum()
        }
    }
    
    /**
     * Valida una dirección SS58 ingresada
     */
    private fun validateAddress() {
        val address = binding.editTextAddress.text.toString().trim()
        if (address.isEmpty()) {
            showToast("❌ Ingresa una dirección SS58")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val validationInfo = ss58Encoder.getAddressValidationInfo(address)
                
                val message = """
                    📍 Dirección: ${validationInfo.address}
                    
                    ✅ Formato válido: ${if (validationInfo.isValidFormat) "Sí" else "No"}
                    ✅ Checksum válido: ${if (validationInfo.isValidChecksum) "Sí" else "No"}
                    ✅ Dirección válida: ${if (validationInfo.isValid) "Sí" else "No"}
                    
                    🌐 Red: ${validationInfo.networkPrefix.networkName}
                    📋 Descripción: ${validationInfo.networkPrefix.description}
                    🔢 Prefijo: ${validationInfo.networkPrefix.value}
                    
                    🔑 Clave pública: ${validationInfo.publicKey.joinToString("") { "%02x".format(it) }}
                    
                    🔐 Checksum: ${validationInfo.checksumInfo.checksumHex}
                    ✅ Checksum válido: ${if (validationInfo.checksumInfo.isValid) "Sí" else "No"}
                """.trimIndent()
                
                showValidationDialog("Validación de Dirección SS58", message)
                
            } catch (e: Exception) {
                showToast("❌ Error validando dirección: ${e.message}")
            }
        }
    }
    
    /**
     * Convierte una dirección entre redes
     */
    private fun convertAddress() {
        val address = binding.editTextConvertFrom.text.toString().trim()
        val targetNetworkName = binding.editTextConvertTo.text.toString().trim()
        
        if (address.isEmpty() || targetNetworkName.isEmpty()) {
            showToast("❌ Completa ambos campos")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val targetNetwork = ss58Encoder.findNetworkByName(targetNetworkName)
                if (targetNetwork == null) {
                    showToast("❌ Red no encontrada: $targetNetworkName")
                    return@launch
                }
                
                val convertedAddress = ss58Encoder.convertToNetwork(address, targetNetwork)
                
                val message = """
                    📍 Dirección original: $address
                    🌐 Red original: ${ss58Encoder.getNetworkPrefix(address).networkName}
                    
                    📍 Dirección convertida: $convertedAddress
                    🌐 Nueva red: ${targetNetwork.networkName}
                    📋 Descripción: ${targetNetwork.description}
                    🔢 Prefijo: ${targetNetwork.value}
                """.trimIndent()
                
                showValidationDialog("Conversión de Dirección", message)
                
            } catch (e: Exception) {
                showToast("❌ Error convirtiendo dirección: ${e.message}")
            }
        }
    }
    
    /**
     * Genera direcciones de prueba para diferentes redes
     */
    private fun generateTestAddresses() {
        // Usar una clave pública de prueba (32 bytes)
        val testPublicKey = ByteArray(32) { it.toByte() }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val parachains = listOf(
                    SS58Encoder.NetworkPrefix.POLKADOT,
                    SS58Encoder.NetworkPrefix.KUSAMA,
                    SS58Encoder.NetworkPrefix.KILT,
                    SS58Encoder.NetworkPrefix.ACALA,
                    SS58Encoder.NetworkPrefix.MOONBEAM,
                    SS58Encoder.NetworkPrefix.ASTAR
                )
                
                val addresses = ss58Encoder.generateAddressesForNetworks(testPublicKey, parachains)
                
                val message = addresses.entries.joinToString("\n\n") { (network, address) ->
                    "🌐 ${network.networkName} (${network.value}):\n$address\n📋 ${network.description}"
                }
                
                showValidationDialog("Direcciones de Prueba", message)
                
            } catch (e: Exception) {
                showToast("❌ Error generando direcciones: ${e.message}")
            }
        }
    }
    
    /**
     * Muestra información de todas las redes soportadas
     */
    private fun showNetworkInfo() {
        val networks = ss58Encoder.getSupportedNetworks()
        
        val message = networks.joinToString("\n\n") { network ->
            "🌐 ${network.name}\n📋 ${network.description}\n🔢 Prefijo: ${network.value}"
        }
        
        showValidationDialog("Redes Soportadas", message)
    }
    
    /**
     * Valida el checksum de una dirección
     */
    private fun validateChecksum() {
        val address = binding.editTextChecksumAddress.text.toString().trim()
        if (address.isEmpty()) {
            showToast("❌ Ingresa una dirección SS58")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val checksumInfo = ss58Encoder.getChecksumInfo(address)
                
                val message = """
                    📍 Dirección: $address
                    
                    🔐 Checksum: ${checksumInfo.checksumHex}
                    📏 Tamaño: ${checksumInfo.size} bytes
                    ✅ Válido: ${if (checksumInfo.isValid) "Sí" else "No"}
                    
                    🔍 Detalles:
                    ${checksumInfo.checksum.joinToString(" ") { "%02x".format(it) }}
                """.trimIndent()
                
                showValidationDialog("Validación de Checksum", message)
                
            } catch (e: Exception) {
                showToast("❌ Error validando checksum: ${e.message}")
            }
        }
    }
    
    /**
     * Muestra un diálogo con información de validación
     */
    private fun showValidationDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Copiar") { _, _ ->
                copyToClipboard(title, message)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    /**
     * Copia texto al portapapeles
     */
    private fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast("✅ $label copiado al portapapeles")
        } catch (e: Exception) {
            showToast("❌ Error copiando $label")
        }
    }
    
    /**
     * Muestra un mensaje toast
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

