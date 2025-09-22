package com.aura.substratecryptotest.crypto.ss58

import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.addressPrefix
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.addressByteOrNull
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b512
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor especializado para codificación y decodificación SS58
 * SS58 es el formato de direcciones utilizado en el ecosistema Substrate
 */
class SS58Encoder {
    
    /**
     * Redes conocidas de Substrate con sus prefix SS58
     * Basado en la especificación SS58 y parachains activas
     */
    enum class NetworkPrefix(val value: Int, val networkName: String, val description: String) {
        // Redes principales
        POLKADOT(0, "Polkadot", "Red principal de Polkadot"),
        KUSAMA(2, "Kusama", "Red canary de Polkadot"),
        
        // Parachains principales
        KILT(38, "Kilt", "Protocolo de identidad descentralizada"),
        ACALA(10, "Acala", "DeFi hub de Polkadot"),
        MOONBEAM(1284, "Moonbeam", "EVM compatible en Polkadot"),
        ASTAR(5, "Astar", "Multi-VM smart contract platform"),
        PARALLEL(172, "Parallel", "DeFi protocol en Polkadot"),
        BIFROST(6, "Bifrost", "Liquid staking protocol"),
        EQUILIBRIUM(1031, "Equilibrium", "DeFi protocol"),
        PHALA(30, "Phala", "Privacy-preserving cloud computing"),
        CRUST(2008, "Crust", "Decentralized storage network"),
        LITENTRY(31, "Litentry", "Decentralized identity aggregator"),
        DARWINIA(18, "Darwinia", "Cross-chain bridge network"),
        MANTRA(44, "Mantra", "DeFi protocol"),
        REEF(42, "Reef", "DeFi operating system"),
        
        // Redes de desarrollo y test
        SUBSTRATE(42, "Substrate", "Red de desarrollo Substrate"),
        WESTEND(42, "Westend", "Red de test de Polkadot"),
        ROCOCO(42, "Rococo", "Red de test de parachains"),
        
        // Redes de Kusama parachains
        KARURA(8, "Karura", "DeFi hub de Kusama"),
        MOONRIVER(1285, "Moonriver", "EVM compatible en Kusama"),
        SHIDEN(5, "Shiden", "Multi-VM smart contract platform en Kusama"),
        BIFROST_KUSAMA(2001, "Bifrost Kusama", "Liquid staking en Kusama"),
        KILT_CANARY(38, "Kilt Canary", "Testnet de Kilt"),
        
        // Redes personalizadas
        CUSTOM(42, "Custom", "Red personalizada")
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
                    // Redes principales
                    0.toShort() -> NetworkPrefix.POLKADOT
                    2.toShort() -> NetworkPrefix.KUSAMA
                    
                    // Parachains principales
                    5.toShort() -> NetworkPrefix.ASTAR  // También SHIDEN usa 5
                    6.toShort() -> NetworkPrefix.BIFROST
                    8.toShort() -> NetworkPrefix.KARURA
                    10.toShort() -> NetworkPrefix.ACALA
                    18.toShort() -> NetworkPrefix.DARWINIA
                    30.toShort() -> NetworkPrefix.PHALA
                    31.toShort() -> NetworkPrefix.LITENTRY
                    38.toShort() -> NetworkPrefix.KILT  // También KILT_PEREGRINE usa 38
                    44.toShort() -> NetworkPrefix.MANTRA
                    172.toShort() -> NetworkPrefix.PARALLEL
                    1031.toShort() -> NetworkPrefix.EQUILIBRIUM
                    1284.toShort() -> NetworkPrefix.MOONBEAM
                    1285.toShort() -> NetworkPrefix.MOONRIVER
                    2001.toShort() -> NetworkPrefix.BIFROST_KUSAMA
                    2008.toShort() -> NetworkPrefix.CRUST
                    
                    // Redes de desarrollo y test
                    42.toShort() -> NetworkPrefix.SUBSTRATE  // También WESTEND, ROCOCO, REEF usan 42
                    
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
                // Usar el SDK real de Substrate para extraer el checksum
                val decodedBytes = address.toAccountId()
                val addressPrefix = address.addressPrefix()
                
                // Calcular el checksum usando la misma lógica que el SDK
                val prefixBytes = when (addressPrefix.toInt()) {
                    in 0..63 -> byteArrayOf(addressPrefix.toByte())
                    in 64..16383 -> {
                        val first = (addressPrefix.toInt() and 0b0000_0000_1111_1100) shr 2
                        val second = (addressPrefix.toInt() shr 8) or ((addressPrefix.toInt() and 0b0000_0000_0000_0011) shl 6)
                        byteArrayOf((first or 0b01000000).toByte(), second.toByte())
                    }
                    else -> byteArrayOf(addressPrefix.toByte())
                }
                
                val prefix = "SS58PRE".toByteArray(Charsets.UTF_8)
                val hash = (prefix + prefixBytes + decodedBytes).blake2b512()
                hash.copyOfRange(0, 2) // Los primeros 2 bytes son el checksum
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
        return network.description
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
    
    /**
     * Obtiene todas las parachains de Polkadot
     * @return Lista de NetworkPrefix para parachains de Polkadot
     */
    fun getPolkadotParachains(): List<NetworkPrefix> {
        return listOf(
            NetworkPrefix.KILT,
            NetworkPrefix.ACALA,
            NetworkPrefix.MOONBEAM,
            NetworkPrefix.ASTAR,
            NetworkPrefix.PARALLEL,
            NetworkPrefix.BIFROST,
            NetworkPrefix.EQUILIBRIUM,
            NetworkPrefix.PHALA,
            NetworkPrefix.CRUST,
            NetworkPrefix.LITENTRY,
            NetworkPrefix.DARWINIA,
            NetworkPrefix.MANTRA,
            NetworkPrefix.REEF
        )
    }
    
    /**
     * Obtiene todas las parachains de Kusama
     * @return Lista de NetworkPrefix para parachains de Kusama
     */
    fun getKusamaParachains(): List<NetworkPrefix> {
        return listOf(
            NetworkPrefix.KARURA,
            NetworkPrefix.MOONRIVER,
            NetworkPrefix.SHIDEN,
            NetworkPrefix.BIFROST_KUSAMA,
            NetworkPrefix.KILT_CANARY
        )
    }
    
    /**
     * Obtiene todas las redes de desarrollo y test
     * @return Lista de NetworkPrefix para redes de desarrollo
     */
    fun getDevelopmentNetworks(): List<NetworkPrefix> {
        return listOf(
            NetworkPrefix.SUBSTRATE,
            NetworkPrefix.WESTEND,
            NetworkPrefix.ROCOCO
        )
    }
    
    /**
     * Obtiene todas las redes principales (Polkadot y Kusama)
     * @return Lista de NetworkPrefix para redes principales
     */
    fun getMainNetworks(): List<NetworkPrefix> {
        return listOf(
            NetworkPrefix.POLKADOT,
            NetworkPrefix.KUSAMA
        )
    }
    
    /**
     * Busca una red por su prefijo numérico
     * @param prefix Prefijo numérico
     * @return NetworkPrefix correspondiente o null si no se encuentra
     */
    fun findNetworkByPrefix(prefix: Int): NetworkPrefix? {
        return NetworkPrefix.values().find { it.value == prefix }
    }
    
    /**
     * Busca una red por su nombre
     * @param name Nombre de la red
     * @return NetworkPrefix correspondiente o null si no se encuentra
     */
    fun findNetworkByName(name: String): NetworkPrefix? {
        return NetworkPrefix.values().find { 
            it.networkName.equals(name, ignoreCase = true) || 
            it.name.equals(name, ignoreCase = true) 
        }
    }
    
    /**
     * Genera direcciones para todas las parachains principales
     * @param publicKey Clave pública
     * @return Map con las direcciones para cada parachain
     */
    suspend fun generateAllParachainAddresses(publicKey: ByteArray): Map<NetworkPrefix, String> {
        val allParachains = getPolkadotParachains() + getKusamaParachains()
        return generateAddressesForNetworks(publicKey, allParachains)
    }
    
    /**
     * Verifica si una dirección es de una parachain específica
     * @param address Dirección SS58
     * @param parachainName Nombre de la parachain
     * @return Boolean indicando si pertenece a la parachain
     */
    suspend fun isParachainAddress(address: String, parachainName: String): Boolean {
        val network = findNetworkByName(parachainName)
        return if (network != null) {
            isFromNetwork(address, network)
        } else {
            false
        }
    }
    
    /**
     * Valida el checksum de una dirección SS58
     * @param address Dirección SS58
     * @return Boolean indicando si el checksum es válido
     */
    suspend fun validateChecksum(address: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Usar el SDK para validar el checksum
                address.toAccountId() // Esto valida automáticamente el checksum
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Obtiene información detallada del checksum de una dirección
     * @param address Dirección SS58
     * @return ChecksumInfo con información del checksum
     */
    suspend fun getChecksumInfo(address: String): ChecksumInfo {
        return withContext(Dispatchers.IO) {
            try {
                val checksum = getChecksum(address)
                val isValid = validateChecksum(address)
                
                ChecksumInfo(
                    checksum = checksum,
                    isValid = isValid,
                    checksumHex = checksum.joinToString("") { "%02x".format(it) },
                    size = checksum.size
                )
            } catch (e: Exception) {
                ChecksumInfo(
                    checksum = ByteArray(0),
                    isValid = false,
                    checksumHex = "",
                    size = 0
                )
            }
        }
    }
    
    /**
     * Verifica si una dirección tiene un formato SS58 válido
     * @param address Dirección SS58
     * @return Boolean indicando si el formato es válido
     */
    suspend fun isValidSS58Format(address: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Verificar que no esté vacía
                if (address.isBlank()) return@withContext false
                
                // Verificar que use caracteres Base58 válidos
                val base58Pattern = Regex("^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+$")
                if (!base58Pattern.matches(address)) return@withContext false
                
                // Verificar que se puede decodificar
                val decoded = address.toAccountId()
                decoded.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Obtiene información completa de validación de una dirección
     * @param address Dirección SS58
     * @return AddressValidationInfo con toda la información de validación
     */
    suspend fun getAddressValidationInfo(address: String): AddressValidationInfo {
        return withContext(Dispatchers.IO) {
            try {
                val isValidFormat = isValidSS58Format(address)
                val isValidChecksum = validateChecksum(address)
                val networkPrefix = getNetworkPrefix(address)
                val checksumInfo = getChecksumInfo(address)
                val publicKey = if (isValidFormat) decode(address) else ByteArray(0)
                
                AddressValidationInfo(
                    address = address,
                    isValidFormat = isValidFormat,
                    isValidChecksum = isValidChecksum,
                    isValid = isValidFormat && isValidChecksum,
                    networkPrefix = networkPrefix,
                    publicKey = publicKey,
                    checksumInfo = checksumInfo
                )
            } catch (e: Exception) {
                AddressValidationInfo(
                    address = address,
                    isValidFormat = false,
                    isValidChecksum = false,
                    isValid = false,
                    networkPrefix = NetworkPrefix.CUSTOM,
                    publicKey = ByteArray(0),
                    checksumInfo = ChecksumInfo(ByteArray(0), false, "", 0)
                )
            }
        }
    }
    
    /**
     * Información de una red
     */
    data class NetworkInfo(
        val prefix: NetworkPrefix,
        val name: String,
        val value: Int,
        val description: String
    )

    /**
     * Información de checksum de una dirección SS58
     */
    data class ChecksumInfo(
        val checksum: ByteArray,
        val isValid: Boolean,
        val checksumHex: String,
        val size: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as ChecksumInfo
            
            if (!checksum.contentEquals(other.checksum)) return false
            if (isValid != other.isValid) return false
            if (checksumHex != other.checksumHex) return false
            if (size != other.size) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = checksum.contentHashCode()
            result = 31 * result + isValid.hashCode()
            result = 31 * result + checksumHex.hashCode()
            result = 31 * result + size
            return result
        }
    }

    /**
     * Información completa de validación de una dirección SS58
     */
    data class AddressValidationInfo(
        val address: String,
        val isValidFormat: Boolean,
        val isValidChecksum: Boolean,
        val isValid: Boolean,
        val networkPrefix: NetworkPrefix,
        val publicKey: ByteArray,
        val checksumInfo: ChecksumInfo
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as AddressValidationInfo
            
            if (address != other.address) return false
            if (isValidFormat != other.isValidFormat) return false
            if (isValidChecksum != other.isValidChecksum) return false
            if (isValid != other.isValid) return false
            if (networkPrefix != other.networkPrefix) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
            if (checksumInfo != other.checksumInfo) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = address.hashCode()
            result = 31 * result + isValidFormat.hashCode()
            result = 31 * result + isValidChecksum.hashCode()
            result = 31 * result + isValid.hashCode()
            result = 31 * result + networkPrefix.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            result = 31 * result + checksumInfo.hashCode()
            return result
        }
    }
}

/**
 * Excepción específica para errores de SS58
 */
class SS58Exception(message: String, cause: Throwable? = null) : Exception(message, cause)
