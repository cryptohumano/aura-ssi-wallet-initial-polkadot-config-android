package com.aura.substratecryptotest.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.aura.substratecryptotest.MainActivity
import com.aura.substratecryptotest.databinding.FragmentWalletInfoBinding
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.wallet.WalletInfo
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalletInfoFragment : Fragment() {
    
    private var _binding: FragmentWalletInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var walletManager: WalletManager
    private lateinit var ss58Encoder: SS58Encoder
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletInfoBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener WalletManager desde la actividad principal
        walletManager = (activity as? MainActivity)?.walletManager 
            ?: throw IllegalArgumentException("WalletManager es requerido")
        
        // Inicializar SS58Encoder
        ss58Encoder = SS58Encoder()
        
        setupUI()
        observeWalletChanges()
        loadWalletInfo()
    }
    
    private fun setupUI() {
        // Configurar botones de copia
        binding.btnCopyMnemonic.setOnClickListener {
            copyMnemonic()
        }
        
        binding.btnCopyAddress.setOnClickListener {
            copyAddress()
        }
        
        binding.btnCopyPublicKey.setOnClickListener {
            copyPublicKey()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadWalletInfo()
        }
        
        // Botones para funcionalidades SS58
        binding.btnGenerateKiltAddress.setOnClickListener {
            generateParachainAddress(SS58Encoder.NetworkPrefix.KILT)
        }
        
        binding.btnGeneratePolkadotAddress.setOnClickListener {
            generateParachainAddress(SS58Encoder.NetworkPrefix.POLKADOT)
        }
        
        binding.btnGenerateKusamaAddress.setOnClickListener {
            generateParachainAddress(SS58Encoder.NetworkPrefix.KUSAMA)
        }
        
        binding.btnGenerateAllAddresses.setOnClickListener {
            generateAllParachainAddresses()
        }
    }
    
    /**
     * Observa los cambios en la wallet actual
     */
    private fun observeWalletChanges() {
        walletManager.currentWallet.observe(viewLifecycleOwner, Observer { wallet ->
            if (wallet != null) {
                val walletInfo = walletManager.getCurrentWalletInfo()
                if (walletInfo != null) {
                    displayWalletInfo(walletInfo)
                }
            } else {
                showNoWalletMessage()
            }
        })
    }
    
    private fun loadWalletInfo() {
        val walletInfo = walletManager.getCurrentWalletInfo()
        if (walletInfo != null) {
            displayWalletInfo(walletInfo)
        } else {
            showNoWalletMessage()
        }
    }
    
    private fun displayWalletInfo(walletInfo: WalletInfo) {
        // Información básica
        binding.textWalletName.text = walletInfo.name
        binding.textWalletId.text = "ID: ${walletInfo.id}"
        binding.textCryptoType.text = "Tipo: ${walletInfo.cryptoType}"
        binding.textDerivationPath.text = "Ruta: ${walletInfo.getFormattedDerivationPath()}"
        binding.textCreatedAt.text = "Creado: ${walletInfo.getFormattedCreatedAt()}"
        
        // Mnemonic formateado
        binding.textMnemonicFormatted.text = walletInfo.getFormattedMnemonic()
        binding.textMnemonicSingleLine.text = walletInfo.mnemonic
        
        // Clave pública
        binding.textPublicKey.text = walletInfo.publicKey
        
        // Dirección principal
        binding.textAddress.text = walletInfo.address
        
        // Mostrar direcciones de parachains si están disponibles
        displayParachainAddresses()
        
        // Mostrar el contenedor de información
        binding.containerWalletInfo.visibility = View.VISIBLE
        binding.containerNoWallet.visibility = View.GONE
    }
    
    /**
     * Muestra las direcciones de parachains disponibles
     */
    private fun displayParachainAddresses() {
        val parachainAddresses = walletManager.getCurrentWalletParachainAddresses()
        
        if (parachainAddresses != null && parachainAddresses.isNotEmpty()) {
            // Mostrar información de parachains disponibles
            val parachainInfo = parachainAddresses.entries.joinToString("\n") { (network, address) ->
                "🌐 ${network.networkName}: ${address.take(20)}..."
            }
            
            // Actualizar el texto de información de parachains si existe
            binding.textParachainInfo.text = "Direcciones generadas para ${parachainAddresses.size} parachains:\n$parachainInfo"
            
            Logger.success("WalletInfoFragment", "Direcciones de parachains cargadas", 
                "Total: ${parachainAddresses.size}, Redes: ${parachainAddresses.keys.joinToString { it.networkName }}")
        } else {
            binding.textParachainInfo.text = "No hay direcciones de parachains disponibles"
        }
    }
    
    private fun showNoWalletMessage() {
        binding.containerWalletInfo.visibility = View.GONE
        binding.containerNoWallet.visibility = View.VISIBLE
    }
    
    private fun copyMnemonic() {
        val mnemonic = walletManager.getCurrentWalletMnemonic()
        if (mnemonic != null) {
            copyToClipboard("Mnemonic", mnemonic)
        } else {
            showToast("❌ No hay wallet actual")
        }
    }
    
    private fun copyAddress() {
        val address = walletManager.getCurrentWalletAddress()
        if (address != null) {
            copyToClipboard("Dirección", address)
        } else {
            showToast("❌ No hay wallet actual")
        }
    }
    
    private fun copyPublicKey() {
        val publicKey = walletManager.getCurrentWalletPublicKey()
        if (publicKey != null) {
            copyToClipboard("Clave Pública", publicKey)
        } else {
            showToast("❌ No hay wallet actual")
        }
    }
    
    private fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast("✅ $label copiado al portapapeles")
        } catch (e: Exception) {
            showToast("❌ Error copiando $label")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Genera una dirección para una parachain específica
     */
    private fun generateParachainAddress(networkPrefix: SS58Encoder.NetworkPrefix) {
        val walletInfo = walletManager.getCurrentWalletInfo()
        if (walletInfo == null) {
            showToast("❌ No hay wallet actual")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Convertir la clave pública de hex a ByteArray
                val publicKeyHex = walletInfo.publicKey
                val publicKey = publicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                
                // Generar dirección para la parachain
                val address = ss58Encoder.encode(publicKey, networkPrefix)
                
                // Mostrar la dirección generada
                showParachainAddressDialog(networkPrefix, address)
                
            } catch (e: Exception) {
                showToast("❌ Error generando dirección ${networkPrefix.networkName}: ${e.message}")
            }
        }
    }
    
    /**
     * Genera direcciones para todas las parachains principales
     */
    private fun generateAllParachainAddresses() {
        val walletInfo = walletManager.getCurrentWalletInfo()
        if (walletInfo == null) {
            showToast("❌ No hay wallet actual")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Convertir la clave pública de hex a ByteArray
                val publicKeyHex = walletInfo.publicKey
                val publicKey = publicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                
                // Generar direcciones para parachains principales
                val parachains = listOf(
                    SS58Encoder.NetworkPrefix.POLKADOT,
                    SS58Encoder.NetworkPrefix.KUSAMA,
                    SS58Encoder.NetworkPrefix.KILT,
                    SS58Encoder.NetworkPrefix.ACALA,
                    SS58Encoder.NetworkPrefix.MOONBEAM,
                    SS58Encoder.NetworkPrefix.ASTAR
                )
                
                val addresses = ss58Encoder.generateAddressesForNetworks(publicKey, parachains)
                
                // Mostrar todas las direcciones
                showAllAddressesDialog(addresses)
                
            } catch (e: Exception) {
                showToast("❌ Error generando direcciones: ${e.message}")
            }
        }
    }
    
    /**
     * Muestra un diálogo con la dirección de una parachain específica
     */
    private fun showParachainAddressDialog(networkPrefix: SS58Encoder.NetworkPrefix, address: String) {
        val message = """
            🌐 ${networkPrefix.networkName}
            📍 ${networkPrefix.description}
            
            📋 Dirección:
            $address
            
            ✅ Prefijo: ${networkPrefix.value}
        """.trimIndent()
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Dirección ${networkPrefix.networkName}")
            .setMessage(message)
            .setPositiveButton("Copiar") { _, _ ->
                copyToClipboard("Dirección ${networkPrefix.networkName}", address)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    /**
     * Muestra un diálogo con todas las direcciones generadas
     */
    private fun showAllAddressesDialog(addresses: Map<SS58Encoder.NetworkPrefix, String>) {
        val message = addresses.entries.joinToString("\n\n") { (network, address) ->
            "🌐 ${network.networkName} (${network.value}):\n$address"
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Direcciones para Todas las Parachains")
            .setMessage(message)
            .setPositiveButton("Copiar Todas") { _, _ ->
                val allAddresses = addresses.entries.joinToString("\n\n") { (network, address) ->
                    "${network.networkName}: $address"
                }
                copyToClipboard("Todas las Direcciones", allAddresses)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): WalletInfoFragment {
            return WalletInfoFragment()
        }
    }
}
