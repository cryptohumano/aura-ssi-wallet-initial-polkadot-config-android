package com.aura.substratecryptotest.data

/**
 * Información de la wallet para la UI
 * Incluye metadata de derivación de cuentas y información DID
 */
data class WalletInfo(
    val name: String,
    val address: String,
    val kiltAddress: String?,
    val polkadotAddress: String?,
    val kusamaAddress: String? = null,
    val createdAt: Long,
    val kiltDid: String? = null,
    val kiltDids: Map<String, String>? = null,
    val derivationPath: String? = null,
    val publicKey: String? = null,
    val metadata: Map<String, Any>? = null
)
