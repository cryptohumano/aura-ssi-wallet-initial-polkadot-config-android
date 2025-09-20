# Estructura Modular de Criptografía Substrate

Este documento describe la nueva estructura modular implementada para el manejo de criptografía en el proyecto Substrate Crypto Test.

## 📁 Estructura de Paquetes

```
com.aura.substratecryptotest.crypto/
├── mnemonic/
│   └── MnemonicManager.kt          # ✅ IMPLEMENTED - Gestión de mnemónicos BIP39
├── keypair/
│   └── KeyPairManager.kt           # ✅ IMPLEMENTED - Gestión de pares de claves
├── junction/
│   └── JunctionManager.kt          # ✅ IMPLEMENTED - Sistema de derivación de claves
├── hash/
│   └── HashManager.kt              # ✅ IMPLEMENTED - Funciones de hashing
├── ss58/
│   └── SS58Encoder.kt              # ✅ IMPLEMENTED - Codificación de direcciones
├── StorageManager.kt               # ✅ IMPLEMENTED - Almacenamiento seguro
├── DependencyChecker.kt            # ❌ OBSOLETE - Verificador de dependencias
└── SDKVerificationManager.kt       # ❌ OBSOLETE - Verificador del SDK
```

## 🔧 Componentes Principales

### 1. MnemonicManager
**Ubicación:** `crypto.mnemonic.MnemonicManager`
**Estado:** ✅ **IMPLEMENTED & TESTED**

**Funcionalidades:**
- ✅ Generación de mnemónicos BIP39 (12, 15, 18, 21, 24 palabras)
- ✅ Validación de mnemónicos BIP39
- ✅ Importación de mnemónicos existentes
- ✅ Generación de seeds con PBKDF2 (con/sin contraseña)
- ✅ Información detallada de mnemónicos
- ✅ Formato de backup numerado
- ✅ Restauración desde backup
- ✅ Análisis de fortaleza (WEAK/MEDIUM/STRONG)
- ✅ Generación múltiple de mnemónicos

**Uso:**
```kotlin
val mnemonicManager = MnemonicManager()

// Generar mnemonic
val mnemonic = mnemonicManager.generateMnemonic(Mnemonic.Length.TWELVE)

// Validar mnemonic
val isValid = mnemonicManager.validateMnemonic(mnemonic)

// Importar mnemonic
val mnemonicInfo = mnemonicManager.importMnemonic(mnemonic)
```

### 2. KeyPairManager
**Ubicación:** `crypto.keypair.KeyPairManager`
**Estado:** ⚠️ **PARTIALLY IMPLEMENTED - REQUIRES COMPLETION**

**Funcionalidades:**
- ✅ Generación básica de pares de claves SR25519, ED25519
- ⚠️ Derivación con rutas personalizadas (PLACEHOLDER)
- ⚠️ Derivación con Junctions (PLACEHOLDER)
- ✅ Generación desde seeds básica
- ⚠️ Información detallada de pares de claves (PLACEHOLDER)
- ✅ Integración con MnemonicManager
- ⚠️ Almacenamiento seguro de keypairs (PLACEHOLDER)

**Algoritmos Soportados:**
- **SR25519:** ✅ Algoritmo principal de Substrate (BASIC IMPLEMENTATION)
- **ED25519:** ✅ Compatible con Ed25519 (BASIC IMPLEMENTATION)
- **ECDSA:** ❌ Compatible con Bitcoin/Ethereum (NOT IMPLEMENTED)

**Funciones con TODO:**
- `getPublicKey()` - Retorna null
- `getPrivateKey()` - Retorna null
- `isValidKeyPair()` - Retorna true siempre
- Derivación con Junctions - Implementación básica

**Uso:**
```kotlin
val keyPairManager = KeyPairManager()

// Generar par de claves SR25519
val keyPair = keyPairManager.generateSr25519KeyPair(mnemonic, password)

// Generar con derivación
val keyPairWithPath = keyPairManager.generateKeyPairWithPath(
    EncryptionAlgorithm.SR25519,
    mnemonic,
    "//Alice",
    password
)
```

### 3. JunctionManager
**Ubicación:** `crypto.junction.JunctionManager`
**Estado:** ⚠️ **PARTIALLY IMPLEMENTED - REQUIRES COMPLETION**

**Funcionalidades:**
- ✅ Creación de Junctions (Hard, Soft, Password, Parent)
- ✅ Parsing de rutas de derivación
- ✅ Validación de rutas básica
- ✅ Rutas predefinidas comunes
- ✅ Conversión entre formatos Substrate y BIP32
- ✅ Normalización de chaincodes con BLAKE2b-256
- ⚠️ Soporte para múltiples tipos de derivación (PLACEHOLDER)

**Funciones con TODO:**
- Derivación real con Junctions - Implementación básica
- Validación avanzada de rutas - Implementación básica

**Tipos de Junction:**
- **Hard:** `//Alice`, `//Bob` (derivación hard)
- **Soft:** `/Alice`, `/Bob` (derivación soft)
- **Password:** `///password` (con contraseña)
- **Parent:** `/..` (derivación desde padre)

**Uso:**
```kotlin
val junctionManager = JunctionManager()

// Crear junctions
val hardJunction = junctionManager.createHardJunction("Alice")
val softJunction = junctionManager.createSoftJunction("Bob")

// Crear ruta de derivación
val path = junctionManager.createDerivationPath(hardJunction, softJunction)

// Parsear ruta existente
val parsedPath = junctionManager.parseDerivationPath("//Alice/Bob")
```

### 4. HashManager
**Ubicación:** `crypto.hash.HashManager`
**Estado:** ✅ **IMPLEMENTED & READY**

**Funcionalidades:**
- ✅ BLAKE2b (128, 256, 512 bits)
- ✅ Keccak-256
- ✅ SHA-256, SHA-512
- ✅ XXHash64/128/256
- ✅ Validación de hashes
- ✅ Hashing de strings y bytes
- ✅ BLAKE2b-128 concat
- ✅ Integración con BouncyCastle

**Algoritmos Soportados:**
- **BLAKE2b-128:** ✅ 16 bytes de salida (IMPLEMENTED)
- **BLAKE2b-256:** ✅ 32 bytes de salida (IMPLEMENTED - principal de Substrate)
- **BLAKE2b-512:** ✅ 64 bytes de salida (IMPLEMENTED)
- **Keccak-256:** ✅ 32 bytes de salida (IMPLEMENTED - compatible con Ethereum)
- **SHA-256:** ✅ 32 bytes de salida (IMPLEMENTED)
- **SHA-512:** ✅ 64 bytes de salida (IMPLEMENTED)
- **XXHash64:** ✅ 8 bytes de salida (IMPLEMENTED)
- **XXHash128:** ✅ 16 bytes de salida (IMPLEMENTED)
- **XXHash256:** ✅ 32 bytes de salida (IMPLEMENTED)

**Nota:** Este es el único manager completamente implementado con funciones reales.

**Uso:**
```kotlin
val hashManager = HashManager()

// Hash BLAKE2b-256
val result = hashManager.blake2b256(data)

// Hash de string
val stringHash = hashManager.hashString(HashAlgorithm.SHA_256, "Hola")

// Validar hash
val isValid = hashManager.verifyHash(data, expectedHash, HashAlgorithm.BLAKE2B_256)
```

### 5. SS58Encoder
**Ubicación:** `crypto.ss58.SS58Encoder`
**Estado:** ⚠️ **PLACEHOLDER - REQUIRES IMPLEMENTATION**

**Funcionalidades:**
- ⚠️ Codificación de claves públicas a direcciones SS58 (PLACEHOLDER)
- ⚠️ Decodificación de direcciones SS58 (PLACEHOLDER)
- ⚠️ Validación de direcciones (PLACEHOLDER)
- ⚠️ Soporte para múltiples redes (PLACEHOLDER)
- ⚠️ Conversión entre redes (PLACEHOLDER)

**Redes Soportadas:**
- **Polkadot:** Prefix 0 (PLACEHOLDER)
- **Kusama:** Prefix 2 (PLACEHOLDER)
- **Substrate:** Prefix 42 (PLACEHOLDER)
- **Westend:** Prefix 42 (PLACEHOLDER)
- **Rococo:** Prefix 42 (PLACEHOLDER)
- **Aura:** Prefix 42 (PLACEHOLDER)

**Nota:** Actualmente usa implementación temporal. Requiere integración con SDK real de SS58.

**Uso:**
```kotlin
val ss58Encoder = SS58Encoder()

// Codificar dirección
val address = ss58Encoder.encode(publicKey, NetworkPrefix.POLKADOT)

// Decodificar dirección
val publicKey = ss58Encoder.decode(address)

// Validar dirección
val isValid = ss58Encoder.validateAddress(address)
```

### 6. StorageManager
**Ubicación:** `crypto.StorageManager`
**Estado:** ✅ **IMPLEMENTED & READY**

**Funcionalidades:**
- ✅ Almacenamiento seguro con EncryptedSharedPreferences
- ✅ Gestión de claves maestras AES256-GCM
- ✅ Almacenamiento de keypairs
- ✅ Almacenamiento de mnemónicos
- ✅ Almacenamiento de metadatos
- ✅ Recuperación de datos
- ✅ Eliminación segura

### 7. WalletManager
**Ubicación:** `wallet.WalletManager`
**Estado:** ✅ **IMPLEMENTED & TESTED**

**Funcionalidades:**
- ✅ Creación de wallets con MnemonicManager real
- ✅ Gestión de múltiples wallets
- ✅ Integración con UI (LiveData/ViewModel)
- ✅ Exportación/Importación básica
- ✅ Selección y eliminación de wallets
- ✅ Manejo de errores

**Uso:**
```kotlin
val cryptoManager = SubstrateCryptoManager()

// Crear wallet completa
val walletInfo = cryptoManager.createWallet(
    mnemonicLength = Mnemonic.Length.TWELVE,
    password = null,
    derivationPath = null
)

// Restaurar wallet
val restoredWallet = cryptoManager.restoreWallet(mnemonic, password)

// Generar direcciones
val addresses = cryptoManager.generateAddresses(publicKey)
```

## 🚀 Ejemplos de Uso

### Crear una Wallet Completa
```kotlin
val cryptoManager = SubstrateCryptoManager()

val walletInfo = cryptoManager.createWallet(
    mnemonicLength = Mnemonic.Length.TWELVE,
    password = "mipassword",
    derivationPath = "//Alice"
)

println("Mnemonic: ${walletInfo.mnemonic.words}")
println("Direcciones:")
walletInfo.addresses.forEach { (algorithm, addresses) ->
    println("$algorithm:")
    addresses.forEach { (network, address) ->
        println("  $network: $address")
    }
}
```

### Trabajar con Derivación
```kotlin
val junctionManager = JunctionManager()
val cryptoManager = SubstrateCryptoManager()

// Crear ruta personalizada
val hardJunction = junctionManager.createHardJunction("Alice")
val softJunction = junctionManager.createSoftJunction("Bob")
val path = junctionManager.createDerivationPath(hardJunction, softJunction)

// Generar claves con derivación
val keyPair = cryptoManager.generateKeyPairWithJunctions(
    EncryptionAlgorithm.SR25519,
    mnemonic,
    path.junctions
)
```

### Operaciones de Hashing
```kotlin
val cryptoManager = SubstrateCryptoManager()

val data = "Hola Substrate!".toByteArray()

// BLAKE2b-256 (principal de Substrate)
val blake2bResult = cryptoManager.hash(HashAlgorithm.BLAKE2B_256, data)
println("BLAKE2b-256: ${blake2bResult.hex}")

// Keccak-256 (compatible con Ethereum)
val keccakResult = cryptoManager.hash(HashAlgorithm.KECCAK_256, data)
println("Keccak-256: ${keccakResult.hex}")
```

## 🔄 Migración desde el Sistema Anterior

### Antes (Sistema Monolítico)
```kotlin
// Archivos dispersos y funcionalidades mezcladas
val mnemonicManager = MnemonicManager()
val keyPairManager = KeyPairManager()
val junctionManager = JunctionManager()
// ... múltiples gestores separados
```

### Después (Sistema Modular)
```kotlin
// Un solo punto de entrada
val cryptoManager = SubstrateCryptoManager()

// Todas las operaciones a través del gestor principal
val walletInfo = cryptoManager.createWallet()
val addresses = cryptoManager.generateAddresses(publicKey)
val hash = cryptoManager.hash(HashAlgorithm.BLAKE2B_256, data)
```

## 📋 Beneficios de la Nueva Estructura

1. **Modularidad:** Cada componente tiene una responsabilidad específica
2. **Mantenibilidad:** Código más fácil de mantener y actualizar
3. **Reutilización:** Componentes pueden ser reutilizados independientemente
4. **Testabilidad:** Cada módulo puede ser probado por separado
5. **Escalabilidad:** Fácil agregar nuevas funcionalidades
6. **Compatibilidad:** Mantiene compatibilidad con el SDK de Substrate Android
7. **Documentación:** Código bien documentado y ejemplos claros

## 🧪 Testing

Para probar la nueva estructura, ejecuta los ejemplos:

```kotlin
val examples = CryptoExamples()
examples.ejecutarTodosLosEjemplos()
```

## 📚 Referencias

- [SDK de Substrate Android](https://github.com/nova-wallet/substrate-sdk-android)
- [BIP39 Mnemonic](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [SS58 Address Format](https://docs.substrate.io/reference/address-formats/)
- [Substrate Cryptography](https://docs.substrate.io/fundamentals/cryptography/)
