package com.aura.substratecryptotest.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager
import com.aura.substratecryptotest.crypto.keypair.KeyPairManager
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de flujos seguros de wallet
 * Integra WalletManager existente con SecureWalletManager
 * Maneja la creación de cuentas de fondos e identidad
 */
class SecureWalletFlowManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureWalletFlowManager"
    }
    
    private val secureWalletManager = SecureWalletManager(context)
    private val mnemonicManager = MnemonicManager()
    private val keyPairManager = KeyPairManager()
    private val ss58Encoder = SS58Encoder()
    
    /**
     * Flujo 1: Crear cuenta de fondos (sin path)
     */
    suspend fun createFundsAccount(
        activity: FragmentActivity,
        accountName: String,
        mnemonic: String
    ): FundsAccountResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando cuenta de fondos", "Nombre: $accountName, Mnemonic: ${mnemonic.length} palabras")
                
                // 1. Validar mnemonic
                val isValidMnemonic = mnemonicManager.validateMnemonic(mnemonic)
                if (!isValidMnemonic) {
                    Logger.error(TAG, "Mnemonic inválido", "No se puede crear cuenta", null)
                    return@withContext FundsAccountResult.Error("Mnemonic inválido")
                }
                
                // 2. Generar seed
                val seed = try {
                    mnemonicManager.generateSeed(mnemonic, null)
                } catch (e: Exception) {
                    Logger.error(TAG, "Error generando seed", e.message ?: "Error desconocido", e)
                    return@withContext FundsAccountResult.Error("Error generando seed")
                }
                
                // 3. Derivar clave sin path (cuenta base) - siguiendo patrón de WalletManager
                val keyPairInfo = keyPairManager.generateSr25519KeyPair(
                    mnemonic = mnemonic,
                    password = null
                ) ?: run {
                    Logger.error(TAG, "Error generando clave", "No se puede crear cuenta", null)
                    return@withContext FundsAccountResult.Error("Error generando clave")
                }
                
                // 4. Generar direcciones para diferentes redes
                val addresses = generateNetworkAddresses(keyPairInfo.publicKey)
                
                // 5. Crear cuenta de fondos
                val privateKey = keyPairInfo.privateKey ?: run {
                    Logger.error(TAG, "Clave privada nula", "No se puede crear cuenta", null)
                    return@withContext FundsAccountResult.Error("Clave privada nula")
                }
                
                val fundsAccount = FundsAccount(
                    name = accountName,
                    mnemonic = mnemonic,
                    seed = seed,
                    publicKey = keyPairInfo.publicKey,
                    privateKey = privateKey,
                    addresses = addresses,
                    createdAt = System.currentTimeMillis()
                )
                
                // 6. Guardar de forma segura con biometría
                val saved = secureWalletManager.createSecureWallet(activity, mnemonic, seed)
                if (!saved) {
                    Logger.error(TAG, "Error guardando wallet segura", "No se puede crear cuenta", null)
                    return@withContext FundsAccountResult.Error("Error guardando wallet")
                }
                
                Logger.success(TAG, "Cuenta de fondos creada exitosamente", "Nombre: $accountName")
                FundsAccountResult.Success(fundsAccount)
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando cuenta de fondos", e.message ?: "Error desconocido", e)
                FundsAccountResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Flujo 2: Crear cuenta de identidad (con path //did//0)
     */
    suspend fun createIdentityAccount(
        activity: FragmentActivity,
        legalName: String,
        fundsAccount: FundsAccount
    ): IdentityAccountResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando cuenta de identidad", "Nombre legal: $legalName")
                
                // 1. Derivar clave con path //did//0
                val keyPairInfo = keyPairManager.generateKeyPairWithPath(
                    algorithm = com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm.SR25519,
                    mnemonic = fundsAccount.mnemonic,
                    derivationPath = "//did//0", // Path para identidad
                    password = null
                ) ?: run {
                    Logger.error(TAG, "Error generando clave de identidad", "No se puede crear identidad", null)
                    return@withContext IdentityAccountResult.Error("Error generando clave de identidad")
                }
                
                // 2. Generar direcciones para identidad
                val addresses = generateNetworkAddresses(keyPairInfo.publicKey)
                
                // 3. Crear cuenta de identidad
                val privateKey = keyPairInfo.privateKey ?: run {
                    Logger.error(TAG, "Clave privada nula", "No se puede crear identidad", null)
                    return@withContext IdentityAccountResult.Error("Clave privada nula")
                }
                
                val identityAccount = IdentityAccount(
                    legalName = legalName,
                    publicKey = keyPairInfo.publicKey,
                    privateKey = privateKey,
                    addresses = addresses,
                    linkedFundsAccount = fundsAccount.name,
                    createdAt = System.currentTimeMillis()
                )
                
                // 4. Crear wallet completa
                val completeWallet = CompleteWallet(
                    fundsAccount = fundsAccount,
                    identityAccount = identityAccount,
                    createdAt = System.currentTimeMillis()
                )
                
                // 5. Guardar estado actualizado con biometría
                val saved = secureWalletManager.createSecureWallet(activity, fundsAccount.mnemonic, fundsAccount.seed)
                if (!saved) {
                    Logger.error(TAG, "Error guardando wallet actualizada", "No se puede crear identidad", null)
                    return@withContext IdentityAccountResult.Error("Error guardando wallet actualizada")
                }
                
                Logger.success(TAG, "Cuenta de identidad creada exitosamente", "Nombre legal: $legalName")
                IdentityAccountResult.Success(completeWallet)
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando cuenta de identidad", e.message ?: "Error desconocido", e)
                IdentityAccountResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Genera direcciones para diferentes redes
     */
    private suspend fun generateNetworkAddresses(publicKey: ByteArray): Map<String, String> {
        val addresses = mutableMapOf<String, String>()
        
        try {
            // Substrate base
            addresses["substrate"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.SUBSTRATE)
            
            // Polkadot
            addresses["polkadot"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.POLKADOT)
            
            // Kusama
            addresses["kusama"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KUSAMA)
            
            // KILT
            addresses["kilt"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KILT)
            
            // Acala
            addresses["acala"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.ACALA)
            
            // Moonbeam
            addresses["moonbeam"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.MOONBEAM)
            
            // Astar
            addresses["astar"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.ASTAR)
            
            Logger.success(TAG, "Direcciones generadas", "Redes: ${addresses.keys.joinToString(", ")}")
            
        } catch (e: Exception) {
            Logger.error(TAG, "Error generando direcciones", e.message ?: "Error desconocido", e)
        }
        
        return addresses
    }
    
    /**
     * Verifica si hay una wallet almacenada
     */
    fun hasStoredWallet(): Boolean {
        return secureWalletManager.hasStoredWallet()
    }
    
    /**
     * Recupera la wallet almacenada
     */
    suspend fun retrieveStoredWallet(activity: FragmentActivity): CompleteWallet? {
        return withContext(Dispatchers.IO) {
            try {
                val mnemonic = secureWalletManager.retrieveMnemonic(activity)
                val seed = secureWalletManager.retrieveSeed(activity)
                
                if (mnemonic != null && seed != null) {
                    // Reconstruir wallet desde datos almacenados
                    // Esto requeriría almacenar metadatos adicionales
                    Logger.success(TAG, "Wallet recuperada", "Mnemonic y seed disponibles")
                    null // TODO: Implementar reconstrucción completa
                } else {
                    null
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Error recuperando wallet", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Cuenta de fondos (sin path)
     */
    data class FundsAccount(
        val name: String,
        val mnemonic: String,
        val seed: ByteArray,
        val publicKey: ByteArray,
        val privateKey: ByteArray,
        val addresses: Map<String, String>,
        val createdAt: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as FundsAccount
            
            if (name != other.name) return false
            if (mnemonic != other.mnemonic) return false
            if (!seed.contentEquals(other.seed)) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
            if (!privateKey.contentEquals(other.privateKey)) return false
            if (addresses != other.addresses) return false
            if (createdAt != other.createdAt) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + mnemonic.hashCode()
            result = 31 * result + seed.contentHashCode()
            result = 31 * result + publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            result = 31 * result + addresses.hashCode()
            result = 31 * result + createdAt.hashCode()
            return result
        }
    }
    
    /**
     * Cuenta de identidad (con path //did//0)
     */
    data class IdentityAccount(
        val legalName: String,
        val publicKey: ByteArray,
        val privateKey: ByteArray,
        val addresses: Map<String, String>,
        val linkedFundsAccount: String,
        val createdAt: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as IdentityAccount
            
            if (legalName != other.legalName) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
            if (!privateKey.contentEquals(other.privateKey)) return false
            if (addresses != other.addresses) return false
            if (linkedFundsAccount != other.linkedFundsAccount) return false
            if (createdAt != other.createdAt) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = legalName.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            result = 31 * result + addresses.hashCode()
            result = 31 * result + linkedFundsAccount.hashCode()
            result = 31 * result + createdAt.hashCode()
            return result
        }
    }
    
    /**
     * Wallet completa con ambas cuentas
     */
    data class CompleteWallet(
        val fundsAccount: FundsAccount,
        val identityAccount: IdentityAccount,
        val createdAt: Long
    )
    
    /**
     * Resultado de creación de cuenta de fondos
     */
    sealed class FundsAccountResult {
        data class Success(val account: FundsAccount) : FundsAccountResult()
        data class Error(val message: String) : FundsAccountResult()
    }
    
    /**
     * Resultado de creación de cuenta de identidad
     */
    sealed class IdentityAccountResult {
        data class Success(val wallet: CompleteWallet) : IdentityAccountResult()
        data class Error(val message: String) : IdentityAccountResult()
    }
}
