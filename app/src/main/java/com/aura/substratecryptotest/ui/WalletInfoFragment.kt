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
import androidx.lifecycle.lifecycleScope
import com.aura.substratecryptotest.MainActivity
import com.aura.substratecryptotest.databinding.FragmentWalletInfoBinding
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.wallet.WalletInfo as WalletWalletInfo
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.user.UserManagementService
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalletInfoFragment : Fragment() {
    
    private var _binding: FragmentWalletInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var walletManager: WalletManager
    private lateinit var ss58Encoder: SS58Encoder
    private lateinit var userManager: UserManager
    private lateinit var appDatabaseManager: AppDatabaseManager
    
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
        
        // Inicializar componentes de usuario
        userManager = UserManager(requireContext())
        appDatabaseManager = AppDatabaseManager(requireContext())
        
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
        
        // Botón para derivar DID KILT
        binding.btnDeriveKiltDid.setOnClickListener {
            deriveKiltDid()
        }
        
        // Botón para probar firma KILT
        binding.btnTestKiltSignature.setOnClickListener {
            testKiltSignature()
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
    
    private fun displayWalletInfo(walletInfo: WalletWalletInfo) {
        Logger.debug("WalletInfoFragment", "Actualizando UI", "DID: ${walletInfo.kiltDid ?: "null"}, Address: ${walletInfo.kiltAddress ?: "null"}")
        
        // Obtener información del usuario actual
        val currentUser = userManager.getCurrentUser()
        val userManagementService = appDatabaseManager.userManagementService
        
        Logger.debug("WalletInfoFragment", "Usuario actual", "User: ${currentUser?.name ?: "null"}, ID: ${currentUser?.id ?: "null"}")
        
        // Información básica de la wallet
        binding.textWalletName.text = walletInfo.name
        binding.textWalletId.text = "ID: ${walletInfo.id}"
        binding.textCryptoType.text = "Tipo: ${walletInfo.cryptoType}"
        binding.textDerivationPath.text = "Ruta: ${walletInfo.getFormattedDerivationPath()}"
        binding.textCreatedAt.text = "Creado: ${walletInfo.getFormattedCreatedAt()}"
        
        // Información del usuario (si está disponible)
        if (currentUser != null) {
            binding.textWalletName.text = "${walletInfo.name} (Usuario: ${currentUser.name})"
            Logger.debug("WalletInfoFragment", "Usuario encontrado", "Nombre: ${currentUser.name}, ID: ${currentUser.id}")
        } else {
            Logger.warning("WalletInfoFragment", "No hay usuario actual", "La wallet existe pero no hay usuario asociado")
        }
        
        // Mnemonic formateado
        binding.textMnemonicFormatted.text = walletInfo.getFormattedMnemonic()
        binding.textMnemonicSingleLine.text = walletInfo.mnemonic
        
        // Clave pública
        binding.textPublicKey.text = walletInfo.publicKey
        
        // Dirección principal (Substrate base)
        binding.textAddress.text = "Substrate: ${walletInfo.address}"
        
        // Mostrar información del usuario actual
        displayUserInfo(currentUser)
        
        // Mostrar información del DID KILT si está disponible (PRIMERO)
        displayKiltDidInfo(walletInfo)
        
        // Mostrar direcciones de parachains si están disponibles (DESPUÉS)
        displayParachainAddresses()
        
        // Mostrar el contenedor de información
        binding.containerWalletInfo.visibility = View.VISIBLE
        binding.containerNoWallet.visibility = View.GONE
    }
    
    /**
     * Muestra información del usuario actual
     */
    private fun displayUserInfo(currentUser: com.aura.substratecryptotest.security.UserManager.User?) {
        if (currentUser != null) {
            Logger.debug("WalletInfoFragment", "Mostrando información de usuario", "Nombre: ${currentUser.name}, ID: ${currentUser.id}")
            
            // Mostrar información del usuario en el log para debugging
            Logger.debug("WalletInfoFragment", "=== INFORMACIÓN DE USUARIO ===", "")
            Logger.debug("WalletInfoFragment", "Nombre de usuario", currentUser.name)
            Logger.debug("WalletInfoFragment", "ID de usuario", currentUser.id)
            Logger.debug("WalletInfoFragment", "Biometric ID", currentUser.biometricId)
            Logger.debug("WalletInfoFragment", "Fecha de creación", currentUser.createdAt.toString())
            Logger.debug("WalletInfoFragment", "Estado activo", currentUser.isActive.toString())
            
            // Si hay campos de texto disponibles en el layout, actualizarlos
            // Por ahora solo loggeamos la información
        } else {
            Logger.warning("WalletInfoFragment", "No hay usuario actual", "La wallet existe pero no hay usuario asociado")
        }
    }
    
    /**
     * Muestra las direcciones de parachains disponibles con derivaciones duales
     */
    private fun displayParachainAddresses() {
        val walletInfo = walletManager.getCurrentWalletInfo()
        
        // Si el DID KILT ya está derivado, NO sobrescribir la comparación de direcciones
        if (walletInfo?.kiltDid != null && walletInfo.kiltAddress != null) {
            Logger.debug("WalletInfoFragment", "DID KILT ya derivado", "Manteniendo comparación de direcciones")
            return // No sobrescribir la información de comparación
        }
        
        val parachainAddresses = walletManager.getCurrentWalletParachainAddresses()
        val dualDerivations = walletManager.getCurrentWalletDualDerivations()
        
        if (parachainAddresses != null && parachainAddresses.isNotEmpty()) {
            val parachainInfo = StringBuilder()
            parachainInfo.append("📍 Direcciones de Parachains:\n\n")
            
            // Mostrar derivaciones duales para KILT y Polkadot
            dualDerivations?.let { derivations ->
                derivations["kilt"]?.let { kilt ->
                    parachainInfo.append("🔹 KILT:\n")
                    parachainInfo.append("  • Base (sin path): ${kilt["base"]?.take(20)}...\n")
                    parachainInfo.append("  • DID (//did//0): ${kilt["did"]?.take(20)}...\n\n")
                }
                
                derivations["polkadot"]?.let { polkadot ->
                    parachainInfo.append("🔹 Polkadot:\n")
                    parachainInfo.append("  • Base (sin path): ${polkadot["base"]?.take(20)}...\n")
                    parachainInfo.append("  • DID (//did//0): ${polkadot["did"]?.take(20)}...\n\n")
                }
            }
            
            // Mostrar otras redes (solo derivación base)
            val otherNetworks = parachainAddresses.filterKeys { 
                it != SS58Encoder.NetworkPrefix.KILT && it != SS58Encoder.NetworkPrefix.POLKADOT 
            }
            
            if (otherNetworks.isNotEmpty()) {
                parachainInfo.append("🔹 Otras redes (solo base):\n")
                otherNetworks.entries.forEach { (network, address) ->
                    parachainInfo.append("  • ${network.networkName}: ${address.take(20)}...\n")
                }
            }
            
            binding.textParachainInfo.text = parachainInfo.toString()
            
            Logger.success("WalletInfoFragment", "Derivaciones duales mostradas", 
                "KILT y Polkadot con ambas derivaciones, ${otherNetworks.size} redes adicionales")
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
    
    /**
     * Deriva un DID KILT desde la cuenta Substrate actual
     */
    private fun deriveKiltDid() {
        val currentWallet = walletManager.currentWallet.value
        if (currentWallet == null) {
            showErrorDialog("No hay wallet disponible", "Primero crea una wallet para derivar el DID KILT")
            return
        }
        
        // Mostrar loading
        binding.btnDeriveKiltDid.isEnabled = false
        binding.btnDeriveKiltDid.text = "Derivando..."
        
        // Ejecutar derivación en background
        lifecycleScope.launch {
            try {
                val kiltDidInfo = walletManager.deriveKiltDidFromCurrentWallet()
                
                if (kiltDidInfo != null) {
                    // ✅ ACTUALIZAR TODA LA UI con la información actualizada
                    Logger.success("WalletInfoFragment", "DID derivado exitosamente", "Actualizando UI completa")
                    val updatedWalletInfo = walletManager.getCurrentWalletInfo()
                    if (updatedWalletInfo != null) {
                        Logger.debug("WalletInfoFragment", "Refrescando UI", "DID: ${updatedWalletInfo.kiltDid}, Address: ${updatedWalletInfo.kiltAddress}")
                        displayWalletInfo(updatedWalletInfo)
                    }
                    
                    // Mostrar diálogo de éxito
                    showSuccessDialog(
                        "DID KILT Derivado Exitosamente",
                        "DID: ${kiltDidInfo.did}\n\nDirección: ${kiltDidInfo.address}\n\nPath: ${kiltDidInfo.derivationPath}"
                    )
                } else {
                    showErrorDialog("Error", "No se pudo derivar el DID KILT")
                }
            } catch (e: Exception) {
                showErrorDialog("Error derivando DID KILT", e.message ?: "Error desconocido")
            } finally {
                // Restaurar botón
                binding.btnDeriveKiltDid.isEnabled = true
                binding.btnDeriveKiltDid.text = "Derivar DID"
            }
        }
    }
    
    /**
     * Muestra la información del DID KILT si está disponible
     */
    private fun displayKiltDidInfo(walletInfo: WalletWalletInfo) {
        if (walletInfo.kiltDid != null && walletInfo.kiltAddress != null) {
            binding.textKiltDidInfo.text = walletInfo.kiltDid
            binding.textKiltDidAddress.text = "KILT (//did//0): ${walletInfo.kiltAddress}"
            binding.btnDeriveKiltDid.text = "DID Ya Derivado"
            binding.btnDeriveKiltDid.isEnabled = false
            
            // Mostrar comparación de direcciones
            showAddressComparison(walletInfo.address, walletInfo.kiltAddress)
            
            // Agregar botón de prueba de encriptación
            binding.btnTestEncryption.visibility = View.VISIBLE
        binding.btnTestEncryption.setOnClickListener {
            testEncryptionManager()
        }
        
        binding.btnTestApiClient.setOnClickListener {
            testApiClient()
        }
        } else {
            binding.textKiltDidInfo.text = "DID no derivado aún"
            binding.textKiltDidAddress.text = "KILT (//did//0): No disponible"
            binding.btnDeriveKiltDid.text = "Derivar DID"
            binding.btnDeriveKiltDid.isEnabled = true
        }
    }
    
    /**
     * Prueba el EncryptionKeyManager con datos reales
     */
    private fun testEncryptionManager() {
        val currentWallet = walletManager.currentWallet.value
        if (currentWallet == null) {
            showErrorDialog("Error", "No hay wallet disponible para probar encriptación")
            return
        }
        
        // Mostrar loading
        binding.btnTestEncryption.isEnabled = false
        binding.btnTestEncryption.text = "Probando..."
        
        // Ejecutar prueba en background
        lifecycleScope.launch {
            try {
                Logger.debug("WalletInfoFragment", "🧪 Iniciando prueba de EncryptionKeyManager", "Wallet: ${currentWallet.name}")
                
                // Crear EncryptionKeyManager
                val encryptionKeyManager = com.aura.substratecryptotest.crypto.encryption.EncryptionKeyManager(requireContext())
                
                // Generar salt
                val salt = encryptionKeyManager.generateSalt()
                Logger.debug("WalletInfoFragment", "✅ Salt generado", "Size: ${salt.size} bytes")
                
                // Generar encryption key usando el DID
                val kiltDid = walletManager.getCurrentWalletKiltDid()
                if (kiltDid != null) {
                    val encryptionKey = encryptionKeyManager.generateEncryptionKeyFromDid(kiltDid, salt)
                    
                    if (encryptionKey != null) {
                        Logger.success("WalletInfoFragment", "✅ Encryption key generada", "Size: ${encryptionKey.size} bytes")
                        
                        // Probar encriptación/desencriptación
                        val testChallenge = "test_challenge_${System.currentTimeMillis()}"
                        val encryptionResult = encryptionKeyManager.encryptChallenge(testChallenge, encryptionKey)
                        
                        if (encryptionResult != null) {
                            val (encryptedChallenge, nonce) = encryptionResult
                            Logger.success("WalletInfoFragment", "✅ Challenge encriptado", "Size: ${encryptedChallenge.size} bytes")
                            
                            val decryptedChallenge = encryptionKeyManager.decryptChallenge(encryptedChallenge, nonce, encryptionKey)
                            
                            if (decryptedChallenge == testChallenge) {
                                Logger.success("WalletInfoFragment", "✅ Challenge desencriptado correctamente", "Match: $decryptedChallenge")
                                
                                // Mostrar resultado exitoso
                                showSuccessDialog(
                                    "Prueba de Encriptación Exitosa",
                                    "✅ Encryption key generada: ${encryptionKey.size} bytes\n" +
                                    "✅ Challenge encriptado: ${encryptedChallenge.size} bytes\n" +
                                    "✅ Challenge desencriptado: $decryptedChallenge\n" +
                                    "✅ DID usado: ${kiltDid.take(20)}..."
                                )
                            } else {
                                Logger.error("WalletInfoFragment", "❌ Challenge desencriptado incorrectamente", "Expected: $testChallenge, Got: $decryptedChallenge", null)
                                showErrorDialog("Error", "Challenge desencriptado incorrectamente")
                            }
                        } else {
                            Logger.error("WalletInfoFragment", "❌ Error encriptando challenge", "No se pudo encriptar", null)
                            showErrorDialog("Error", "No se pudo encriptar el challenge")
                        }
                    } else {
                        Logger.error("WalletInfoFragment", "❌ Error generando encryption key", "No se pudo generar", null)
                        showErrorDialog("Error", "No se pudo generar la encryption key")
                    }
                } else {
                    Logger.error("WalletInfoFragment", "❌ No hay DID KILT disponible", "Derivar DID primero", null)
                    showErrorDialog("Error", "No hay DID KILT disponible. Deriva el DID primero.")
                }
                
            } catch (e: Exception) {
                Logger.error("WalletInfoFragment", "❌ Error en prueba de encriptación", e.message ?: "Error desconocido", e)
                showErrorDialog("Error en prueba", e.message ?: "Error desconocido")
            } finally {
                // Restaurar botón
                binding.btnTestEncryption.isEnabled = true
                binding.btnTestEncryption.text = "Probar Encriptación"
            }
        }
    }
    
    /**
     * Muestra comparación entre dirección Substrate base y KILT derivada
     */
    private fun showAddressComparison(substrateAddress: String, kiltAddress: String) {
        val comparison = """
            📍 Comparación de Direcciones:
            
            🔹 Substrate Base (sin path):
            $substrateAddress
            
            🔹 KILT Derivada (//did//0):
            $kiltAddress
            
            ✅ Derivación exitosa - Direcciones diferentes
            📊 Path aplicado: //did//0
        """.trimIndent()
        
        // Actualizar el texto de información de parachains para mostrar la comparación
        binding.textParachainInfo.text = comparison
        
        Logger.success("WalletInfoFragment", "Comparación de direcciones mostrada", 
            "Substrate: ${substrateAddress.take(20)}... | KILT: ${kiltAddress.take(20)}...")
    }
    
    /**
     * Muestra un diálogo de éxito
     */
    private fun showSuccessDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("✅ $title")
            .setMessage(message)
            .setPositiveButton("Copiar DID") { _, _ ->
                copyToClipboard("DID KILT", binding.textKiltDidInfo.text.toString())
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    /**
     * Prueba la firma KILT con el DID actual
     */
    private fun testKiltSignature() {
        val currentWallet = walletManager.currentWallet.value
        if (currentWallet == null) {
            showErrorDialog("No hay wallet disponible", "Primero crea una wallet para probar las firmas KILT")
            return
        }
        
        if (currentWallet.kiltDid == null) {
            showErrorDialog("DID no disponible", "Primero deriva el DID KILT para probar las firmas")
            return
        }
        
        // Mostrar loading
        binding.btnTestKiltSignature.isEnabled = false
        binding.btnTestKiltSignature.text = "Probando..."
        
        // Ejecutar prueba de firma en background
        lifecycleScope.launch {
            try {
                val kiltDidInfo = walletManager.getCurrentWalletKiltInfo()
                if (kiltDidInfo != null) {
                    // Crear KiltSignatureManager y probar firma
                    val signatureManager = com.aura.substratecryptotest.crypto.kilt.KiltSignatureManager()
                    
                    // Crear KiltDidInfo desde la información disponible
                    val didInfo = com.aura.substratecryptotest.crypto.kilt.KiltDidManager.KiltDidInfo(
                        did = kiltDidInfo.primaryDid ?: "",
                        address = kiltDidInfo.kiltAddress ?: "",
                        publicKey = ByteArray(32), // TODO: Obtener clave pública real
                        verificationRelationship = com.aura.substratecryptotest.crypto.kilt.KiltDidManager.VerificationRelationship.AUTHENTICATION,
                        didType = com.aura.substratecryptotest.crypto.kilt.KiltDidManager.DidType.FULL,
                        derivationPath = "//did//0"
                    )
                    
                    val signature = signatureManager.testDidSignature(didInfo, "Mensaje de prueba KILT")
                    
                    // Mostrar resultado
                    showSuccessDialog(
                        "Firma KILT Exitosa",
                        "Key URI: ${signature.keyUri}\n\nFirma: ${signature.signature}\n\nRelación: ${signature.verificationRelationship.name}\n\nNonce: ${signature.nonce}\n\nSubmitter: ${signature.submitter}"
                    )
                } else {
                    showErrorDialog("Error", "No se pudo obtener información del DID KILT")
                }
            } catch (e: Exception) {
                showErrorDialog("Error probando firma KILT", e.message ?: "Error desconocido")
            } finally {
                // Restaurar botón
                binding.btnTestKiltSignature.isEnabled = true
                binding.btnTestKiltSignature.text = "Probar Firma"
            }
        }
    }
    
    private fun testApiClient() {
        val currentWallet = walletManager.currentWallet.value
        if (currentWallet == null) {
            showErrorDialog("Error", "No hay wallet disponible para probar API Client")
            return
        }
        
        // Mostrar loading
        binding.btnTestApiClient.isEnabled = false
        binding.btnTestApiClient.text = "Probando..."
        
        // Ejecutar prueba en background
        lifecycleScope.launch {
            try {
                Logger.debug("WalletInfoFragment", "🧪 Iniciando prueba de API Client", "Wallet: ${currentWallet.name}")
                
                val apiTest = com.aura.substratecryptotest.api.DidApiTest(requireContext())
                val success = apiTest.runBasicTests()
                
                // Limpiar sesión después de los tests
                apiTest.cleanup()
                
                if (success) {
                    showSuccessDialog("✅ API Client", "Tests básicos pasaron correctamente")
                } else {
                    showErrorDialog("❌ API Client", "Algunos tests fallaron")
                }
            } catch (e: Exception) {
                Logger.error("WalletInfoFragment", "Error en prueba de API Client", e.message ?: "Error desconocido", e)
                showErrorDialog("❌ Error", "Error ejecutando tests: ${e.message}")
            } finally {
                // Restaurar botón
                binding.btnTestApiClient.isEnabled = true
                binding.btnTestApiClient.text = "🌐 Probar API Client"
            }
        }
    }
    
    /**
     * Muestra un diálogo de error
     */
    private fun showErrorDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("❌ $title")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
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
