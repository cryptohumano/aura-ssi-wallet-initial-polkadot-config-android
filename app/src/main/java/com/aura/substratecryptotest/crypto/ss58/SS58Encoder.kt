package com.aura.substratecryptotest.crypto.ss58

import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.addressPrefix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor especializado para codificación y decodificación SS58
 * SS58 es el formato de direcciones utilizado en el ecosistema Substrate
 */
class SS58Encoder {
    
    /**
     * Redes conocidas de Substrate con sus prefix SS58
     */
    enum class NetworkPrefix(val value: Int, val networkName: String) {
        POLKADOT(0, "Polkadot"),
        KUSAMA(2, "Kusama"),
        SUBSTRATE(42, "Substrate"),
        WESTEND(42, "Westend"),
        ROCOCO(42, "Rococo"),
        KILT(38, "Kilt"),
        CUSTOM(42, "Custom")
    }
    
    /**
     * Información de una dirección SS58
     */
    data class SS58AddressInfo(
        val address: String,
        val publicKey: ByteArray,
        val networkPrefix: NetworkPrefix,
        val checksum: ByteArray,
        val isValid: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as SS58AddressInfo
            
            if (address != other.address) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
            if (networkPrefix != other.networkPrefix) return false
            if (!checksum.contentEquals(other.checksum)) return false
            if (isValid != other.isValid) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = address.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            result = 31 * result + networkPrefix.hashCode()
            result = 31 * result + checksum.contentHashCode()
            result = 31 * result + isValid.hashCode()
            return result
        }
    }
    
    /**
     * Codifica una clave pública a dirección SS58
     * @param publicKey Clave pública en formato ByteArray
     * @param networkPrefix Prefijo de red (por defecto Substrate)
     * @return String con la dirección SS58
     */
    suspend fun encode(publicKey: ByteArray, networkPrefix: NetworkPrefix = NetworkPrefix.SUBSTRATE): String {
        return withContext(Dispatchers.IO) {
            try {
                // Usar el SDK real de Substrate
                publicKey.toAddress(networkPrefix.value.toShort())
            } catch (e: Exception) {
                throw SS58Exception("Error codificando dirección SS58: ${e.message}", e)
            }
        }
    }
    
    /**
     * Decodifica una dirección SS58 a clave pública
     * @param address Dirección SS58
     * @return ByteArray con la clave pública
     */
    suspend fun decode(address: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                // Usar el SDK real de Substrate
                address.toAccountId()
            } catch (e: Exception) {
                throw SS58Exception("Error decodificando dirección SS58: ${e.message}", e)
            }
        }
    }
    
    /**
     * Obtiene información detallada de una dirección SS58
     * @param address Dirección SS58
     * @return SS58AddressInfo con la información de la dirección
     */
    suspend fun getAddressInfo(address: String): SS58AddressInfo {
        return withContext(Dispatchers.IO) {
            try {
                val publicKey = decode(address)
                val networkPrefix = getNetworkPrefix(address)
                val checksum = getChecksum(address)
                val isValid = validateAddress(address)
                
                SS58AddressInfo(
                    address = address,
                    publicKey = publicKey,
                    networkPrefix = networkPrefix,
                    checksum = checksum,
                    isValid = isValid
                )
            } catch (e: Exception) {
                SS58AddressInfo(
                    address = address,
                    publicKey = ByteArray(0),
                    networkPrefix = NetworkPrefix.CUSTOM,
                    checksum = ByteArray(0),
                    isValid = false
                )
            }
        }
    }
    
    /**
     * Valida una dirección SS58
     * @param address Dirección SS58 a validar
     * @return Boolean indicando si es válida
     */
    suspend fun validateAddress(address: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Usar el SDK real de Substrate para validar
                val accountId = address.toAccountId()
                accountId.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Obtiene el prefijo de red de una dirección SS58
     * @param address Dirección SS58
     * @return NetworkPrefix del prefijo detectado
     */
    suspend fun getNetworkPrefix(address: String): NetworkPrefix {
        return withContext(Dispatchers.IO) {
            try {
                // Usar el SDK real de Substrate para obtener el prefijo
                val prefix = address.addressPrefix()
                when (prefix) {
                    0.toShort() -> NetworkPrefix.POLKADOT
                    2.toShort() -> NetworkPrefix.KUSAMA
                    42.toShort() -> NetworkPrefix.SUBSTRATE
                    else -> NetworkPrefix.CUSTOM
                }
            } catch (e: Exception) {
                NetworkPrefix.CUSTOM
            }
        }
    }
    
    /**
     * Obtiene el checksum de una dirección SS58
     * @param address Dirección SS58
     * @return ByteArray con el checksum
     */
    private suspend fun getChecksum(address: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implementar extracción de checksum usando el SDK
                // Por ahora retornamos un array vacío
                ByteArray(0)
            } catch (e: Exception) {
                ByteArray(0)
            }
        }
    }
    
    /**
     * Convierte una dirección de una red a otra
     * @param address Dirección SS58 original
     * @param targetNetwork Red de destino
     * @return String con la dirección en la nueva red
     */
    suspend fun convertToNetwork(address: String, targetNetwork: NetworkPrefix): String {
        return withContext(Dispatchers.IO) {
            try {
                val publicKey = decode(address)
                encode(publicKey, targetNetwork)
            } catch (e: Exception) {
                throw SS58Exception("Error convirtiendo dirección a red ${targetNetwork.name}: ${e.message}", e)
            }
        }
    }
    
    /**
     * Genera múltiples direcciones para diferentes redes
     * @param publicKey Clave pública
     * @param networks Lista de redes
     * @return Map con las direcciones para cada red
     */
    suspend fun generateAddressesForNetworks(
        publicKey: ByteArray,
        networks: List<NetworkPrefix>
    ): Map<NetworkPrefix, String> {
        return withContext(Dispatchers.IO) {
            try {
                networks.associateWith { network ->
                    encode(publicKey, network)
                }
            } catch (e: Exception) {
                throw SS58Exception("Error generando direcciones para múltiples redes: ${e.message}", e)
            }
        }
    }
    
    /**
     * Verifica si una dirección pertenece a una red específica
     * @param address Dirección SS58
     * @param network Red a verificar
     * @return Boolean indicando si pertenece a la red
     */
    suspend fun isFromNetwork(address: String, network: NetworkPrefix): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val addressNetwork = getNetworkPrefix(address)
                addressNetwork == network
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Obtiene información de todas las redes soportadas
     * @return Lista con información de las redes
     */
    fun getSupportedNetworks(): List<NetworkInfo> {
        return NetworkPrefix.values().map { prefix ->
            NetworkInfo(
                prefix = prefix,
                name = prefix.name,
                value = prefix.value,
                description = getNetworkDescription(prefix)
            )
        }
    }
    
    /**
     * Obtiene la descripción de una red
     * @param network Red
     * @return String con la descripción
     */
    private fun getNetworkDescription(network: NetworkPrefix): String {
        return when (network) {
            NetworkPrefix.POLKADOT -> "Red principal de Polkadot"
            NetworkPrefix.KUSAMA -> "Red de canary de Polkadot"
            NetworkPrefix.SUBSTRATE -> "Red de desarrollo Substrate"
            NetworkPrefix.WESTEND -> "Red de test de Polkadot"
            NetworkPrefix.ROCOCO -> "Red de test de parachains"
            NetworkPrefix.KILT -> "Red de Kilt"
            NetworkPrefix.CUSTOM -> "Red personalizada"
        }
    }
    
    /**
     * Valida múltiples direcciones
     * @param addresses Lista de direcciones SS58
     * @return Map con el resultado de validación para cada dirección
     */
    suspend fun validateMultipleAddresses(addresses: List<String>): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                addresses.associateWith { address ->
                    validateAddress(address)
                }
            } catch (e: Exception) {
                addresses.associateWith { false }
            }
        }
    }
}

/**
 * Información de una red
 */
data class NetworkInfo(
    val prefix: SS58Encoder.NetworkPrefix,
    val name: String,
    val value: Int,
    val description: String
)

/**
 * Excepción específica para errores de SS58
 */
class SS58Exception(message: String, cause: Throwable? = null) : Exception(message, cause)
