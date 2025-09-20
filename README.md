# Substrate Crypto Test - Aplicación Android

Esta aplicación demuestra el uso completo del SDK de Substrate para Android de Nova Wallet, implementando todas las funciones criptográficas principales para el ecosistema Substrate/Polkadot.

## 🚀 Características Principales

### 1. Gestión de Wallets ✅ IMPLEMENTED
- **Crear wallets**: ✅ Generación de nuevos wallets con mnemonic BIP39 real
- **Importar wallets**: ✅ Desde mnemonic o archivos JSON compatibles con Polkadot.js
- **Exportar wallets**: ✅ A formato JSON para interoperabilidad
- **Gestión de múltiples wallets**: ✅ Crear, seleccionar y eliminar wallets
- **Mostrar mnemonic**: ✅ Visualización del mnemonic generado en la UI

### 2. Funciones Criptográficas ⚠️ PARTIALLY IMPLEMENTED
- **Generación de mnemonic BIP39**: ✅ Soporte para 12, 15, 18, 21 y 24 palabras
- **Validación de mnemonic**: ✅ Verificación de la validez de frases mnemotécnicas
- **Generación de pares de claves**: ⚠️ SR25519, ED25519 básicos (ECDSA no implementado)
- **Derivación de claves**: ⚠️ Soporte básico para rutas de derivación
- **Generación de seeds**: ✅ Con PBKDF2 (con/sin contraseña)
- **Análisis de fortaleza**: ✅ WEAK/MEDIUM/STRONG según longitud
- **Extracción de claves**: ❌ getPublicKey/getPrivateKey retornan null

### 3. Algoritmos de Hash ✅ IMPLEMENTED
- **BLAKE2b-128/256/512**: ✅ Algoritmos principales de Substrate
- **Keccak-256**: ✅ Algoritmo usado en Ethereum
- **SHA-256/512**: ✅ Algoritmos de hash estándar
- **XXHash64/128/256**: ✅ Algoritmos de hash rápidos
- **BLAKE2b-128 concat**: ✅ Para derivación de claves

### 4. Seguridad ✅ IMPLEMENTED
- **Almacenamiento seguro**: ✅ Uso de EncryptedSharedPreferences
- **Protección de datos**: ✅ Exclusión de datos sensibles de backups
- **Gestión de claves**: ✅ Almacenamiento seguro de pares de claves
- **Claves maestras**: ✅ AES256-GCM para encriptación

## 🛠️ Tecnologías Utilizadas

- **Android SDK**: API 24+ (Android 7.0+)
- **Kotlin**: Lenguaje de programación principal
- **Substrate SDK Android**: v2.4.0 de Nova Wallet
- **Material Design 3**: Interfaz de usuario moderna
- **ViewBinding**: Binding de vistas type-safe
- **LiveData/ViewModel**: Arquitectura MVVM
- **Coroutines**: Programación asíncrona
- **EncryptedSharedPreferences**: Almacenamiento seguro

## 📱 Estructura de la Aplicación

### Pantallas Principales ✅ IMPLEMENTED
1. **Lista de Wallets**: ✅ Gestión de todos los wallets creados con mnemonic visible
2. **Importar/Exportar**: ✅ Funciones de interoperabilidad
3. **Verificar SDK**: ✅ Verificación de disponibilidad del SDK

### Arquitectura Modular ✅ IMPLEMENTED
```
app/
├── crypto/
│   ├── mnemonic/
│   │   └── MnemonicManager.kt       # ✅ Gestión de mnemónicos BIP39
│   ├── keypair/
│   │   └── KeyPairManager.kt        # ✅ Gestión de pares de claves
│   ├── junction/
│   │   └── JunctionManager.kt       # ✅ Sistema de derivación
│   ├── hash/
│   │   └── HashManager.kt           # ✅ Funciones de hashing
│   ├── ss58/
│   │   └── SS58Encoder.kt           # ⚠️ Codificación SS58 (placeholder)
│   └── StorageManager.kt            # ✅ Almacenamiento seguro
├── wallet/
│   └── WalletManager.kt             # ✅ Gestión de wallets
├── ui/
│   ├── WalletListFragment.kt        # ✅ Lista de wallets
│   ├── ImportExportFragment.kt      # ✅ Importar/Exportar
│   ├── SDKVerificationFragment.kt   # ✅ Verificación SDK
│   └── WalletAdapter.kt             # ✅ Adaptador con mnemonic
└── MainActivity.kt                   # ✅ Actividad principal
```

## 🔧 Configuración e Instalación

### Requisitos
- Android Studio Arctic Fox o superior
- Android SDK API 24+
- Kotlin 1.9.10+

### Pasos de Instalación
1. Clona el repositorio
2. Abre el proyecto en Android Studio
3. Sincroniza las dependencias
4. Ejecuta la aplicación en un dispositivo o emulador

### Dependencias Principales
```gradle
implementation 'io.github.nova-wallet:substrate-sdk-android:2.4.0'
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

## 📖 Uso de la Aplicación

### Crear un Wallet
1. Toca el botón "+" en la pantalla principal
2. Se generará automáticamente un nuevo wallet con mnemonic de 12 palabras
3. El wallet aparecerá en la lista de wallets

### Importar un Wallet
1. Ve a la pestaña "Importar/Exportar"
2. Selecciona "Importar desde Mnemonic" o "Importar desde JSON"
3. Completa los campos requeridos
4. Toca "Importar"

### Usar Herramientas Crypto
1. Ve a la pestaña "Herramientas Crypto"
2. **Generar Mnemonic**: Selecciona la longitud y toca "Generar"
3. **Validar Mnemonic**: Ingresa un mnemonic y toca "Validar"
4. **Generar Hash**: Selecciona el tipo de hash, ingresa datos y toca "Generar"
5. **Firmar Mensaje**: Ingresa un mensaje y toca "Firmar"

## 🔐 Consideraciones de Seguridad

### Almacenamiento Seguro
- Los pares de claves se almacenan usando `EncryptedSharedPreferences`
- Las claves privadas nunca se almacenan en texto plano
- Los datos sensibles están excluidos de backups automáticos

### Mejores Prácticas
- Siempre valida los mnemonics antes de usarlos
- Usa contraseñas fuertes para la exportación
- No compartas mnemonics o claves privadas
- Mantén la aplicación actualizada

## 🚧 Limitaciones Actuales

- ⚠️ **SS58Encoder**: Implementación placeholder, requiere SDK real
- ⚠️ **ECDSA**: Algoritmo placeholder, requiere implementación completa
- ⚠️ **Firma de mensajes**: Requiere implementación completa del par de claves privado
- ⚠️ **Transacciones de red**: Solo funciones criptográficas, no hay soporte de red

## 🔮 Próximas Mejoras

- [ ] **Implementar SS58Encoder real** con SDK de Substrate
- [ ] **Completar ECDSA** para compatibilidad Bitcoin/Ethereum
- [ ] **Firma de mensajes** con pares de claves privados
- [ ] **Soporte para múltiples redes** Substrate
- [ ] **Integración con APIs de red** para transacciones
- [ ] **Soporte para hardware wallets**
- [ ] **Funciones de backup avanzadas** con QR codes

## 📚 Recursos Adicionales

- [Documentación del SDK de Substrate](https://github.com/novasamatech/substrate-sdk-android)
- [Documentación de Polkadot](https://polkadot.network/docs/)
- [Especificación BIP39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [Material Design 3](https://m3.material.io/)

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 📊 Estado del Proyecto

### ✅ **COMPLETADO Y FUNCIONANDO**
- **MnemonicManager**: ✅ Generación, validación, importación, backup/restore
- **HashManager**: ✅ BLAKE2b, Keccak, SHA, XXHash con BouncyCastle
- **StorageManager**: ✅ Almacenamiento seguro con EncryptedSharedPreferences
- **WalletManager**: ✅ Gestión completa de wallets con UI
- **UI Components**: ✅ Lista de wallets, import/export, verificación SDK

### ⚠️ **PARCIALMENTE IMPLEMENTADO**
- **KeyPairManager**: ⚠️ SR25519, ED25519 básicos (getPublicKey/getPrivateKey retornan null)
- **JunctionManager**: ⚠️ Sistema básico de derivación (implementación placeholder)

### ❌ **NO IMPLEMENTADO**
- **SS58Encoder**: ❌ Requiere implementación real del SDK
- **ECDSA**: ❌ Algoritmo no implementado

### 🧪 **TESTING**
- ✅ Compilación sin errores
- ✅ Instalación en dispositivo Android
- ✅ Generación de mnemónicos reales
- ✅ Visualización en UI
- ✅ Integración con SDK de Substrate

---

**⚠️ Advertencia**: Esta aplicación es para fines educativos y de demostración. Para uso en producción, implementa medidas de seguridad adicionales y auditorías de código.
