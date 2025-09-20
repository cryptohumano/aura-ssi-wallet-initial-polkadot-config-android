# 🧪 Pruebas de Verificación Criptográfica

Este directorio contiene las pruebas de verificación para asegurar que la generación de claves criptográficas es correcta y compatible con el SDK de Substrate.

## 📁 Estructura de Pruebas

```
app/src/test/java/com/aura/substratecryptotest/crypto/
└── KeyVerificationTest.kt
```

## 🔍 Pruebas Disponibles

### 1. **testSdkCompatibility()**
- **Propósito**: Verifica compatibilidad con el SDK de referencia de Substrate
- **Mnemonic de prueba**: `bottom drive obey lake curtain smoke basket hold race lonely fit walk`
- **Clave pública esperada**: `46ebddef8cd9bb167dc30878d7113b7e168e6f0646beffd77d69d39bad76b47a`
- **Dirección esperada**: `5DfhGyQdFobKM8NsWvEeAKk5EQQgYe9AydgJ7rMB6E1EqRzV`

### 2. **testKeyGenerationWithKnownData()**
- **Propósito**: Verifica generación con mnemonic BIP39 estándar
- **Mnemonic de prueba**: `abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about`
- **Verificación**: Valida que las claves generadas son verificables

### 3. **testSubstrateDevelopmentMnemonic()**
- **Propósito**: Verifica mnemonic de desarrollo estándar de Substrate
- **Uso**: Genera cuentas conocidas como Alice, Bob, Charlie, etc.

## 🚀 Cómo Ejecutar las Pruebas

### Desde Android Studio:
1. Abre el proyecto en Android Studio
2. Navega a `app/src/test/java/com/aura/substratecryptotest/crypto/KeyVerificationTest.kt`
3. Haz clic derecho en la clase o método de prueba
4. Selecciona "Run 'KeyVerificationTest'"

### Desde Terminal:
```bash
./gradlew test
```

## ✅ Criterios de Éxito

Las pruebas son exitosas cuando:
- ✅ Las claves generadas coinciden exactamente con las esperadas
- ✅ Las direcciones SS58 son correctas
- ✅ La verificación de firmas digitales es exitosa
- ✅ La regeneración desde mnemonic produce las mismas claves

## 🔧 Configuración

Las pruebas utilizan:
- **SDK de Substrate**: `io.novasama.substrate_sdk_android:2.4.0`
- **Algoritmo**: SR25519
- **Codificación**: SS58 para direcciones
- **Derivación**: PKCS5S2ParametersGenerator con SHA512

## 📝 Notas Importantes

- Las pruebas son **determinísticas** - siempre producen los mismos resultados
- Los mnemónicos de prueba son **oficiales** y están documentados
- Las claves esperadas están **verificadas** contra herramientas oficiales de Substrate
- Las pruebas se ejecutan en el **hilo principal** usando `runBlocking`

## 🐛 Solución de Problemas

Si las pruebas fallan:
1. Verifica que el SDK de Substrate esté correctamente configurado
2. Confirma que las dependencias de BouncyCastle estén presentes
3. Revisa los logs para identificar errores específicos
4. Asegúrate de que el mnemonic de prueba sea exacto (sin espacios extra)
