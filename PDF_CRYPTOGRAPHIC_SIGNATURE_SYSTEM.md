# Sistema de Firma Criptográfica de PDFs con Sr25519

## Descripción General

Este sistema implementa la firma criptográfica de documentos PDF usando el algoritmo **Sr25519** con cuentas derivadas `//did//0`, siguiendo el patrón de **DIDsign.io** pero adaptado para aplicación nativa Kotlin con primitivos de Substrate.

## Características Principales

### ✅ Implementado
- **Firma Sr25519**: Usando primitivos del SDK de Substrate
- **Derivación de claves**: Cuenta derivada `//did//0` para identidad
- **Formato JWS**: JSON Web Signature compatible con DIDsign.io
- **Hash SHA-256**: Para integridad de archivos PDF
- **Verificación de firmas**: Validación criptográfica completa
- **Firma en lote**: Múltiples PDFs simultáneamente
- **Integración UI**: Componentes Compose para firma y verificación
- **Almacenamiento seguro**: Archivos `.didsign` con metadatos

### 🔄 En Desarrollo
- **Timestamping blockchain**: Opcional con transacciones KILT
- **Verificación on-chain**: Validación de DIDs en blockchain
- **Credenciales KILT**: Inclusión de credenciales verificables

## Arquitectura del Sistema

### Componentes Principales

```
📁 crypto/pdf/
├── PDFSignatureManager.kt          # Gestor principal de firmas
└── PDFSignatureData.kt            # Estructuras de datos

📁 ui/components/
└── PDFCryptographicSignatureDialog.kt  # Componente UI

📁 examples/
└── PDFCryptographicSignatureExample.kt  # Ejemplos de uso
```

### Flujo de Firma

```mermaid
graph TD
    A[PDF Generado] --> B[Calcular Hash SHA-256]
    B --> C[Derivar Clave //did//0]
    C --> D[Firmar con Sr25519]
    D --> E[Generar JWS]
    E --> F[Crear Archivo .didsign]
    F --> G[Guardar Metadatos]
```

### Flujo de Verificación

```mermaid
graph TD
    A[PDF + Archivo .didsign] --> B[Cargar Metadatos]
    B --> C[Calcular Hash Actual]
    C --> D[Comparar Hashes]
    D --> E[Verificar Firma Sr25519]
    E --> F[Validar DID URI]
    F --> G[Resultado Verificación]
```

## Uso del Sistema

### 1. Firma Básica de PDF

```kotlin
val pdfSignatureManager = PDFSignatureManager(context)
val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

val signatureResult = pdfSignatureManager.signPDF(
    pdfFile = pdfFile,
    mnemonic = testMnemonic,
    signerName = "Usuario Test"
)

when (signatureResult) {
    is PDFSignatureManager.SignatureResult.Success -> {
        println("✅ PDF firmado: ${signatureResult.signatureFile.name}")
        println("🔑 DID URI: ${signatureResult.signature.didKeyUri}")
    }
    is PDFSignatureManager.SignatureResult.Error -> {
        println("❌ Error: ${signatureResult.message}")
    }
}
```

### 2. Verificación de Firma

```kotlin
val verificationResult = pdfSignatureManager.verifyPDFSignature(
    pdfFile = originalPDF,
    signatureFile = signatureFile
)

when (verificationResult) {
    is PDFSignatureManager.VerificationResult.Valid -> {
        println("✅ Firma válida")
        println("👤 Firmante: ${verificationResult.signerInfo.address}")
    }
    is PDFSignatureManager.VerificationResult.Invalid -> {
        println("❌ Firma inválida: ${verificationResult.reason}")
    }
    is PDFSignatureManager.VerificationResult.Error -> {
        println("❌ Error: ${verificationResult.message}")
    }
}
```

### 3. Firma en Lote

```kotlin
val results = pdfSignatureManager.signMultiplePDFs(
    pdfFiles = listOf(pdf1, pdf2, pdf3),
    mnemonic = mnemonic,
    signerName = "Usuario Test"
)

val successCount = results.count { it is PDFSignatureManager.SignatureResult.Success }
println("📊 Firmados: $successCount/${results.size}")
```

### 4. Integración con UI

```kotlin
@Composable
fun MyPDFScreen() {
    var showSignatureDialog by remember { mutableStateOf(false) }
    
    Button(
        onClick = { showSignatureDialog = true }
    ) {
        Text("Firmar PDF Criptográficamente")
    }
    
    if (showSignatureDialog) {
        PDFCryptographicSignatureDialog(
            pdfFile = pdfFile,
            onDismiss = { showSignatureDialog = false },
            onSignatureComplete = { signature, signatureFile ->
                // Manejar firma completada
            },
            onError = { error ->
                // Manejar error
            }
        )
    }
}
```

## Estructura de Datos

### PDFSignature

```kotlin
@Serializable
data class PDFSignature(
    val jws: String,                    // JWT con firma
    val hashes: List<String>,           // Hashes SHA-256
    val didKeyUri: String,              // URI del DID
    val signature: String,              // Firma Sr25519 (hex)
    val timestamp: Long,               // Timestamp de firma
    val pdfFileName: String,           // Nombre del PDF
    val signerAddress: String,          // Dirección del firmante
    val remark: RemarkInfo? = null      // Timestamping opcional
)
```

### Archivo .didsign

```json
{
  "jws": "eyJhbGciOiJTcjI1NTE5IiwidHlwIjoiSlNTIiwia2lkIjoiZGlkOmtpbHQ6NHRQcUxxdWljb1NtcTlMUDk1RFZaNFJFdjdOR1c4NU56U1dTa3RnM0FWb1kxN2ZOfmRpZDpraWx0OjR0UHFMcXVpY29TbXE5TFA5NURWWjRSRXY3TkdXODVOelNXVGt0ZzNBVm9ZMTdmTiNhdXRoZW50aWNhdGlvbiJ9.eyJoYXNoIjoiZjFhMmIzYzQifQ.0xb0a3373978f151ece371d68c7a1098b714244326652e60a9845959af88c71b3d0dc5fa0e27ffe10ac102f7e86ac2e98319b8b2c733434eb65f97c497a6092f8e",
  "hashes": ["f1a2b3c4..."],
  "didKeyUri": "did:kilt:4tPqLquicoSmq9LP95DVZ4REv7NGW85NzSWSktg3AVoY17fN#authentication",
  "signature": "0xb0a3373978f151ece371d68c7a1098b714244326652e60a9845959af88c71b3d0dc5fa0e27ffe10ac102f7e86ac2e98319b8b2c733434eb65f97c497a6092f8e",
  "timestamp": 1703123456789,
  "pdfFileName": "documento.pdf",
  "signerAddress": "4tPqLquicoSmq9LP95DVZ4REv7NGW85NzSWSktg3AVoY17fN"
}
```

## Algoritmos Criptográficos

### Sr25519 (Substrate)
- **Tipo**: Firma digital de curva elíptica
- **Curva**: Ed25519 con variantes específicas de Substrate
- **Tamaño de clave**: 32 bytes (256 bits)
- **Tamaño de firma**: 64 bytes
- **Derivación**: Usando `//did//0` para identidad

### SHA-256
- **Propósito**: Hash de integridad de archivos
- **Tamaño**: 32 bytes (256 bits)
- **Uso**: Verificación de integridad del PDF

### JWS (JSON Web Signature)
- **Formato**: `header.payload.signature`
- **Codificación**: Base64URL
- **Header**: `{"alg":"Sr25519","typ":"JWS","kid":"did:kilt:..."}`
- **Payload**: `{"hash":"...","timestamp":...}`

## Integración con SecureWalletFlowManager

### Creación de Cuenta de Identidad

```kotlin
val secureWalletManager = SecureWalletFlowManager(context)

// Crear cuenta de fondos
val fundsAccountResult = secureWalletManager.createFundsAccount(
    activity = activity,
    accountName = "Mi Cuenta",
    mnemonic = mnemonic
)

// Crear cuenta de identidad con derivación //did//0
val identityAccountResult = secureWalletManager.createIdentityAccount(
    activity = activity,
    legalName = "Mi Nombre Legal",
    fundsAccount = fundsAccountResult.account
)
```

### Uso para Firma

```kotlin
// Usar la wallet completa para firmar PDFs
val signatureResult = pdfSignatureManager.signPDF(
    pdfFile = pdfFile,
    mnemonic = wallet.fundsAccount.mnemonic,
    signerName = wallet.identityAccount.legalName
)
```

## Comparación con DIDsign.io

| Característica | DIDsign.io | Nuestra Implementación |
|----------------|------------|----------------------|
| **Plataforma** | Web (React) | Android (Kotlin) |
| **Wallet** | Sporran Extension | SecureWalletManager |
| **Algoritmo** | Sr25519 | Sr25519 ✅ |
| **Derivación** | //did//0 | //did//0 ✅ |
| **Formato** | JWS | JWS ✅ |
| **Hash** | SHA-256 | SHA-256 ✅ |
| **Timestamping** | Blockchain | Opcional |
| **Verificación** | On-chain | Local + Opcional |

## Seguridad

### Medidas Implementadas
- **Claves derivadas**: Uso de `//did//0` para identidad
- **Firma criptográfica**: Sr25519 con validación completa
- **Integridad**: Hash SHA-256 del contenido
- **Almacenamiento**: Archivos `.didsign` con metadatos
- **Verificación**: Validación de firma y hash

### Consideraciones
- **Claves privadas**: Manejo seguro a través de SecureWalletManager
- **Mnemónicos**: Protección con biometría
- **Archivos**: Almacenamiento en directorio seguro
- **Validación**: Verificación completa antes de aceptar firmas

## Ejemplos de Uso

### Ejemplo Completo

```kotlin
class PDFSignatureExample(private val context: Context) {
    
    suspend fun signAndVerifyPDF() {
        val pdfSignatureManager = PDFSignatureManager(context)
        
        // 1. Crear PDF de prueba
        val testPDF = createTestPDF()
        
        // 2. Firmar
        val signatureResult = pdfSignatureManager.signPDF(
            pdfFile = testPDF,
            mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            signerName = "Usuario Test"
        )
        
        // 3. Verificar
        if (signatureResult is PDFSignatureManager.SignatureResult.Success) {
            val verificationResult = pdfSignatureManager.verifyPDFSignature(
                pdfFile = testPDF,
                signatureFile = signatureResult.signatureFile
            )
            
            when (verificationResult) {
                is PDFSignatureManager.VerificationResult.Valid -> {
                    println("✅ PDF firmado y verificado exitosamente!")
                }
                else -> {
                    println("❌ Error en verificación")
                }
            }
        }
    }
}
```

## Próximos Pasos

### Funcionalidades Futuras
1. **Timestamping Blockchain**: Integración con KILT para timestamps
2. **Verificación On-chain**: Validación de DIDs en blockchain
3. **Credenciales KILT**: Inclusión de credenciales verificables
4. **Múltiples Algoritmos**: Soporte para Ed25519 y ECDSA
5. **Firma Avanzada**: Firmas con múltiples firmantes
6. **Validación de Certificados**: Verificación de certificados digitales

### Mejoras Técnicas
1. **Optimización**: Mejora en rendimiento de firmas en lote
2. **Caché**: Sistema de caché para verificaciones frecuentes
3. **Compresión**: Compresión de archivos `.didsign`
4. **Encriptación**: Encriptación adicional de archivos de firma
5. **Auditoría**: Logs detallados de operaciones de firma

## Conclusión

Este sistema proporciona una implementación completa de firma criptográfica de PDFs usando Sr25519 con cuentas derivadas `//did//0`, siguiendo los estándares de DIDsign.io pero adaptado para aplicaciones nativas Android. La implementación garantiza:

- **Seguridad**: Firma criptográfica robusta con Sr25519
- **Integridad**: Verificación de hash SHA-256
- **Compatibilidad**: Formato JWS compatible con estándares web
- **Usabilidad**: Interfaz intuitiva para usuarios finales
- **Escalabilidad**: Soporte para firma en lote y múltiples documentos

El sistema está listo para uso en producción y puede extenderse fácilmente con funcionalidades adicionales como timestamping blockchain y verificación on-chain.
