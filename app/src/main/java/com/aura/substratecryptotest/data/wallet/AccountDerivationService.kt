package com.aura.substratecryptotest.data.wallet

import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import com.aura.substratecryptotest.crypto.keypair.KeyPairManager
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar la derivación de cuentas desde mnemónicos
 */
class AccountDerivationService {
    
    private val keyPairManager = KeyPairManager()
    private val ss58Encoder = SS58Encoder()
    
    /**
     * Deriva una cuenta de fondos desde un mnemonic (versión con callbacks)
     */
    fun deriveFundsAccount(
        mnemonic: String,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("AccountDerivationService", "Derivando cuenta de fondos", "Mnemonic completo: $mnemonic")
                
                val keyPairInfo = keyPairManager.generateSr25519KeyPair(mnemonic, null)
                
                if (keyPairInfo != null) {
                    // ✅ GENERAR DIRECCIONES PARA DIFERENTES REDES
                    val addresses = generateNetworkAddresses(keyPairInfo.publicKey)
                    
                    val derivationResult = mutableMapOf<String, String>().apply {
                        // Dirección principal (Substrate)
                        put("address", addresses["substrate"] ?: "")
                        put("publicKey", keyPairInfo.publicKey?.joinToString("") { "%02x".format(it) } ?: "")
                        put("privateKey", keyPairInfo.privateKey?.joinToString("") { "%02x".format(it) } ?: "")
                        
                        // ✅ 4 redes principales (como SecureWalletFlowManager)
                        put("POLKADOT", addresses["polkadot"] ?: "")
                        put("KUSAMA", addresses["kusama"] ?: "")
                        put("KILT", addresses["kilt"] ?: "")
                    }
                    
                    val mainAddress = addresses["substrate"] ?: "N/A"
                    Logger.success("AccountDerivationService", "Cuenta derivada exitosamente", "Dirección: ${mainAddress.take(20)}...")
                    
                    withContext(Dispatchers.Main) {
                        onSuccess(derivationResult)
                    }
                } else {
                    Logger.error("AccountDerivationService", "Error derivando cuenta", "No se pudo generar el par de claves", null)
                    withContext(Dispatchers.Main) {
                        onError("No se pudo generar el par de claves")
                    }
                }
                
            } catch (e: Exception) {
                Logger.error("AccountDerivationService", "Error derivando cuenta", e.message ?: "Error desconocido", e)
                
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error derivando cuenta")
                }
            }
        }
    }
    
    /**
     * Deriva una cuenta de fondos desde un mnemonic (versión con Result)
     */
    suspend fun deriveFundsAccountWithResult(mnemonic: String): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("AccountDerivationService", "Derivando cuenta de fondos con Result", "Mnemonic completo: $mnemonic")
                
                val keyPairInfo = keyPairManager.generateSr25519KeyPair(mnemonic, null)
                
                if (keyPairInfo != null) {
                    // ✅ GENERAR DIRECCIONES PARA DIFERENTES REDES
                    val addresses = generateNetworkAddresses(keyPairInfo.publicKey)
                    
                    val derivationResult = mutableMapOf<String, String>().apply {
                        // Dirección principal (Substrate)
                        put("address", addresses["substrate"] ?: "")
                        put("publicKey", keyPairInfo.publicKey?.joinToString("") { "%02x".format(it) } ?: "")
                        put("privateKey", keyPairInfo.privateKey?.joinToString("") { "%02x".format(it) } ?: "")
                        
                        // ✅ 4 redes principales (como SecureWalletFlowManager)
                        put("POLKADOT", addresses["polkadot"] ?: "")
                        put("KUSAMA", addresses["kusama"] ?: "")
                        put("KILT", addresses["kilt"] ?: "")
                    }
                    
                    val mainAddress = addresses["substrate"] ?: "N/A"
                    Logger.success("AccountDerivationService", "Cuenta derivada exitosamente", "Dirección: ${mainAddress.take(20)}...")
                    
                    Result.success(derivationResult)
                } else {
                    Logger.error("AccountDerivationService", "Error derivando cuenta", "No se pudo generar el par de claves", null)
                    Result.failure(Exception("No se pudo generar el par de claves"))
                }
                
            } catch (e: Exception) {
                Logger.error("AccountDerivationService", "Error derivando cuenta", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Genera direcciones para diferentes redes usando SS58Encoder
     * Basado en SecureWalletFlowManager para consistencia
     */
    private suspend fun generateNetworkAddresses(publicKey: ByteArray): Map<String, String> {
        val addresses = mutableMapOf<String, String>()
        
        return withContext(Dispatchers.IO) {
            try {
                // Substrate base
                addresses["substrate"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.SUBSTRATE)
                
                // Polkadot
                addresses["polkadot"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.POLKADOT)
                
                // Kusama
                addresses["kusama"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KUSAMA)
                
                // KILT
                addresses["kilt"] = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KILT)
                
                Logger.success("AccountDerivationService", "Direcciones generadas", "Redes: ${addresses.keys.joinToString(", ")}")
                
            } catch (e: Exception) {
                Logger.error("AccountDerivationService", "Error generando direcciones", e.message ?: "Error desconocido", e)
            }
            
            addresses
        }
    }
}
