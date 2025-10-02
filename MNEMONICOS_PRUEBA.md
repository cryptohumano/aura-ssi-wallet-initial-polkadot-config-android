# 🧪 Mnemónicos de Prueba para Verificación de Claves SR25519

## 📋 Resumen

Este documento proporciona mnemónicos de prueba estándar para verificar que la generación de claves SR25519 en tu aplicación es correcta y compatible con el ecosistema Substrate/Polkadot.

## 🔑 Mnemónicos de Prueba Disponibles

### 1. **Mnemonic de Desarrollo Estándar de Substrate** (Recomendado)

```
bottom drive obey lake curtain smoke basket hold race lonely fit walk
```

**Características:**
- ✅ **Oficial de Substrate**: Es el mnemonic de desarrollo estándar
- ✅ **Genera cuentas conocidas**: Alice, Bob, Charlie, etc.
- ✅ **Compatible con subkey**: Puedes verificar con la herramienta oficial
- ✅ **12 palabras**: Cumple con BIP39 estándar

**Uso en tu aplicación:**
```kotlin
// Ejecutar prueba
walletManager.testSubstrateDevelopmentMnemonic()
```

### 2. **Mnemonic de Prueba BIP39 Estándar**

```
abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
```

**Características:**
- ✅ **BIP39 estándar**: Mnemonic de prueba conocido
- ✅ **12 palabras**: Longitud estándar
- ✅ **Fácil de recordar**: Todas las palabras son "abandon" excepto la última

**Uso en tu aplicación:**
```kotlin
// Ejecutar prueba
walletManager.testKeyGenerationWithKnownData()
```

## 🔍 Cómo Verificar con Herramientas Externas

### 1. **Usando subkey (Herramienta Oficial de Substrate)**

```bash
# Instalar subkey
cargo install --force subkey

# Verificar mnemonic de desarrollo de Substrate
subkey inspect "bottom drive obey lake curtain smoke basket hold race lonely fit walk"

# Verificar mnemonic BIP39 estándar
subkey inspect "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
```

**Salida esperada:**
```
Secret Key URI `bottom drive obey lake curtain smoke basket hold race lonely fit walk` is account:
  Network ID:        substrate
  Secret seed:       0x...
  Public key (hex):  0x...
  Account ID:        0x...
  SS58 Address:      5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY
```

### 2. **Usando Polkadot.js Apps**

1. Ve a https://polkadot.js.org/apps/
2. Ve a "Accounts" → "Add account"
3. Usa cualquiera de los mnemónicos de prueba
4. Verifica que la dirección generada coincida con la de tu aplicación

### 3. **Usando BIP39 Online**

1. Ve a https://bip39.online/
2. Ingresa el mnemonic de prueba
3. Selecciona "Substrate" como derivación
4. Compara las claves generadas

## 🧪 Pruebas Implementadas en tu Aplicación

### 1. **Verificación Automática**

Cada vez que creas una wallet, el sistema automáticamente:
- ✅ Genera las claves SR25519
- ✅ Verifica la estructura de las claves (32 bytes cada una)
- ✅ Prueba la firma digital
- ✅ Verifica compatibilidad con Substrate
- ✅ Confirma regeneración desde mnemonic

### 2. **Prueba con Mnemonic de Desarrollo de Substrate**

```kotlin
// En MainActivity o donde necesites ejecutar la prueba
runSubstrateDevelopmentTest()
```

**Logs esperados:**
```
D WalletManager: 🧪 Iniciando prueba con mnemonic de desarrollo de Substrate...
D KeyVerificationManager: Verificando mnemonic de desarrollo de Substrate
D KeyVerificationManager: Clave pública generada: [hex]
D KeyVerificationManager: Dirección generada: [SS58 address]
I WalletManager: 🎉 PRUEBA SUBSTRATE EXITOSA
```

### 3. **Prueba con Mnemonic BIP39 Estándar**

```kotlin
// En MainActivity o donde necesites ejecutar la prueba
runKeyVerificationTest()
```

**Logs esperados:**
```
D WalletManager: 🧪 Iniciando prueba con datos conocidos...
D KeyVerificationManager: Verificando mnemonic conocido abandon abandon...
D KeyVerificationManager: Clave pública generada: [hex]
D KeyVerificationManager: Dirección generada: [SS58 address]
I WalletManager: 🎉 PRUEBA EXITOSA
```

## 📊 Interpretación de Resultados

### ✅ **Prueba Exitosa**
```
I WalletManager: 🎉 PRUEBA SUBSTRATE EXITOSA
D WalletManager: Mnemonic de desarrollo de Substrate generó claves válidas y verificables
```

**Significa:**
- Las claves SR25519 son criptográficamente válidas
- La firma digital funciona correctamente
- Las claves son compatibles con Substrate
- El mnemonic genera consistentemente las mismas claves

### ❌ **Prueba Fallida**
```
E WalletManager: ❌ PRUEBA SUBSTRATE FALLIDA
E WalletManager: Error en prueba Substrate: [descripción del error]
```

**Posibles causas:**
- Error en la implementación de SR25519
- Problema con la derivación de claves
- Incompatibilidad con el SDK de Substrate
- Error en la generación de seed

## 🔧 Solución de Problemas

### Problema: "Firma no válida"
**Causa**: Error en la implementación de SR25519
**Solución**: Verificar que el SDK de Substrate esté correctamente integrado

### Problema: "Claves incompatibles"
**Causa**: Tamaño incorrecto de claves
**Solución**: Verificar implementación de `extractPublicKey()` y `extractPrivateKey()`

### Problema: "Regeneración fallida"
**Causa**: El mnemonic no genera las mismas claves
**Solución**: Verificar implementación de `generateSeed()` en `MnemonicManager`

## 📈 Validación Cruzada

### 1. **Comparar con subkey**
```bash
# Generar con subkey
subkey inspect "bottom drive obey lake curtain smoke basket hold race lonely fit walk"

# Comparar con tu aplicación
# Deben generar la misma clave pública y dirección
```

### 2. **Comparar con Polkadot.js**
1. Usa el mismo mnemonic en Polkadot.js Apps
2. Compara la dirección SS58 generada
3. Deben coincidir exactamente

### 3. **Verificar Consistencia**
1. Ejecuta la misma prueba múltiples veces
2. Debe generar siempre las mismas claves
3. Si no es consistente, hay un problema en la implementación

## 🎯 Casos de Prueba Adicionales

### 1. **Mnemonic con Contraseña**
```
Mnemonic: "bottom drive obey lake curtain smoke basket hold race lonely fit walk"
Password: "test"
```
**Esperado**: Debe generar claves diferentes a sin contraseña

### 2. **Diferentes Rutas de Derivación**
```
//Alice
//Bob
//Charlie
//Dave
//Eve
//Ferdie
```

### 3. **Mnemónicos de Diferentes Longitudes**
```
12 palabras: abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
15 palabras: abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
18 palabras: abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
21 palabras: abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
24 palabras: abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art
```

## 📝 Logs de Verificación

### Filtros Útiles
```bash
# Solo pruebas de Substrate
adb logcat | grep "SUBSTRATE"

# Solo pruebas exitosas
adb logcat | grep "EXITOSA"

# Solo errores de verificación
adb logcat | grep "Error en prueba"
```

### Estructura de Logs
```
D WalletManager: 🧪 Iniciando prueba con mnemonic de desarrollo de Substrate...
D KeyVerificationManager: Verificando mnemonic de desarrollo de Substrate
D KeyVerificationManager: Clave pública generada: [hex]
D KeyVerificationManager: Dirección generada: [SS58 address]
I WalletManager: 🎉 PRUEBA SUBSTRATE EXITOSA
```

## 🚀 Próximos Pasos

1. **Integrar en UI**: Agregar botones para ejecutar las pruebas
2. **Reportes detallados**: Generar reportes de verificación más detallados
3. **Pruebas automatizadas**: Incluir en tests unitarios
4. **Validación de red**: Verificar claves contra nodos de red reales

## 📚 Referencias

- [Substrate Development Accounts](https://docs.substrate.io/reference/glossary/)
- [BIP39 Standard](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [SR25519 Specification](https://github.com/w3f/schnorrkel)
- [Subkey Documentation](https://docs.substrate.io/reference/command-line-tools/subkey/)

---

**Conclusión**: Estos mnemónicos de prueba te permiten verificar que tu implementación de SR25519 es correcta y compatible con el ecosistema Substrate/Polkadot. Las pruebas automáticas te dan confianza en que las claves generadas son válidas y seguras.


