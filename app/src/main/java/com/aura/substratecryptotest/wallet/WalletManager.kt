package com.aura.substratecryptotest.wallet

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager
import com.aura.substratecryptotest.crypto.keypair.KeyPairManager
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import com.aura.substratecryptotest.crypto.verification.KeyVerificationManager
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.crypto.kilt.KiltProtocolManager
import com.aura.substratecryptotest.crypto.kilt.KiltDidManager
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.launch

/**
 * Gestor de wallets simplificado
 */
class WalletManager(private val context: Context) : ViewModel() {
    
    private val mnemonicManager = MnemonicManager()
    private val keyPairManager = KeyPairManager()
    private val keyVerificationManager = KeyVerificationManager()
    private val ss58Encoder = SS58Encoder()
    private val kiltProtocolManager = KiltProtocolManager()
    
    private val _wallets = MutableLiveData<List<Wallet>>()
    private val _currentWallet = MutableLiveData<Wallet?>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<String?>()
    
    val wallets: LiveData<List<Wallet>> = _wallets
    val currentWallet: LiveData<Wallet?> = _currentWallet
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<String?> = _error
    
    /**
     * Crea una nueva wallet
     */
    fun createWallet(
        name: String,
        password: String?,
        mnemonicLength: Mnemonic.Length,
        cryptoType: EncryptionAlgorithm
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Logger.i("WalletManager", "🚀 Iniciando creación de wallet...")
                Logger.debug("WalletManager", "Configuración", "Nombre: $name, Algoritmo: $cryptoType, Longitud mnemonic: $mnemonicLength")
                
                // Generar mnemonic real usando MnemonicManager
                val mnemonic = mnemonicManager.generateMnemonic(mnemonicLength)
                Logger.success("WalletManager", "Mnemonic generado", "${mnemonic.take(20)}...")
                
                // Generar par de claves real usando KeyPairManager
                val keyPairInfo = when (cryptoType) {
                    EncryptionAlgorithm.SR25519 -> keyPairManager.generateSr25519KeyPair(mnemonic, password)
                    EncryptionAlgorithm.ED25519 -> keyPairManager.generateEd25519KeyPair(mnemonic, password)
                    EncryptionAlgorithm.ECDSA -> keyPairManager.generateEcdsaKeyPair(mnemonic, password)
                }
                
                if (keyPairInfo != null) {
                    Logger.success("WalletManager", "Par de claves generado exitosamente", 
                        "Algoritmo: ${keyPairInfo.algorithm}, Clave pública: ${keyPairInfo.publicKey.size} bytes, Clave privada: ${keyPairInfo.privateKey?.size ?: 0} bytes")
                    Logger.debug("WalletManager", "Clave pública (hex)", keyPairInfo.publicKey.joinToString("") { "%02x".format(it) })
                    Logger.debug("WalletManager", "Clave privada (hex)", keyPairInfo.privateKey?.joinToString("") { "%02x".format(it) } ?: "N/A")
                    
                    // 🔍 VERIFICACIÓN DE CLAVES - Validar que las claves son correctas
                    Logger.debug("WalletManager", "Iniciando verificación", "de claves...")
                    val verificationResult = keyVerificationManager.verifySr25519KeyPair(keyPairInfo.keyPair, mnemonic)
                    
                    if (verificationResult.isValid) {
                        Logger.success("WalletManager", "✅ Verificación de claves EXITOSA", 
                            "Todas las pruebas pasaron: firma=${verificationResult.signatureValid}, " +
                            "claves=${verificationResult.publicKeyValid && verificationResult.privateKeyValid}")
                    } else {
                        Logger.warning("WalletManager", "⚠️ Verificación de claves con problemas", 
                            "Errores: ${verificationResult.errors.size}, Advertencias: ${verificationResult.warnings.size}")
                        verificationResult.errors.forEach { error ->
                            Logger.error("WalletManager", "Error de verificación", error)
                        }
                        verificationResult.warnings.forEach { warning ->
                            Logger.warning("WalletManager", "Advertencia de verificación", warning)
                        }
                    }
                } else {
                    Logger.error("WalletManager", "Error generando par de claves", "No se pudo generar el par de claves")
                    _error.value = "Error generando par de claves"
                    return@launch
                }
                
                // Generar direcciones para múltiples parachains (sin path)
                val addresses = generateMultipleAddresses(keyPairInfo.publicKey)
                val mainAddress = addresses[SS58Encoder.NetworkPrefix.SUBSTRATE] ?: generateAddress(keyPairInfo.publicKey)
                
                // Generar direcciones DID derivadas para KILT y Polkadot (con path //did//0)
                val didDerivedAddresses = generateDidDerivedAddresses(mnemonic, password)
                
                // Combinar direcciones: normales + DID derivadas
                val allAddresses = addresses.toMutableMap()
                allAddresses.putAll(didDerivedAddresses)
                
                Logger.i("WalletManager", "🆔 Wallet Substrate creada con derivaciones duales - KILT y Polkadot tienen ambas derivaciones")
                
                val wallet = Wallet(
                    id = generateWalletId(),
                    name = name,
                    mnemonic = mnemonic,
                    publicKey = keyPairInfo.publicKey,
                    privateKey = keyPairInfo.privateKey,
                    address = mainAddress,
                    cryptoType = cryptoType,
                    derivationPath = "", // TODO: Implementar JunctionCoder para paths reales
                    createdAt = System.currentTimeMillis(),
                    metadata = mapOf(
                        "addresses" to allAddresses,
                        "parachain_count" to allAddresses.size,
                        "dual_derivations" to mapOf(
                            "kilt" to mapOf(
                                "base" to addresses[SS58Encoder.NetworkPrefix.KILT],
                                "did" to didDerivedAddresses[SS58Encoder.NetworkPrefix.KILT]
                            ),
                            "polkadot" to mapOf(
                                "base" to addresses[SS58Encoder.NetworkPrefix.POLKADOT],
                                "did" to didDerivedAddresses[SS58Encoder.NetworkPrefix.POLKADOT]
                            )
                        )
                    ),
                    kiltDid = null, // Se derivará manualmente
                    kiltAddress = null, // Se derivará manualmente
                    kiltDids = null // Se derivará manualmente
                )
                
                // Información sobre derivación manual de DID KILT
                Logger.success("WalletManager", "✅ Wallet Substrate creada exitosamente", 
                    "Cuenta Substrate lista para usar")
                Logger.debug("WalletManager", "Para derivar DID KILT", 
                    "Ve a 'Info Wallet' y presiona 'Derivar DID' para generar el DID con path //did//0")
                
                val currentWallets = _wallets.value?.toMutableList() ?: mutableListOf()
                currentWallets.add(wallet)
                _wallets.value = currentWallets
                _currentWallet.value = wallet
                
            } catch (e: Exception) {
                _error.value = "Error al crear wallet: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Importa una wallet desde JSON
     */
    fun importWalletFromJson(name: String, @Suppress("UNUSED_PARAMETER") jsonString: String, @Suppress("UNUSED_PARAMETER") password: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Implementación temporal simplificada
                val wallet = Wallet(
                    id = generateWalletId(),
                    name = name,
                    mnemonic = "", // No disponible en importación JSON
                    publicKey = ByteArray(32), // Implementación simplificada
                    privateKey = null, // No disponible en importación JSON
                    address = generateAddress(ByteArray(32)),
                    cryptoType = EncryptionAlgorithm.SR25519, // Asumir SR25519 por defecto
                    derivationPath = "//0",
                    createdAt = System.currentTimeMillis(),
                    metadata = emptyMap()
                )
                
                val currentWallets = _wallets.value?.toMutableList() ?: mutableListOf()
                currentWallets.add(wallet)
                _wallets.value = currentWallets
                _currentWallet.value = wallet
                
            } catch (e: Exception) {
                _error.value = "Error al importar wallet: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Selecciona una wallet
     */
    fun selectWallet(walletId: String) {
        val wallet = _wallets.value?.find { it.id == walletId }
        _currentWallet.value = wallet
    }
    
    /**
     * Elimina una wallet
     */
    fun deleteWallet(walletId: String) {
        val currentWallets = _wallets.value?.toMutableList() ?: mutableListOf()
        currentWallets.removeAll { it.id == walletId }
        _wallets.value = currentWallets
        
        if (_currentWallet.value?.id == walletId) {
            _currentWallet.value = null
        }
    }
    
    /**
     * Exporta una wallet a JSON
     */
    fun exportWalletToJson(walletId: String): String {
        val wallet = _wallets.value?.find { it.id == walletId }
        return if (wallet != null) {
            // Implementación simplificada
            """{"id":"${wallet.id}","name":"${wallet.name}","cryptoType":"${wallet.cryptoType}"}"""
        } else {
            ""
        }
    }
    
    /**
     * Limpia el error
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Obtiene el mnemonic completo de la wallet actual
     */
    fun getCurrentWalletMnemonic(): String? {
        return _currentWallet.value?.mnemonic
    }
    
    /**
     * Obtiene el mnemonic de una wallet específica
     */
    fun getWalletMnemonic(walletId: String): String? {
        return _wallets.value?.find { it.id == walletId }?.mnemonic
    }
    
    /**
     * Obtiene la dirección de la wallet actual
     */
    fun getCurrentWalletAddress(): String? {
        return _currentWallet.value?.address
    }
    
    /**
     * Obtiene la clave pública de la wallet actual
     */
    fun getCurrentWalletPublicKey(): String? {
        return _currentWallet.value?.publicKey?.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Obtiene el DID KILT principal de la wallet actual
     */
    fun getCurrentWalletKiltDid(): String? {
        return _currentWallet.value?.kiltDid
    }
    
    /**
     * Obtiene la dirección KILT de la wallet actual
     */
    fun getCurrentWalletKiltAddress(): String? {
        return _currentWallet.value?.kiltAddress
    }
    
    /**
     * Obtiene todos los DIDs KILT de la wallet actual
     */
    fun getCurrentWalletKiltDids(): Map<String, String>? {
        return _currentWallet.value?.kiltDids
    }
    
    /**
     * Obtiene información completa de DIDs KILT de la wallet actual
     */
    fun getCurrentWalletKiltInfo(): KiltWalletInfo? {
        val wallet = _currentWallet.value ?: return null
        return KiltWalletInfo(
            primaryDid = wallet.kiltDid,
            kiltAddress = wallet.kiltAddress,
            allDids = wallet.kiltDids,
            hasKiltSupport = wallet.kiltDid != null
        )
    }
    
    /**
     * Deriva un DID KILT desde la wallet actual usando su clave pública
     * @return KiltDidInfo derivado o null si hay error
     */
    suspend fun deriveKiltDidFromCurrentWallet(): KiltDidManager.KiltDidInfo? {
        val wallet = _currentWallet.value ?: return null
        
        return try {
            Logger.i("WalletManager", "🆔 Derivando DID KILT al path //did//0 desde wallet actual...")
            Logger.debug("WalletManager", "Mnemonic de wallet: ${wallet.mnemonic.length} palabras", "Palabras: ${wallet.mnemonic.length}")
            
            val kiltDidInfo = kiltProtocolManager.deriveKiltDidFromWallet(
                mnemonic = wallet.mnemonic,
                password = null // TODO: Obtener password si es necesario
            )
            
            // Actualizar la wallet con el DID derivado
            val updatedWallet = wallet.copy(
                kiltDid = kiltDidInfo.did,
                kiltAddress = kiltDidInfo.address,
                kiltDids = mapOf("authentication" to kiltDidInfo.did)
            )
            
            // Actualizar la lista de wallets
            val currentWallets = _wallets.value?.toMutableList() ?: mutableListOf()
            val index = currentWallets.indexOfFirst { it.id == wallet.id }
            if (index >= 0) {
                currentWallets[index] = updatedWallet
                _wallets.value = currentWallets
                _currentWallet.value = updatedWallet
            }
            
                Logger.success("WalletManager", "✅ DID KILT derivado exitosamente desde clave pública",
                    "DID: ${kiltDidInfo.did}")
                Logger.debug("WalletManager", "🔍 Comparación de direcciones", 
                    "Substrate Base: ${wallet.address}\nKILT Derivada: ${kiltDidInfo.address}")
                Logger.debug("WalletManager", "🔍 Path de derivación aplicado", 
                    "Path: ${kiltDidInfo.derivationPath}")
            
            kiltDidInfo
        } catch (e: Exception) {
            Logger.error("WalletManager", "Error derivando DID KILT", e.message ?: "Error desconocido")
            null
        }
    }
    
    /**
     * Obtiene todas las direcciones de parachains de la wallet actual
     */
    fun getCurrentWalletParachainAddresses(): Map<SS58Encoder.NetworkPrefix, String>? {
        val wallet = _currentWallet.value ?: return null
        @Suppress("UNCHECKED_CAST")
        return wallet.metadata["addresses"] as? Map<SS58Encoder.NetworkPrefix, String>
    }
    
    /**
     * Obtiene la dirección de una parachain específica de la wallet actual
     */
    fun getCurrentWalletParachainAddress(networkPrefix: SS58Encoder.NetworkPrefix): String? {
        return getCurrentWalletParachainAddresses()?.get(networkPrefix)
    }
    
    /**
     * Obtiene la dirección KILT de parachain de la wallet actual
     */
    fun getCurrentWalletKiltParachainAddress(): String? {
        return getCurrentWalletParachainAddress(SS58Encoder.NetworkPrefix.KILT)
    }
    
    /**
     * Obtiene las derivaciones duales (base y DID) para KILT y Polkadot
     */
    @Suppress("UNCHECKED_CAST")
    fun getCurrentWalletDualDerivations(): Map<String, Map<String, String?>>? {
        val wallet = _currentWallet.value
        return wallet?.metadata?.get("dual_derivations") as? Map<String, Map<String, String?>>
    }
    
    /**
     * Obtiene la dirección Polkadot de la wallet actual
     */
    fun getCurrentWalletPolkadotAddress(): String? {
        return getCurrentWalletParachainAddress(SS58Encoder.NetworkPrefix.POLKADOT)
    }
    
    /**
     * Obtiene la dirección Kusama de la wallet actual
     */
    fun getCurrentWalletKusamaAddress(): String? {
        return getCurrentWalletParachainAddress(SS58Encoder.NetworkPrefix.KUSAMA)
    }
    
    /**
     * Obtiene información completa de la wallet actual
     */
    fun getCurrentWalletInfo(): WalletInfo? {
        val wallet = _currentWallet.value ?: return null
        return WalletInfo(
            id = wallet.id,
            name = wallet.name,
            mnemonic = wallet.mnemonic,
            publicKey = wallet.publicKey.joinToString("") { "%02x".format(it) },
            address = wallet.address, // Dirección Substrate base
            cryptoType = wallet.cryptoType,
            derivationPath = wallet.derivationPath,
            createdAt = wallet.createdAt,
            kiltDid = wallet.kiltDid,
            kiltAddress = wallet.kiltAddress, // Dirección KILT derivada con //did//0
            kiltDids = wallet.kiltDids
        )
    }
    
    /**
     * Genera un ID único para la wallet
     */
    private fun generateWalletId(): String {
        return "wallet_${System.currentTimeMillis()}"
    }
    
    
    /**
     * Genera direcciones para múltiples parachains principales
     */
    private suspend fun generateMultipleAddresses(publicKey: ByteArray): Map<SS58Encoder.NetworkPrefix, String> {
        return try {
            val parachains = listOf(
                SS58Encoder.NetworkPrefix.POLKADOT,
                SS58Encoder.NetworkPrefix.KUSAMA,
                SS58Encoder.NetworkPrefix.SUBSTRATE,
                SS58Encoder.NetworkPrefix.KILT,
                SS58Encoder.NetworkPrefix.ACALA,
                SS58Encoder.NetworkPrefix.MOONBEAM,
                SS58Encoder.NetworkPrefix.ASTAR
            )
            
            val addresses = ss58Encoder.generateAddressesForNetworks(publicKey, parachains)
            
            Logger.success("WalletManager", "Direcciones generadas para ${addresses.size} parachains", 
                "Parachains: ${addresses.keys.joinToString { it.networkName }}")
            
            // Log específico para dirección KILT
            val kiltAddress = addresses[SS58Encoder.NetworkPrefix.KILT]
            if (kiltAddress != null) {
                Logger.debug("WalletManager", "🔍 Dirección KILT generada en wallet creation", 
                    "KILT Address: $kiltAddress")
            }
            
            addresses
        } catch (e: Exception) {
            Logger.error("WalletManager", "Error generando direcciones múltiples", e.message ?: "Error desconocido", e)
            // Fallback: solo dirección Substrate
            mapOf(SS58Encoder.NetworkPrefix.SUBSTRATE to generateAddress(publicKey))
        }
    }
    
    /**
     * Genera direcciones con derivación //did//0 para KILT y Polkadot
     */
    private suspend fun generateDidDerivedAddresses(mnemonic: String, password: String?): Map<SS58Encoder.NetworkPrefix, String> {
        return try {
            val didDerivedAddresses = mutableMapOf<SS58Encoder.NetworkPrefix, String>()
            
            // Generar clave pública derivada con path //did//0
            val keyPairManager = KeyPairManager()
            val keyPairInfo = keyPairManager.generateKeyPairWithPath(
                algorithm = EncryptionAlgorithm.SR25519,
                mnemonic = mnemonic,
                derivationPath = "//did//0",
                password = password
            )
            
            if (keyPairInfo != null) {
                // Generar direcciones KILT y Polkadot con la clave derivada
                val kiltDidAddress = ss58Encoder.encode(keyPairInfo.publicKey, SS58Encoder.NetworkPrefix.KILT)
                val polkadotDidAddress = ss58Encoder.encode(keyPairInfo.publicKey, SS58Encoder.NetworkPrefix.POLKADOT)
                
                didDerivedAddresses[SS58Encoder.NetworkPrefix.KILT] = kiltDidAddress
                didDerivedAddresses[SS58Encoder.NetworkPrefix.POLKADOT] = polkadotDidAddress
                
                Logger.success("WalletManager", "Direcciones DID derivadas generadas", 
                    "KILT DID: ${kiltDidAddress.take(20)}..., Polkadot DID: ${polkadotDidAddress.take(20)}...")
            } else {
                Logger.error("WalletManager", "Error generando keypair para derivación DID", "KeyPairInfo es null")
            }
            
            didDerivedAddresses
        } catch (e: Exception) {
            Logger.error("WalletManager", "Error generando direcciones DID derivadas", e.message ?: "Error desconocido", e)
            emptyMap()
        }
    }
    
    /**
     * Genera una dirección SS58 usando el SS58Encoder
     */
    private suspend fun generateAddress(publicKey: ByteArray): String {
        return try {
            val address = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.SUBSTRATE)
            Logger.debug("WalletManager", "Dirección SS58 generada", address)
            address
        } catch (e: Exception) {
            Logger.error("WalletManager", "Error generando dirección SS58", e.message ?: "Error desconocido", e)
            // Fallback a dirección de ejemplo si hay error
            "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        }
    }
}

/**
 * Clase de datos para representar una wallet
 */
data class Wallet(
    val id: String,
    val name: String,
    val mnemonic: String,
    val publicKey: ByteArray,
    val privateKey: ByteArray?,
    val address: String,
    val cryptoType: EncryptionAlgorithm,
    val derivationPath: String,
    val createdAt: Long,
    val metadata: Map<String, Any>,
    val kiltDid: String? = null,                    // DID KILT principal
    val kiltAddress: String? = null,                // Dirección KILT (prefix 38)
    val kiltDids: Map<String, String>? = null      // Múltiples DIDs KILT
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Wallet

        if (id != other.id) return false
        if (name != other.name) return false
        if (mnemonic != other.mnemonic) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (privateKey != null && other.privateKey != null) {
            if (!privateKey.contentEquals(other.privateKey)) return false
        } else if (privateKey != other.privateKey) return false
        if (address != other.address) return false
        if (cryptoType != other.cryptoType) return false
        if (derivationPath != other.derivationPath) return false
        if (createdAt != other.createdAt) return false
        if (metadata != other.metadata) return false
        if (kiltDid != other.kiltDid) return false
        if (kiltAddress != other.kiltAddress) return false
        if (kiltDids != other.kiltDids) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mnemonic.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + (privateKey?.contentHashCode() ?: 0)
        result = 31 * result + address.hashCode()
        result = 31 * result + cryptoType.hashCode()
        result = 31 * result + derivationPath.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + (kiltDid?.hashCode() ?: 0)
        result = 31 * result + (kiltAddress?.hashCode() ?: 0)
        result = 31 * result + (kiltDids?.hashCode() ?: 0)
        return result
    }
}

/**
 * Información de wallet para mostrar en la UI
 */
data class WalletInfo(
    val id: String,
    val name: String,
    val mnemonic: String,
    val publicKey: String,
    val address: String,
    val cryptoType: EncryptionAlgorithm,
    val derivationPath: String,
    val createdAt: Long,
    val kiltDid: String? = null,
    val kiltAddress: String? = null,
    val kiltDids: Map<String, String>? = null
) {
    /**
     * Obtiene el mnemonic formateado para mostrar (con números)
     */
    fun getFormattedMnemonic(): String {
        return mnemonic.split(" ").mapIndexed { index, word ->
            "${index + 1}. $word"
        }.joinToString("\n")
    }
    
    /**
     * Obtiene el mnemonic como lista de palabras
     */
    fun getMnemonicWords(): List<String> {
        return mnemonic.split(" ")
    }
    
    /**
     * Obtiene la fecha de creación formateada
     */
    fun getFormattedCreatedAt(): String {
        val date = java.util.Date(createdAt)
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getFormattedDerivationPath(): String {
        return when {
            derivationPath.isBlank() -> "Sin path (cuenta base)"
            kiltAddress != null -> "Base: sin path | KILT: //did//0"
            else -> derivationPath
        }
    }
}

/**
 * Información específica de DIDs KILT de una wallet
 */
data class KiltWalletInfo(
    val primaryDid: String?,
    val kiltAddress: String?,
    val allDids: Map<String, String>?,
    val hasKiltSupport: Boolean
)
