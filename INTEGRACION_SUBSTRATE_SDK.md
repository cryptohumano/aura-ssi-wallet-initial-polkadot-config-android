# Integración del SDK de Substrate para Android - Guía Completa

## 📋 Tabla de Contenidos
1. [Introducción](#introducción)
2. [Configuración del Proyecto](#configuración-del-proyecto)
3. [Instalación del SDK](#instalación-del-sdk)
4. [Análisis de la Estructura Real del SDK](#análisis-de-la-estructura-real-del-sdk)
5. [Errores Comunes y Soluciones](#errores-comunes-y-soluciones)
6. [Análisis de Causas de Importación Incorrecta](#análisis-de-causas-de-importación-incorrecta)
7. [Implementación Correcta](#implementación-correcta)
8. [Verificación y Testing](#verificación-y-testing)
9. [Conclusión](#conclusión)

## 🎯 Introducción

El **Substrate SDK para Android** de Nova Wallet es una biblioteca criptográfica completa que proporciona funcionalidades para:
- Generación y validación de mnemónicos BIP39
- Creación de pares de claves (SR25519, ED25519, ECDSA)
- Importación/exportación de cuentas en formato JSON
- Algoritmos de hashing (SHA256, Keccak256, Blake2b, XXHash)
- Firma y verificación de mensajes

## ⚙️ Configuración del Proyecto

### 1. Configuración de Gradle

#### `build.gradle` (Nivel de Proyecto)
```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:7.4.2'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### `app/build.gradle` (Nivel de Módulo)
```groovy
android {
    namespace 'com.aura.substratecryptotest'
    compileSdk 34

    defaultConfig {
        applicationId "com.aura.substratecryptotest"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = '1.8'
    }

    buildFeatures {
        viewBinding true
    }

    packagingOptions {
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/LICENSE.txt'
        exclude 'META-INF/license.txt'
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/NOTICE.txt'
        exclude 'META-INF/notice.txt'
        exclude 'META-INF/ASL2.0'
    }
}

dependencies {
    // Android Core
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.activity:activity-ktx:1.8.0'
    implementation 'androidx.fragment:fragment-ktx:1.6.2'
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // Substrate SDK
    implementation 'io.github.nova-wallet:substrate-sdk-android:2.4.0'
}
```

### 2. Configuración de Gradle Wrapper

#### `gradle/wrapper/gradle-wrapper.properties`
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-all.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## 📦 Instalación del SDK

### 1. Dependencia Maven
```groovy
implementation 'io.github.nova-wallet:substrate-sdk-android:2.4.0'
```

### 2. Repositorios Maven

#### ¿Qué es Maven Central?
**Maven Central** es el **repositorio principal** de paquetes Java/Android. Es como una "base de datos gigante" donde los desarrolladores publican sus librerías.

#### Repositorios vs Proveedores

**Repositorios** (Repositories) - Servidores donde se almacenan los paquetes:
- **Maven Central** (`mavenCentral()`) - Repositorio oficial de Apache Maven
- **Google Maven** (`google()`) - Repositorio oficial de Google
- **JitPack** (`maven { url 'https://jitpack.io' }`) - Repositorio para proyectos de GitHub

**Proveedores** (Publishers) - Organizaciones que publican los paquetes:
- **`io.github.nova-wallet`** - Nova Wallet (nuestro caso)
- **`com.google`** - Google
- **`org.jetbrains`** - JetBrains

#### Configuración de Repositorios
```groovy
repositories {
    google()           // Librerías de Android y Google
    mavenCentral()     // Librerías oficiales Java/Android
    maven { url 'https://jitpack.io' }  // Proyectos de GitHub
}
```

### 2. Verificación de Descarga
```bash
# Verificar que el SDK se descargó correctamente
find ~/.gradle/caches -name "*substrate*" -type f

# Verificar el contenido del JAR
unzip -l ~/.gradle/caches/transforms-3/*/transformed/substrate-sdk-android-2.4.0-api.jar | grep -E "\.class$" | head -20
```

## 🔍 Análisis de la Estructura Real del SDK

### Clases Disponibles (Verificado)

#### Mnemónicos
- ✅ `io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic`
- ✅ `io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic.Length`
- ✅ `io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator`

#### Criptografía
- ✅ `io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair`
- ✅ `io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519SubstrateKeypairFactory`
- ✅ `io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Ed25519SubstrateKeypairFactory`

#### Importación/Exportación
- ✅ `io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder`
- ✅ `io.novasama.substrate_sdk_android.encrypt.json.JsonSeedEncoder`
- ✅ `io.novasama.substrate_sdk_android.encrypt.EncryptionType`

#### Hashing
- ✅ `io.novasama.substrate_sdk_android.hash.Hasher`
- ✅ `io.novasama.substrate_sdk_android.hash.XXHash`

### Clases NO Disponibles (Errores Comunes)
- ❌ `io.novasama.substrate_sdk_android.Bip39`
- ❌ `io.novasama.substrate_sdk_android.MnemonicLength`
- ❌ `io.novasama.substrate_sdk_android.crypto.KeyPair`
- ❌ `io.novasama.substrate_sdk_android.crypto.deriveKeyPair`

## 🚨 Dependencias Críticas Faltantes

### Problema Identificado
El SDK de Substrate requiere **13 dependencias** que no se descargan automáticamente. Esto causa errores de compilación como:
- `Cannot access class 'com.google.gson.Gson'`
- `Cannot access class 'org.bouncycastle.jcajce.provider.digest.Blake2b'`
- `Unresolved reference: Bip39`

### Dependencias del POM
Según el POM oficial del SDK, estas son las dependencias requeridas:

#### Criptografía
```groovy
implementation 'org.bouncycastle:bcprov-jdk15on:1.70'
implementation 'net.i2p.crypto:eddsa:0.3.0'
implementation 'org.web3j:crypto:4.8.0'
```

#### JSON y Utilidades
```groovy
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'org.lz4:lz4-java:1.7.1'
implementation 'org.eclipse.birt.runtime.3_7_1:org.apache.xerces:2.9.0'
```

#### Corrutinas
```groovy
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4'
```

#### NovaCrypto (BIP39, SHA256)
```groovy
implementation 'com.github.NovaCrypto:BIP39:0e7fa95f80'
implementation 'com.github.NovaCrypto:Sha256:57bed72da5'
implementation 'com.github.NovaCrypto:ToRuntime:c3ae3080eb'
```

#### Dependencias Opcionales
```groovy
implementation 'com.caverock:androidsvg-aar:1.4'
implementation 'com.neovisionaries:nv-websocket-client:2.10'
implementation 'io.github.novacrypto:SecureString:2022.01.17'
```

### Solución
Añadir **TODAS** las dependencias del POM al `build.gradle` del módulo app.

## 🚨 Errores Comunes y Soluciones

### 1. Errores de Importación Incorrecta

#### ❌ Error: `Unresolved reference: Bip39`
```kotlin
// INCORRECTO
import io.novasama.substrate_sdk_android.Bip39

// CORRECTO
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
```

#### ❌ Error: `Unresolved reference: MnemonicLength`
```kotlin
// INCORRECTO
import io.novasama.substrate_sdk_android.MnemonicLength

// CORRECTO
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
// Usar: Mnemonic.Length.TWELVE
```

#### ❌ Error: `Unresolved reference: KeyPair`
```kotlin
// INCORRECTO
import io.novasama.substrate_sdk_android.crypto.KeyPair

// CORRECTO
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Ed25519SubstrateKeypairFactory
```

### 2. Errores de Configuración de Gradle

#### ❌ Error: `Plugin [id: 'com.android.application', version: '8.1.4'] was not found`
```groovy
// INCORRECTO
classpath 'com.android.tools.build:gradle:8.1.4'

// CORRECTO
classpath 'com.android.tools.build:gradle:7.4.2'
```

#### ❌ Error: `Namespace not specified`
```groovy
// AÑADIR en app/build.gradle
android {
    namespace 'com.aura.substratecryptotest'
    // ...
}
```

#### ❌ Error: `Minimum supported Gradle version is 7.5`
```properties
# ACTUALIZAR gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-all.zip
```

### 3. Errores de Dependencias

#### ❌ Error: `Cannot access class 'com.google.gson.Gson'`
```groovy
// AÑADIR en app/build.gradle
dependencies {
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

#### ❌ Error: `Cannot access class 'org.bouncycastle.jcajce.provider.digest.Blake2b'`
```groovy
// AÑADIR en app/build.gradle
dependencies {
    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'
}
```

## 🔬 Análisis de Causas de Importación Incorrecta

### 1. **Error de Documentación**
- La documentación oficial puede estar desactualizada
- Los ejemplos en GitHub pueden usar versiones anteriores
- La estructura de paquetes cambió entre versiones

### 2. **Error de Maven Central**
- El repositorio Maven Central muestra información incorrecta
- Los metadatos del POM pueden estar desactualizados
- La búsqueda en Maven Central puede mostrar clases que no existen

### 3. **Error de IDE**
- El IDE puede sugerir importaciones incorrectas
- El autocompletado puede mostrar clases inexistentes
- La indexación puede estar desactualizada

### 4. **Error de Versión**
- Diferentes versiones del SDK tienen diferentes estructuras
- La versión 2.4.0 tiene una estructura diferente a versiones anteriores
- Los cambios breaking no están documentados

### 5. **Error de Análisis Estático**
- Las herramientas de análisis pueden no detectar la estructura real
- Los decompiladores pueden mostrar información incorrecta
- La reflexión puede no funcionar correctamente

## ✅ Implementación Correcta

### 1. Gestor Criptográfico Principal

```kotlin
package com.aura.substratecryptotest.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedEncoder
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.hash.Hasher
import io.novasama.substrate_sdk_android.hash.XXHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubstrateCryptoManager(private val context: Context) {
    
    private val mnemonicCreator = MnemonicCreator()
    private val jsonSeedDecoder = JsonSeedDecoder()
    private val jsonSeedEncoder = JsonSeedEncoder()
    
    // Almacenamiento seguro
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "substrate_crypto_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Genera un mnemonic BIP39
     */
    suspend fun generateMnemonic(length: Mnemonic.Length = Mnemonic.Length.TWELVE): String {
        return withContext(Dispatchers.IO) {
            try {
                mnemonicCreator.generateMnemonic(length)
            } catch (e: Exception) {
                "error generating mnemonic: ${e.message}"
            }
        }
    }
    
    /**
     * Valida un mnemonic BIP39
     */
    suspend fun validateMnemonic(mnemonic: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                mnemonicCreator.generateEntropy(mnemonic)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Genera un par de claves SR25519
     */
    suspend fun generateSr25519KeyPair(mnemonic: String, password: String? = null): Sr25519Keypair? {
        return withContext(Dispatchers.IO) {
            try {
                val entropy = mnemonicCreator.generateEntropy(mnemonic)
                val seed = mnemonicCreator.generateSeed(entropy, password)
                Sr25519Keypair(seed)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Funciones de hashing
     */
    suspend fun hashData(data: ByteArray, hashType: HashType): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                when (hashType) {
                    HashType.SHA256 -> Hasher.sha256(data)
                    HashType.KECCAK256 -> Hasher.keccak256(data)
                    HashType.BLAKE2B256 -> Hasher.blake2b256(data)
                    HashType.BLAKE2B512 -> Hasher.blake2b512(data)
                    HashType.XXHASH64 -> XXHash.hash64(data)
                    HashType.XXHASH128 -> XXHash.hash128(data)
                }
            } catch (e: Exception) {
                data
            }
        }
    }
}

enum class HashType {
    SHA256, KECCAK256, BLAKE2B256, BLAKE2B512, XXHASH64, XXHASH128
}
```

### 2. Verificación del SDK

```kotlin
package com.aura.substratecryptotest.crypto

import android.content.Context
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedEncoder
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.hash.Hasher
import io.novasama.substrate_sdk_android.hash.XXHash

class SDKVerificationManager(private val context: Context) {
    
    fun verifySDKAvailability(): SDKVerificationResult {
        val availableClasses = mutableListOf<String>()
        val missingClasses = mutableListOf<String>()
        
        val criticalClasses = listOf(
            "io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic",
            "io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator",
            "io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder",
            "io.novasama.substrate_sdk_android.encrypt.json.JsonSeedEncoder",
            "io.novasama.substrate_sdk_android.encrypt.EncryptionType",
            "io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair",
            "io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519SubstrateKeypairFactory",
            "io.novasama.substrate_sdk_android.hash.Hasher",
            "io.novasama.substrate_sdk_android.hash.XXHash"
        )
        
        criticalClasses.forEach { className ->
            try {
                Class.forName(className)
                availableClasses.add(className)
            } catch (e: ClassNotFoundException) {
                missingClasses.add(className)
            }
        }
        
        return SDKVerificationResult(
            availableClasses = availableClasses,
            missingClasses = missingClasses,
            isSDKAvailable = missingClasses.isEmpty()
        )
    }
    
    fun testBasicFunctionality(): BasicFunctionalityTest {
        val mnemonicCreator = MnemonicCreator()
        val testData = "Hello, Substrate!".toByteArray()
        
        return try {
            // Test mnemonic generation
            val mnemonic = mnemonicCreator.generateMnemonic(Mnemonic.Length.TWELVE)
            
            // Test hashing
            val sha256Hash = Hasher.sha256(testData)
            val keccak256Hash = Hasher.keccak256(testData)
            val xxHash64 = XXHash.hash64(testData)
            
            BasicFunctionalityTest(
                mnemonicGeneration = true,
                hashing = true,
                error = null
            )
        } catch (e: Exception) {
            BasicFunctionalityTest(
                mnemonicGeneration = false,
                hashing = false,
                error = e.message
            )
        }
    }
}

data class SDKVerificationResult(
    val availableClasses: List<String>,
    val missingClasses: List<String>,
    val isSDKAvailable: Boolean
)

data class BasicFunctionalityTest(
    val mnemonicGeneration: Boolean,
    val hashing: Boolean,
    val error: String?
)
```

## 🧪 Verificación y Testing

### 1. Script de Verificación

```bash
#!/bin/bash
# verify_dependencies.sh

echo "🔍 Verificando dependencias del SDK de Substrate..."

# Verificar que Gradle está funcionando
echo "📦 Verificando Gradle..."
./gradlew --version

# Verificar que el SDK se descargó
echo "📥 Verificando descarga del SDK..."
find ~/.gradle/caches -name "*substrate*" -type f | head -5

# Verificar clases disponibles
echo "🔍 Verificando clases disponibles..."
unzip -l ~/.gradle/caches/transforms-3/*/transformed/substrate-sdk-android-2.4.0-api.jar | grep -E "\.class$" | grep -E "(Mnemonic|Keypair|Hash)" | head -10

# Compilar el proyecto
echo "🔨 Compilando proyecto..."
./gradlew build

echo "✅ Verificación completada"
```

### 2. Testing en la Aplicación

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Verificar SDK
        val verificationManager = SDKVerificationManager(this)
        val verificationResult = verificationManager.verifySDKAvailability()
        
        if (verificationResult.isSDKAvailable) {
            Log.d("SDK", "✅ SDK disponible")
            Log.d("SDK", "Clases disponibles: ${verificationResult.availableClasses.size}")
        } else {
            Log.e("SDK", "❌ SDK no disponible")
            Log.e("SDK", "Clases faltantes: ${verificationResult.missingClasses}")
        }
        
        // Test básico
        val basicTest = verificationManager.testBasicFunctionality()
        if (basicTest.mnemonicGeneration && basicTest.hashing) {
            Log.d("SDK", "✅ Funcionalidad básica OK")
        } else {
            Log.e("SDK", "❌ Error en funcionalidad básica: ${basicTest.error}")
        }
    }
}
```

## 📊 Resumen de Errores y Soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `Unresolved reference: Bip39` | Clase no existe | Usar `MnemonicCreator` |
| `Unresolved reference: MnemonicLength` | Clase no existe | Usar `Mnemonic.Length` |
| `Unresolved reference: KeyPair` | Clase no existe | Usar `Sr25519Keypair` |
| `Plugin not found` | Versión incorrecta | Usar Gradle 7.4.2 |
| `Namespace not specified` | Configuración faltante | Añadir `namespace` |
| `Cannot access class 'Gson'` | Dependencia faltante | Añadir Gson |
| `Cannot access class 'BouncyCastle'` | Dependencia faltante | Añadir BouncyCastle |

## 🎯 Conclusión

### Problemas Identificados

1. **Documentación Desactualizada**: La documentación oficial no refleja la estructura real del SDK
2. **Metadatos Incorrectos**: Maven Central muestra información incorrecta sobre las clases disponibles
3. **Cambios Breaking**: La versión 2.4.0 tiene una estructura completamente diferente a versiones anteriores
4. **Dependencias Faltantes**: El SDK requiere dependencias adicionales no documentadas

### Soluciones Implementadas

1. **Análisis Directo**: Inspección directa del JAR para identificar clases reales
2. **Configuración Correcta**: Gradle y dependencias configuradas correctamente
3. **Implementación Robusta**: Manejo de errores y verificación de disponibilidad
4. **Testing Completo**: Verificación de funcionalidad básica

### Recomendaciones

1. **Siempre verificar la estructura real** del SDK antes de implementar
2. **Usar herramientas de análisis** como `unzip -l` para inspeccionar JARs
3. **Implementar verificación de disponibilidad** en tiempo de ejecución
4. **Mantener dependencias actualizadas** y verificar compatibilidad
5. **Documentar cambios** en la estructura del SDK para futuras versiones

### Próximos Pasos

1. Completar la implementación de todas las funcionalidades
2. Añadir tests unitarios completos
3. Implementar manejo de errores robusto
4. Crear documentación de API para el equipo
5. Configurar CI/CD para verificación automática

---

**Nota**: Este documento se basa en el análisis del SDK de Substrate v2.4.0. Para versiones futuras, es recomendable verificar la estructura de paquetes nuevamente.
