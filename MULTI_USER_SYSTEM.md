# Sistema de Múltiples Usuarios con Autenticación Biométrica

## 📋 Resumen

Este sistema implementa un **sistema robusto de múltiples usuarios** con **autenticación biométrica obligatoria** para todas las operaciones críticas. Cada usuario tiene su propia base de datos aislada y sus datos están protegidos por TouchID/FaceID.

## 🔐 Características Principales

### ✅ **Autenticación Biométrica Obligatoria**
- **TouchID/FaceID requerido** para todas las operaciones de escritura
- **Acceso a datos sensibles** (mnemónicos, claves privadas) protegido
- **Cambio de usuario** requiere autenticación biométrica
- **Timeout automático** de sesiones (5 minutos)

### ✅ **Aislamiento Completo por Usuario**
- **Base de datos separada** para cada usuario usando Room Database
- **Almacenamiento seguro** de llaves privadas en Android KeyStore
- **Datos encriptados** específicos por usuario
- **Sin acceso cruzado** entre usuarios

### ✅ **Gestión de Sesiones**
- **Sesiones activas** con timeout automático
- **Cambio de usuario** con autenticación biométrica
- **Cierre de sesión** automático y manual
- **Renovación de sesión** en cada actividad

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                     │
├─────────────────────────────────────────────────────────────┤
│  UserManagementScreen  │  UserManagementViewModel          │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE SEGURIDAD                       │
├─────────────────────────────────────────────────────────────┤
│  UserManager  │  BiometricAuthInterceptor  │  KeyStoreManager │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE DATOS                           │
├─────────────────────────────────────────────────────────────┤
│  SecureUserRepository  │  UserDatabaseManager  │  Room DB     │
└─────────────────────────────────────────────────────────────┘
```

## 📁 Componentes Principales

### 1. **UserManager** - Gestión de Usuarios
```kotlin
// Crear nuevo usuario
val result = userManager.registerNewUser("Juan Pérez", requireBiometric = true)

// Autenticar usuario existente
val authResult = userManager.authenticateUser(userId, requireBiometric = true)

// Cambiar usuario
val switchResult = userManager.switchUser(targetUserId, requireBiometric = true)
```

### 2. **BiometricAuthInterceptor** - Autenticación Biométrica
```kotlin
// Requerir autenticación para operaciones críticas
val authResult = biometricAuthInterceptor.requireBiometricAuthForWriteOperation("creación de wallet")

// Requerir autenticación para datos sensibles
val sensitiveAuth = biometricAuthInterceptor.requireBiometricAuthForSensitiveData("mnemonic")
```

### 3. **SecureUserRepository** - Repositorio Seguro
```kotlin
// Crear wallet (requiere TouchID/FaceID)
val walletResult = secureUserRepository.createUserWallet(
    walletName = "Mi Wallet",
    mnemonic = "abandon abandon...",
    publicKey = publicKeyBytes,
    privateKey = privateKeyBytes,
    address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
    cryptoType = "SR25519",
    derivationPath = "//0",
    requireBiometric = true
)

// Obtener mnemonic (requiere TouchID/FaceID)
val mnemonicResult = secureUserRepository.getWalletMnemonic(walletId, requireBiometric = true)
```

### 4. **UserDatabaseManager** - Base de Datos Aislada
```kotlin
// Cada usuario tiene su propia base de datos
val userDb = databaseManager.getUserDatabase(userId)

// Cambiar a base de datos de otro usuario
val newDb = databaseManager.switchToUserDatabase(targetUserId)
```

## 🔒 Flujo de Seguridad

### **Operación de Escritura (Crear Wallet)**
```
1. Usuario solicita crear wallet
2. Sistema verifica usuario activo
3. 🔐 REQUIERE TouchID/FaceID
4. Usuario autentica con biometría
5. Sistema encripta datos con clave específica del usuario
6. Datos se guardan en base de datos del usuario
7. ✅ Operación completada
```

### **Acceso a Datos Sensibles (Mnemonic)**
```
1. Usuario solicita mnemonic
2. Sistema verifica usuario activo
3. 🔐 REQUIERE TouchID/FaceID
4. Usuario autentica con biometría
5. Sistema desencripta datos del KeyStore
6. ✅ Mnemonic devuelto al usuario
```

### **Cambio de Usuario**
```
1. Usuario solicita cambiar a otro usuario
2. Sistema cierra sesión actual
3. 🔐 REQUIERE TouchID/FaceID
4. Usuario autentica con biometría
5. Sistema abre base de datos del nuevo usuario
6. ✅ Usuario cambiado exitosamente
```

## 📊 Almacenamiento de Datos

### **Android KeyStore (Por Usuario)**
```
Usuario 1:
├── user1_mnemonic (encriptado con TouchID)
├── user1_wallet1_private_key (encriptado con TouchID)
└── user1_wallet2_private_key (encriptado con TouchID)

Usuario 2:
├── user2_mnemonic (encriptado con TouchID)
├── user2_wallet1_private_key (encriptado con TouchID)
└── user2_wallet2_private_key (encriptado con TouchID)
```

### **Room Database (Por Usuario)**
```
user_db_user1:
├── user_wallets
├── user_documents
└── user_kilt_identities

user_db_user2:
├── user_wallets
├── user_documents
└── user_kilt_identities
```

## 🚀 Uso del Sistema

### **1. Crear Usuario**
```kotlin
val userManager = UserManager(context)
val result = userManager.registerNewUser("Juan Pérez", requireBiometric = true)
```

### **2. Crear Wallet**
```kotlin
val secureRepo = SecureUserRepository.getInstance(context)
val walletResult = secureRepo.createUserWallet(
    walletName = "Mi Wallet Principal",
    mnemonic = "abandon abandon abandon...",
    publicKey = publicKeyBytes,
    privateKey = privateKeyBytes,
    address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
    cryptoType = "SR25519",
    derivationPath = "//0",
    requireBiometric = true
)
```

### **3. Cambiar Usuario**
```kotlin
val switchResult = secureRepo.switchUser(targetUserId)
```

### **4. Acceder a Mnemonic**
```kotlin
val mnemonicResult = secureRepo.getWalletMnemonic(walletId, requireBiometric = true)
```

## 🔧 Configuración Requerida

### **Dependencias en build.gradle**
```gradle
implementation "androidx.biometric:biometric:1.1.0"
implementation "androidx.room:room-runtime:2.4.3"
implementation "androidx.room:room-ktx:2.4.3"
implementation "androidx.security:security-crypto:1.1.0-alpha06"
```

### **Permisos en AndroidManifest.xml**
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

## 🛡️ Seguridad Implementada

### **✅ Autenticación Biométrica**
- TouchID/FaceID obligatorio para operaciones críticas
- Timeout de 30 segundos para claves biométricas
- Fallback a contraseña generada si no hay biometría

### **✅ Encriptación de Datos**
- AES-256-GCM para encriptación de datos sensibles
- Claves específicas por usuario en Android KeyStore
- IV único para cada operación de encriptación

### **✅ Aislamiento de Datos**
- Base de datos separada por usuario
- SharedPreferences específicos por usuario
- Sin acceso cruzado entre usuarios

### **✅ Gestión de Sesiones**
- Timeout automático de 5 minutos
- Renovación de sesión en cada actividad
- Cierre automático al cambiar usuario

## 📱 Interfaz de Usuario

### **UserManagementScreen**
- Lista de usuarios registrados
- Usuario actual destacado
- Botones para crear, cambiar y eliminar usuarios
- Confirmación biométrica para todas las acciones

### **Funcionalidades**
- ✅ Crear nuevo usuario
- ✅ Cambiar entre usuarios
- ✅ Eliminar usuario
- ✅ Ver usuario actual
- ✅ Cerrar sesión

## 🧪 Ejemplo de Uso Completo

```kotlin
class MultiUserSystemExample(private val context: Context) {
    
    suspend fun demonstrateCompleteSystem() {
        // 1. Crear usuarios
        val user1 = userManager.registerNewUser("Usuario Principal", requireBiometric = true)
        val user2 = userManager.registerNewUser("Usuario Secundario", requireBiometric = true)
        
        // 2. Crear wallets para cada usuario
        val wallet1 = secureRepo.createUserWallet(/* datos usuario 1 */)
        secureRepo.switchUser(user2.id)
        val wallet2 = secureRepo.createUserWallet(/* datos usuario 2 */)
        
        // 3. Demostrar aislamiento
        secureRepo.switchUser(user1.id)
        val user1Wallets = secureRepo.getUserWallets() // Solo wallets de usuario 1
        
        // 4. Acceder a datos sensibles
        val mnemonic = secureRepo.getWalletMnemonic(wallet1.id, requireBiometric = true)
    }
}
```

## 🎯 Beneficios del Sistema

### **🔐 Seguridad Máxima**
- TouchID/FaceID obligatorio para operaciones críticas
- Datos encriptados específicos por usuario
- Aislamiento completo entre usuarios

### **👥 Múltiples Usuarios**
- Cada usuario tiene su propia base de datos
- Sin interferencia entre usuarios
- Cambio fácil entre usuarios

### **⚡ Rendimiento**
- Base de datos optimizada por usuario
- Carga rápida de datos específicos
- Gestión eficiente de memoria

### **🛠️ Mantenibilidad**
- Código modular y bien estructurado
- Fácil extensión para nuevos usuarios
- Logging completo para debugging

## 📝 Notas Importantes

1. **TouchID/FaceID es OBLIGATORIO** para operaciones críticas
2. **Cada usuario tiene datos completamente aislados**
3. **Las sesiones expiran automáticamente** después de 5 minutos
4. **El cambio de usuario requiere autenticación biométrica**
5. **Los datos se almacenan de forma segura** en Android KeyStore

Este sistema proporciona la máxima seguridad para aplicaciones que manejan datos sensibles como wallets de criptomonedas, con soporte completo para múltiples usuarios y autenticación biométrica obligatoria.



