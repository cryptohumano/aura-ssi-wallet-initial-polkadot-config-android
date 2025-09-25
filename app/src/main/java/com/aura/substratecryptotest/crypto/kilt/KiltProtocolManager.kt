package com.aura.substratecryptotest.crypto.kilt

import com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager
import com.aura.substratecryptotest.crypto.mnemonic.MnemonicInfo
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor principal del protocolo KILT
 * Integra todos los componentes necesarios para trabajar con DIDs KILT
 */
class KiltProtocolManager {
    
    private val mnemonicManager = MnemonicManager()
    private val didManager = KiltDidManager()
    private val signatureManager = KiltSignatureManager()
    
    /**
     * Información completa de un wallet KILT
     */
    data class KiltWallet(
        val mnemonic: String,
        val authenticationDid: KiltDidManager.KiltDidInfo,
        val walletInfo: MnemonicInfo
    )
    
    /**
     * Opciones para crear un wallet KILT
     */
    data class KiltWalletOptions(
        val mnemonicLength: Mnemonic.Length = Mnemonic.Length.TWELVE,
        val password: String? = null
    )
    
    /**
     * Crea un wallet KILT completo
     * @param options Opciones de creación
     * @return KiltWallet con toda la información
     */
    suspend fun createKiltWallet(options: KiltWalletOptions = KiltWalletOptions()): KiltWallet = withContext(Dispatchers.IO) {
        try {
            // 1. Generar mnemonic
            val mnemonic = mnemonicManager.generateMnemonic(options.mnemonicLength)
            val mnemonicInfo = mnemonicManager.importMnemonic(mnemonic)
            
            // 2. Generar solo el DID de autenticación KILT
            val authenticationDid = didManager.generateKiltAuthenticationDid(mnemonic, options.password)
            
            KiltWallet(
                mnemonic = mnemonic,
                authenticationDid = authenticationDid,
                walletInfo = mnemonicInfo
            )
        } catch (e: Exception) {
            throw KiltProtocolException("Error creando wallet KILT: ${e.message}", e)
        }
    }
    
    /**
     * Deriva un DID KILT desde una cuenta Substrate existente derivando al path //did//0
     * @param mnemonic Mnemonic de la wallet Substrate
     * @param password Contraseña opcional
     * @return KiltDidInfo con el DID derivado
     */
    suspend fun deriveKiltDidFromWallet(
        mnemonic: String,
        password: String? = null
    ): KiltDidManager.KiltDidInfo = withContext(Dispatchers.IO) {
        try {
            println("🔍 KiltProtocolManager: === INICIO deriveKiltDidFromWallet ===")
            println("🔍 KiltProtocolManager: Mnemonic: ${mnemonic.length} palabras")
            println("🔍 KiltProtocolManager: Password: ${password ?: "null"}")
            
            // Primero probar la derivación del path
            val testResult = didManager.testPathDerivation(mnemonic, password)
            println("🔍 KiltProtocolManager: Resultado de prueba: ${testResult.message}")
            
            if (!testResult.success) {
                throw KiltProtocolException("Error en derivación del path: ${testResult.message}")
            }
            
            // Si la derivación funciona, generar el DID completo
            val did = "did:kilt:${testResult.kiltAddress}"
            println("🔍 KiltProtocolManager: DID generado: $did")
            
            KiltDidManager.KiltDidInfo(
                did = did,
                address = testResult.kiltAddress ?: "",
                publicKey = testResult.publicKey ?: ByteArray(32),
                verificationRelationship = KiltDidManager.VerificationRelationship.AUTHENTICATION,
                didType = KiltDidManager.DidType.FULL,
                derivationPath = testResult.derivationPath
            )
        } catch (e: Exception) {
            throw KiltProtocolException("Error derivando DID KILT desde wallet: ${e.message}", e)
        }
    }
    
    /**
     * Deriva un DID KILT desde una cuenta Substrate existente (método legacy)
     * @param mnemonic Mnemonic de la cuenta Substrate
     * @param password Contraseña opcional
     * @return KiltDidInfo con el DID derivado
     */
    suspend fun deriveKiltDidFromAccount(
        mnemonic: String,
        password: String? = null
    ): KiltDidManager.KiltDidInfo = withContext(Dispatchers.IO) {
        try {
            // Derivar solo el DID de autenticación usando el path //did//0
            didManager.generateKiltAuthenticationDid(mnemonic, password)
        } catch (e: Exception) {
            throw KiltProtocolException("Error derivando DID KILT desde cuenta: ${e.message}", e)
        }
    }
    
    /**
     * Restaura un wallet KILT desde mnemonic existente
     * @param mnemonic Mnemonic existente
     * @param password Contraseña opcional
     * @return KiltWallet restaurado
     */
    suspend fun restoreKiltWallet(
        mnemonic: String,
        password: String? = null
    ): KiltWallet = withContext(Dispatchers.IO) {
        try {
            // 1. Validar mnemonic
            if (!mnemonicManager.validateMnemonic(mnemonic)) {
                throw KiltProtocolException("Mnemonic inválido")
            }
            
            // 2. Importar mnemonic
            val mnemonicInfo = mnemonicManager.importMnemonic(mnemonic)
            
            // 3. Generar solo el DID de autenticación KILT
            val authenticationDid = didManager.generateKiltAuthenticationDid(mnemonic, password)
            
            KiltWallet(
                mnemonic = mnemonic,
                authenticationDid = authenticationDid,
                walletInfo = mnemonicInfo
            )
        } catch (e: Exception) {
            throw KiltProtocolException("Error restaurando wallet KILT: ${e.message}", e)
        }
    }
    
    /**
     * Autoriza una transacción usando un DID KILT
     * @param wallet Wallet KILT
     * @param extrinsic Transacción a autorizar
     * @param didPurpose Propósito del DID a usar (authentication, assertion, delegation)
     * @param submitter Dirección del submitter
     * @return Transacción autorizada
     */
    suspend fun authorizeTransaction(
        wallet: KiltWallet,
        extrinsic: Any,
        submitter: String
    ): KiltSignatureManager.TransactionAuthorization = withContext(Dispatchers.IO) {
        try {
            val options = KiltSignatureManager.AuthorizationOptions(
                submitter = submitter
            )
            
            signatureManager.authorizeTransaction(wallet.authenticationDid, extrinsic, options)
        } catch (e: Exception) {
            throw KiltProtocolException("Error autorizando transacción: ${e.message}", e)
        }
    }
    
    /**
     * Autoriza múltiples transacciones en lote
     * @param wallet Wallet KILT
     * @param extrinsics Lista de transacciones
     * @param didPurpose Propósito del DID a usar
     * @param submitter Dirección del submitter
     * @return Lista de transacciones autorizadas
     */
    suspend fun authorizeBatch(
        wallet: KiltWallet,
        extrinsics: List<Any>,
        submitter: String
    ): List<KiltSignatureManager.TransactionAuthorization> = withContext(Dispatchers.IO) {
        try {
            val options = KiltSignatureManager.AuthorizationOptions(
                submitter = submitter
            )
            
            signatureManager.authorizeBatch(wallet.authenticationDid, extrinsics, options)
        } catch (e: Exception) {
            throw KiltProtocolException("Error autorizando lote de transacciones: ${e.message}", e)
        }
    }
    
    /**
     * Valida un DID KILT
     * @param did DID a validar
     * @return Boolean indicando si es válido
     */
    suspend fun validateDid(did: String): Boolean = withContext(Dispatchers.IO) {
        try {
            didManager.validateKiltDid(did)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extrae información de una transacción
     * @param extrinsic Transacción Substrate
     * @return Información de la transacción
     */
    suspend fun analyzeTransaction(extrinsic: Any): KiltSignatureManager.TransactionAnalysis = withContext(Dispatchers.IO) {
        try {
            signatureManager.analyzeTransaction(extrinsic)
        } catch (e: Exception) {
            throw KiltProtocolException("Error analizando transacción: ${e.message}", e)
        }
    }
    
    /**
     * Obtiene información del DID de autenticación del wallet
     * @param wallet Wallet KILT
     * @return Información del DID de autenticación
     */
    fun getAuthenticationDidInfo(wallet: KiltWallet): KiltDidManager.KiltDidInfo {
        return wallet.authenticationDid
    }
    
    /**
     * Verifica si una transacción requiere firma DID
     * @param extrinsic Transacción Substrate
     * @return Boolean indicando si requiere firma DID
     */
    suspend fun requiresDidSignature(extrinsic: Any): Boolean = withContext(Dispatchers.IO) {
        try {
            signatureManager.analyzeTransaction(extrinsic).needsDidSignature
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtiene estadísticas del wallet KILT
     * @param wallet Wallet KILT
     * @return Estadísticas del wallet
     */
    fun getWalletStats(wallet: KiltWallet): KiltWalletStats {
        return KiltWalletStats(
            totalDids = 1, // Solo el DID de autenticación
            availablePurposes = listOf("authentication"),
            primaryDid = wallet.authenticationDid.did,
            mnemonicStrength = when (wallet.walletInfo.length) {
                12 -> "WEAK"
                15 -> "MEDIUM" 
                18 -> "STRONG"
                21 -> "STRONG"
                24 -> "STRONG"
                else -> "UNKNOWN"
            },
            derivationPaths = listOf(wallet.authenticationDid.derivationPath)
        )
    }
    
    /**
     * Estadísticas de un wallet KILT
     */
    data class KiltWalletStats(
        val totalDids: Int,
        val availablePurposes: List<String>,
        val primaryDid: String,
        val mnemonicStrength: String,
        val derivationPaths: List<String>
    )
}

/**
 * Excepción específica para errores del protocolo KILT
 */
class KiltProtocolException(message: String, cause: Throwable? = null) : Exception(message, cause)
