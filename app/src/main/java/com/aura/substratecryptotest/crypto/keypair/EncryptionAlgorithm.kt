package com.aura.substratecryptotest.crypto.keypair

/**
 * Algoritmos de cifrado soportados para la generación de pares de claves
 */
enum class EncryptionAlgorithm {
    SR25519,    // Schnorr signatures over Ristretto25519
    ED25519,    // Edwards curve Digital Signature Algorithm
    ECDSA       // Elliptic Curve Digital Signature Algorithm
}
