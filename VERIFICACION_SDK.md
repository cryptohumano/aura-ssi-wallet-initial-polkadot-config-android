# 🔍 Verificación del SDK de Nova Wallet

Este documento explica cómo verificar que el SDK de Substrate de Nova Wallet esté funcionando correctamente en tu aplicación Android.

## 📋 Métodos de Verificación

### 1. **Verificación en la Aplicación** (Recomendado)

La aplicación incluye una pestaña dedicada "Verificar SDK" que te permite:

- ✅ **Verificación Completa**: Ejecuta todas las pruebas del SDK
- 📦 **Verificación de Dependencias**: Comprueba que todas las clases estén disponibles
- 🧪 **Pruebas Individuales**: Prueba funciones específicas (BIP39, Hash, Claves)

#### Cómo usar:
1. Ejecuta la aplicación
2. Ve a la pestaña "Verificar SDK"
3. Presiona "🚀 Verificar SDK" para una verificación completa
4. Presiona "📦 Dependencias" para verificar las clases disponibles
5. Usa los botones de prueba individual para funciones específicas

### 2. **Verificación por Línea de Comandos**

```bash
# Ejecutar el script de verificación
./verify_dependencies.sh

# O verificar manualmente
./gradlew app:dependencies | grep substrate-sdk-android
```

### 3. **Verificación Programática**

El código incluye verificadores automáticos:

```kotlin
// Verificar dependencias
val dependencyChecker = DependencyChecker(context)
val result = dependencyChecker.checkSDKDependencies()

// Verificar funcionalidad del SDK
val verificationManager = SDKVerificationManager(context)
val sdkResult = verificationManager.verifySDKAvailability()
```

## 🔧 Solución de Problemas

### ❌ **Problema: SDK no encontrado**

**Síntomas:**
- Error: "ClassNotFoundException: io.github.novasamatech.substrate_sdk_android.Bip39"
- La verificación muestra "Dependencias faltantes"

**Soluciones:**
1. **Verificar build.gradle**:
   ```gradle
   dependencies {
       implementation 'io.github.nova-wallet:substrate-sdk-android:2.4.0'
   }
   ```

2. **Limpiar y reconstruir**:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

3. **Verificar conectividad**:
   - Asegúrate de tener conexión a internet
   - Verifica que no haya firewall bloqueando Maven Central

### ❌ **Problema: Funciones no disponibles**

**Síntomas:**
- Error: "NoSuchMethodError"
- La verificación muestra "Métodos faltantes"

**Soluciones:**
1. **Verificar versión del SDK**:
   - Asegúrate de usar la versión 2.4.0 o superior
   - Verifica que no haya conflictos de versiones

2. **Verificar ProGuard**:
   - Asegúrate de que las reglas de ProGuard no estén obfuscando el SDK
   - Revisa el archivo `proguard-rules.pro`

### ❌ **Problema: Compilación falla**

**Síntomas:**
- Error de compilación al incluir el SDK
- "Could not resolve dependency"

**Soluciones:**
1. **Verificar repositorios**:
   ```gradle
   repositories {
       google()
       mavenCentral()
       maven { url 'https://jitpack.io' }
   }
   ```

2. **Verificar versión de Gradle**:
   - Usa Gradle 8.0 o superior
   - Verifica la compatibilidad con Android Gradle Plugin

## 📊 Interpretación de Resultados

### ✅ **Verificación Exitosa**
```
✅ SDK completamente funcional
📊 Estadísticas:
• Disponibles: 15
• Faltantes: 0
• Errores: 0
• Pruebas exitosas: 8
```

### ❌ **Verificación con Problemas**
```
❌ SDK con problemas (3 faltantes, 2 errores)
📊 Estadísticas:
• Disponibles: 12
• Faltantes: 3
• Errores: 2
• Pruebas exitosas: 5
```

## 🧪 Pruebas Específicas

### **BIP39 (Mnemonic)**
- ✅ Generación de mnemonic
- ✅ Validación de mnemonic
- ✅ Conversión a entropía
- ✅ Generación de semilla

### **Funciones de Hash**
- ✅ SHA256
- ✅ Keccak256
- ✅ Blake2b256/512
- ✅ XXHash64/128

### **Pares de Claves**
- ✅ SR25519
- ✅ ED25519
- ✅ ECDSA
- ✅ Derivación de claves

### **Encriptación/Desencriptación**
- ✅ JsonSeedDecoder
- ✅ JsonSeedEncoder
- ✅ Tipos de encriptación

## 📚 Recursos Adicionales

- [Documentación del SDK](https://github.com/novasamatech/substrate-sdk-android)
- [Ejemplos de uso](https://github.com/novasamatech/substrate-sdk-android/tree/main/sample)
- [Issues y soporte](https://github.com/novasamatech/substrate-sdk-android/issues)

## 🚀 Próximos Pasos

Una vez que la verificación sea exitosa:

1. **Implementa las funciones criptográficas** en tu aplicación
2. **Prueba con datos reales** de la red Substrate
3. **Integra con APIs de red** para transacciones
4. **Implementa medidas de seguridad** adicionales

---

**💡 Tip**: Ejecuta la verificación regularmente durante el desarrollo para asegurar que el SDK siga funcionando correctamente.
