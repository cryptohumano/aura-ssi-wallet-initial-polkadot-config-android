# Guía de Logging - Substrate Crypto Test

## Resumen de los Problemas Identificados y Solucionados

### Problemas Originales
1. **Uso de `println()`**: Los logs se mostraban como `System.out` en lugar de usar el sistema nativo de Android
2. **Falta de configuración**: No había configuración específica para logging en el build
3. **Logs inconsistentes**: Diferentes formatos y niveles de logging en toda la aplicación
4. **Logs en hilos de background**: Los logs se ejecutaban en `Dispatchers.IO` causando problemas de visibilidad

### Soluciones Implementadas

## 1. Sistema de Logging Nativo de Android

### Antes (Problemático)
```kotlin
println("🔑 KeyPairManager: Generando par de claves SR25519...")
```

### Después (Correcto)
```kotlin
Log.i("KeyPairManager", "🔑 Generando par de claves SR25519...")
```

## 2. Clase Logger Centralizada

Se creó una clase `Logger` centralizada en `/app/src/main/java/com/aura/substratecryptotest/utils/Logger.kt` que proporciona:

### Características Principales
- **Configuración centralizada**: Control de habilitación y niveles de logging
- **Métodos especializados**: Para diferentes componentes (KeyPair, Wallet, Mnemonic, etc.)
- **Formato consistente**: Logs con emojis y formato estándar
- **Control de build**: Diferentes configuraciones para debug y release

### Uso Básico
```kotlin
import com.aura.substratecryptotest.utils.Logger

// Logs básicos
Logger.i("MiTag", "Mensaje informativo")
Logger.d("MiTag", "Mensaje de debug")
Logger.e("MiTag", "Mensaje de error", exception)

// Logs especializados
Logger.keyPair("Generando par de claves...")
Logger.wallet("Creando wallet...")
Logger.mnemonic("Validando mnemonic...")

// Logs con formato especial
Logger.success("Operación", "Completada exitosamente", "Detalles adicionales")
Logger.error("Operación", "Error", "Descripción del error", exception)
Logger.warning("Operación", "Advertencia importante")
Logger.debug("Operación", "Información de debug")
```

## 3. Configuración en build.gradle

### Configuración de Build Types
```gradle
buildTypes {
    debug {
        debuggable true
        minifyEnabled false
        buildConfigField "boolean", "DEBUG_LOGGING", "true"
    }
    release {
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        buildConfigField "boolean", "DEBUG_LOGGING", "false"
    }
}
```

### Control de Logging
- **Debug**: Logging completo habilitado
- **Release**: Solo logs de error y warning

## 4. Niveles de Logging

### Jerarquía de Niveles
1. **VERBOSE** - Información muy detallada (solo debug)
2. **DEBUG** - Información de debug (solo debug)
3. **INFO** - Información general
4. **WARN** - Advertencias
5. **ERROR** - Errores

### Configuración de Niveles
```kotlin
// Cambiar nivel mínimo de logging
Logger.setMinLevel(Logger.Level.INFO)

// Habilitar/deshabilitar logging
Logger.setLoggingEnabled(true)
```

## 5. Mejores Prácticas

### ✅ Correcto
```kotlin
// Usar Logger centralizada
Logger.success("KeyPairManager", "Par de claves generado", "SR25519, 32 bytes")

// Logs específicos por componente
Logger.keyPair("Generando claves SR25519...")
Logger.wallet("Wallet creada exitosamente")

// Manejo de errores
try {
    // operación
} catch (e: Exception) {
    Logger.error("Componente", "Operación falló", e.message ?: "Error desconocido", e)
}
```

### ❌ Evitar
```kotlin
// No usar println()
println("Mensaje de log")

// No usar System.out
System.out.println("Mensaje")

// No usar Log directamente sin contexto
Log.d("", "Mensaje sin tag")
```

## 6. Cómo Ver los Logs

### Android Studio
1. Abrir **Logcat** (View → Tool Windows → Logcat)
2. Filtrar por tag: `KeyPairManager`, `WalletManager`, etc.
3. Seleccionar nivel de log apropiado

### Terminal/ADB
```bash
# Ver todos los logs
adb logcat

# Filtrar por tag específico
adb logcat -s KeyPairManager

# Filtrar por nivel
adb logcat *:E  # Solo errores
adb logcat *:I  # Info y superior
```

### Filtros Útiles
```bash
# Solo logs de la aplicación
adb logcat | grep "com.aura.substratecryptotest"

# Solo logs de KeyPairManager
adb logcat | grep "KeyPairManager"

# Solo errores y warnings
adb logcat *:W
```

## 7. Ejemplos de Uso en el Código

### KeyPairManager
```kotlin
Logger.keyPair("Generando par de claves SR25519...")
Logger.debug("KeyPairManager", "Mnemonic", "'${mnemonic.take(20)}...' (${mnemonic.length} chars)")
Logger.success("KeyPairManager", "Par de claves generado exitosamente", 
    "Algoritmo: ${keyPairInfo.algorithm}, Clave pública: ${keyPairInfo.publicKey.size} bytes")
```

### WalletManager
```kotlin
Logger.wallet("Iniciando creación de wallet...")
Logger.debug("WalletManager", "Configuración", "Nombre: $name, Algoritmo: $cryptoType")
Logger.success("WalletManager", "Wallet creada exitosamente")
```

## 8. Troubleshooting

### Si no ves los logs:
1. Verificar que estás en modo debug
2. Comprobar que `Logger.setLoggingEnabled(true)`
3. Verificar el nivel mínimo de logging
4. Usar filtros correctos en Logcat

### Si los logs son muy verbosos:
1. Cambiar nivel mínimo a `Logger.Level.INFO`
2. Usar `Logger.setLoggingEnabled(false)` en release
3. Filtrar por tags específicos en Logcat

## 9. Migración de Código Existente

### Paso 1: Reemplazar println()
```kotlin
// Antes
println("🔑 KeyPairManager: Mensaje")

// Después
Logger.keyPair("Mensaje")
```

### Paso 2: Usar métodos especializados
```kotlin
// Antes
Log.i("KeyPairManager", "✅ Par de claves generado")

// Después
Logger.success("KeyPairManager", "Par de claves generado")
```

### Paso 3: Configurar tags consistentes
```kotlin
// Usar tags descriptivos y consistentes
Logger.debug("KeyPairManager", "Operación", "Detalles")
Logger.debug("WalletManager", "Operación", "Detalles")
```

## Conclusión

El nuevo sistema de logging proporciona:
- ✅ Logs nativos de Android (no System.out)
- ✅ Configuración centralizada
- ✅ Formato consistente
- ✅ Control de niveles
- ✅ Métodos especializados por componente
- ✅ Mejor debugging y troubleshooting

Los logs ahora se mostrarán correctamente en Logcat y serán más fáciles de filtrar y analizar.


