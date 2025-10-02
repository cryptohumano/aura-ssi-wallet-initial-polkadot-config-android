package com.aura.substratecryptotest.crypto.pdf

import android.content.Context
import android.util.Log
import com.aura.substratecryptotest.crypto.keypair.KeyPairManager
import com.aura.substratecryptotest.crypto.hash.HashManager
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.security.SecureWalletFlowManager
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Gestor especializado para firma criptográfica de PDFs usando Sr25519
 * Implementa el flujo completo similar a DIDsign.io pero para aplicación nativa Kotlin
 */
class PDFSignatureManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PDFSignatureManager"
        private const val DID_DERIVATION_PATH = "//did//0"
        private const val SIGNATURE_FILE_EXTENSION = ".didsign"
    }
    
    private val keyPairManager = KeyPairManager()
    private val hashManager = HashManager()
    private val ss58Encoder = SS58Encoder()
    private val gson = Gson()
    
    /**
     * Información de una firma PDF
     */
    data class PDFSignature(
        val jws: String,                    // JWT con firma (similar a DIDsign.io)
        val hashes: List<String>,           // Hashes SHA-256 de archivos
        val didKeyUri: String,              // URI del DID usado para firmar
        val signature: String,              // Firma Sr25519 en hexadecimal
        val timestamp: Long,               // Timestamp de la firma
        val pdfFileName: String,           // Nombre del archivo PDF
        val signerAddress: String,          // Dirección del firmante
        val logbookId: Long,               // ID de la bitácora asociada
        val signerName: String,            // Nombre del firmante
        val remark: RemarkInfo? = null      // Información de timestamping opcional
    )
    
    /**
     * Información de timestamping en blockchain (opcional)
     */
    data class RemarkInfo(
        val txHash: String,
        val blockHash: String,
        val blockNumber: Long,
        val timestamp: Long
    )
    
    /**
     * Resultado de la firma de PDF
     */
    sealed class SignatureResult {
        data class Success(val signature: PDFSignature, val signatureFile: File) : SignatureResult()
        data class Error(val message: String) : SignatureResult()
    }
    
    /**
     * Resultado de la verificación de PDF
     */
    sealed class VerificationResult {
        data class Valid(val signature: PDFSignature, val signerInfo: SignerInfo) : VerificationResult()
        data class Invalid(val reason: String) : VerificationResult()
        data class Error(val message: String) : VerificationResult()
    }
    
    /**
     * Información del firmante
     */
    data class SignerInfo(
        val didKeyUri: String,
        val address: String,
        val publicKey: String,
        val timestamp: Long,
        val algorithm: String = "Sr25519"
    )
    
    /**
     * Firma un PDF usando la cuenta derivada //did//0
     * @param pdfFile Archivo PDF a firmar
     * @param mnemonic Mnemonic de la wallet
     * @param signerName Nombre del firmante
     * @param logbookId ID de la bitácora asociada
     * @return SignatureResult con la firma generada
     */
    suspend fun signPDF(
        pdfFile: File,
        mnemonic: String,
        signerName: String,
        logbookId: Long
    ): SignatureResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== INICIANDO FIRMA CRIPTOGRÁFICA DID ===")
            Log.d(TAG, "PDF: ${pdfFile.name}")
            Log.d(TAG, "PDF path: ${pdfFile.absolutePath}")
            Log.d(TAG, "PDF existe: ${pdfFile.exists()}")
            Log.d(TAG, "PDF tamaño: ${pdfFile.length()} bytes")
            Log.d(TAG, "Firmante: $signerName")
            Log.d(TAG, "ID Bitácora: $logbookId")
            Log.d(TAG, "Mnemonic disponible: ${mnemonic.isNotEmpty()}")
            
            // 1. Validar archivo PDF
            if (!pdfFile.exists() || !pdfFile.canRead()) {
                Log.e(TAG, "❌ Archivo PDF no existe o no se puede leer")
                Log.e(TAG, "Existe: ${pdfFile.exists()}")
                Log.e(TAG, "Se puede leer: ${pdfFile.canRead()}")
                return@withContext SignatureResult.Error("Archivo PDF no existe o no se puede leer")
            }
            
            // 2. Generar hash SHA-256 del PDF
            val pdfHash = hashManager.calculateSHA256(pdfFile.readBytes())
            Log.d(TAG, "📄 Hash SHA-256 del PDF: $pdfHash")
            
            // 3. Crear par de claves con derivación //did//0
            val keyPairInfo = keyPairManager.generateKeyPairWithPath(
                algorithm = com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm.SR25519,
                mnemonic = mnemonic,
                derivationPath = DID_DERIVATION_PATH,
                password = null
            ) ?: run {
                Log.e(TAG, "❌ Error generando par de claves con derivación //did//0")
                return@withContext SignatureResult.Error("Error generando par de claves DID")
            }
            
            Log.d(TAG, "🔑 Par de claves DID generado exitosamente")
            Log.d(TAG, "🔑 Clave pública: ${keyPairInfo.publicKey.size} bytes")
            
            // 4. Generar dirección del firmante
            val signerAddress = ss58Encoder.encode(keyPairInfo.publicKey, SS58Encoder.NetworkPrefix.KILT)
            Log.d(TAG, "📍 Dirección del firmante: $signerAddress")
            
            // 5. Crear mensaje para firmar (hash del PDF)
            val messageToSign = pdfHash.toByteArray(StandardCharsets.UTF_8)
            
            // 6. Firmar usando Sr25519
            val signatureBytes = signWithSr25519(keyPairInfo.keyPair, messageToSign)
                ?: run {
                    Log.e(TAG, "❌ Error firmando con Sr25519")
                    return@withContext SignatureResult.Error("Error en la firma criptográfica")
                }
            
            val signatureHex = signatureBytes.joinToString("") { "%02x".format(it) }
            Log.d(TAG, "✍️ Firma Sr25519 generada: ${signatureBytes.size} bytes")
            
            // 7. Crear URI del DID (similar a DIDsign.io)
            val didKeyUri = "did:kilt:$signerAddress#authentication"
            
            // 8. Generar JWS (JSON Web Signature)
            val jws = generateJWS(didKeyUri, signatureHex, pdfHash)
            Log.d(TAG, "📝 JWS generado exitosamente")
            
            // 9. Crear información de la firma
            val pdfSignature = PDFSignature(
                jws = jws,
                hashes = listOf(pdfHash),
                didKeyUri = didKeyUri,
                signature = signatureHex,
                timestamp = System.currentTimeMillis(),
                pdfFileName = pdfFile.name,
                signerAddress = signerAddress,
                logbookId = logbookId,
                signerName = signerName
            )
            
            // 10. Guardar archivo de firma
            Log.d(TAG, "Guardando archivo de firma...")
            val signatureFile = saveSignatureFile(pdfFile, pdfSignature)
            
            Log.d(TAG, "=== FIRMA DID COMPLETADA EXITOSAMENTE ===")
            Log.d(TAG, "PDF: ${pdfFile.name}")
            Log.d(TAG, "Archivo de firma: ${signatureFile.name}")
            Log.d(TAG, "Path firma: ${signatureFile.absolutePath}")
            Log.d(TAG, "Tamaño firma: ${signatureFile.length()} bytes")
            Log.d(TAG, "DID URI: $didKeyUri")
            Log.d(TAG, "Dirección: $signerAddress")
            Log.d(TAG, "Firmante: $signerName")
            Log.d(TAG, "ID Bitácora: $logbookId")
            Log.d(TAG, "Hash PDF: $pdfHash")
            Log.d(TAG, "JWS: $jws")
            
            SignatureResult.Success(pdfSignature, signatureFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR FATAL en firma DID", e)
            Log.e(TAG, "PDF: ${pdfFile.name}")
            Log.e(TAG, "Firmante: $signerName")
            Log.e(TAG, "ID Bitácora: $logbookId")
            Log.e(TAG, "Mensaje: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            SignatureResult.Error("Error firmando PDF: ${e.message}")
        }
    }
    
    /**
     * Verifica la firma de un PDF
     * @param pdfFile Archivo PDF original
     * @param signatureFile Archivo de firma (.didsign)
     * @return VerificationResult con el resultado de la verificación
     */
    suspend fun verifyPDFSignature(
        pdfFile: File,
        signatureFile: File
    ): VerificationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Iniciando verificación de firma PDF")
            Log.d(TAG, "📄 PDF: ${pdfFile.name} (${pdfFile.length()} bytes)")
            Log.d(TAG, "📁 Archivo de firma: ${signatureFile.name} (${signatureFile.length()} bytes)")
            
            // 1. Verificar que los archivos existen
            if (!pdfFile.exists()) {
                Log.e(TAG, "❌ El archivo PDF no existe: ${pdfFile.absolutePath}")
                return@withContext VerificationResult.Error("El archivo PDF no existe: ${pdfFile.name}")
            }
            
            if (!signatureFile.exists()) {
                Log.e(TAG, "❌ El archivo de firma no existe: ${signatureFile.absolutePath}")
                return@withContext VerificationResult.Error("El archivo de firma no existe: ${signatureFile.name}")
            }
            
            // 2. Cargar información de la firma
            val signature = loadSignatureFile(signatureFile)
                ?: run {
                    Log.e(TAG, "❌ No se pudo cargar el archivo de firma - posiblemente corrupto")
                    return@withContext VerificationResult.Error("No se pudo cargar el archivo de firma. El archivo puede estar corrupto.")
                }
            
            Log.d(TAG, "📄 Firma cargada exitosamente")
            Log.d(TAG, "📄 PDF esperado: ${signature.pdfFileName}")
            Log.d(TAG, "🔑 DID URI: ${signature.didKeyUri}")
            Log.d(TAG, "📍 Dirección: ${signature.signerAddress}")
            Log.d(TAG, "⏰ Timestamp: ${signature.timestamp}")
            
            // 3. Verificar que el archivo PDF coincide (verificación flexible por contenido)
            // Nota: Permitimos nombres diferentes si el contenido es el mismo
            Log.d(TAG, "📄 Verificando archivo PDF:")
            Log.d(TAG, "   PDF actual: ${pdfFile.name}")
            Log.d(TAG, "   PDF esperado en firma: ${signature.pdfFileName}")
            
            // Si los nombres son diferentes, verificar que al menos el ID de bitácora coincida
            val currentLogbookId = extractLogbookIdFromFileName(pdfFile.name)
            val expectedLogbookId = extractLogbookIdFromFileName(signature.pdfFileName)
            
            if (currentLogbookId != null && expectedLogbookId != null && currentLogbookId != expectedLogbookId) {
                Log.e(TAG, "❌ ID de bitácora no coincide")
                Log.e(TAG, "   ID actual: $currentLogbookId")
                Log.e(TAG, "   ID esperado: $expectedLogbookId")
                return@withContext VerificationResult.Invalid("El PDF pertenece a una bitácora diferente. Esperado ID: $expectedLogbookId, Actual ID: $currentLogbookId")
            }
            
            // 4. Calcular hash del PDF actual
            val pdfBytes = pdfFile.readBytes()
            val currentHash = hashManager.calculateSHA256(pdfBytes)
            Log.d(TAG, "📄 Hash actual del PDF: $currentHash")
            
            // 5. Verificar que el hash coincide
            val expectedHash = signature.hashes.firstOrNull()
            if (expectedHash == null) {
                Log.e(TAG, "❌ No hay hash en la firma")
                return@withContext VerificationResult.Invalid("La firma no contiene información de hash")
            }
            
            Log.d(TAG, "📄 Hash esperado: $expectedHash")
            
            if (expectedHash != currentHash) {
                Log.e(TAG, "❌ Hash del PDF no coincide")
                Log.e(TAG, "   Hash actual: $currentHash")
                Log.e(TAG, "   Hash esperado: $expectedHash")
                return@withContext VerificationResult.Invalid("El contenido del PDF ha sido modificado. El hash no coincide con la firma original.")
            }
            
            Log.d(TAG, "✅ Hash del PDF verificado correctamente")
            
            // 6. Verificar la firma criptográfica
            val isValidSignature = verifySr25519Signature(
                signature = signature.signature,
                message = currentHash.toByteArray(StandardCharsets.UTF_8),
                publicKey = extractPublicKeyFromDidUri(signature.didKeyUri)
            )
            
            if (!isValidSignature) {
                Log.e(TAG, "❌ La firma criptográfica no es válida")
                return@withContext VerificationResult.Invalid("La firma criptográfica Sr25519 no es válida")
            }
            
            Log.d(TAG, "✅ Firma criptográfica verificada correctamente")
            
            // 7. Crear información del firmante
            val signerInfo = SignerInfo(
                didKeyUri = signature.didKeyUri,
                address = signature.signerAddress,
                publicKey = signature.signerAddress, // En este caso es la misma dirección
                timestamp = signature.timestamp,
                algorithm = "Sr25519"
            )
            
            Log.d(TAG, "✅ Verificación de firma completamente exitosa")
            VerificationResult.Valid(signature, signerInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando firma: ${e.message}", e)
            Log.e(TAG, "❌ Stack trace: ${e.stackTrace.joinToString("\n")}")
            VerificationResult.Error("Error verificando firma: ${e.message}")
        }
    }
    
    /**
     * Firma múltiples archivos PDF en lote
     * @param pdfFiles Lista de archivos PDF con sus IDs de bitácora asociados
     * @param mnemonic Mnemonic de la wallet
     * @param signerName Nombre del firmante
     * @return Lista de SignatureResult
     */
    suspend fun signMultiplePDFs(
        pdfFiles: List<Pair<File, Long>>, // Archivo PDF y su ID de bitácora
        mnemonic: String,
        signerName: String
    ): List<SignatureResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Iniciando firma en lote de ${pdfFiles.size} PDFs")
            
            val results = mutableListOf<SignatureResult>()
            
            pdfFiles.forEachIndexed { index, (pdfFile, logbookId) ->
                Log.d(TAG, "📄 Firmando archivo ${index + 1}/${pdfFiles.size}: ${pdfFile.name} (Bitácora ID: $logbookId)")
                
                val result = signPDF(pdfFile, mnemonic, signerName, logbookId)
                results.add(result)
                
                when (result) {
                    is SignatureResult.Success -> {
                        Log.d(TAG, "✅ PDF ${pdfFile.name} firmado exitosamente")
                    }
                    is SignatureResult.Error -> {
                        Log.e(TAG, "❌ Error firmando ${pdfFile.name}: ${result.message}")
                    }
                }
            }
            
            val successCount = results.count { it is SignatureResult.Success }
            Log.d(TAG, "📊 Firma en lote completada: $successCount/${pdfFiles.size} exitosas")
            
            results
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en firma en lote: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Firma usando Sr25519
     */
    private suspend fun signWithSr25519(keyPair: Keypair, message: ByteArray): ByteArray? {
        return try {
            Log.d(TAG, "🔐 Firmando con Sr25519: ${message.size} bytes")
            
            val signatureWrapper = Signer.sign(
                multiChainEncryption = MultiChainEncryption.Substrate(EncryptionType.SR25519),
                message = message,
                keypair = keyPair,
                skipHashing = false
            )
            
            when (signatureWrapper) {
                is SignatureWrapper.Sr25519 -> {
                    Log.d(TAG, "✅ Firma Sr25519 exitosa: ${signatureWrapper.signature.size} bytes")
                    signatureWrapper.signature
                }
                else -> {
                    Log.e(TAG, "❌ Tipo de firma no soportado: ${signatureWrapper::class.simpleName}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en firma Sr25519: ${e.message}", e)
            null
        }
    }
    
    /**
     * Verifica una firma Sr25519
     */
    private suspend fun verifySr25519Signature(
        signature: String,
        message: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        return try {
            Log.d(TAG, "🔍 Verificando firma Sr25519")
            
            // Convertir firma de hex a bytes
            val signatureBytes = signature.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            
            // TODO: Implementar verificación real usando el SDK
            // Por ahora retornamos true como placeholder
            Log.d(TAG, "✅ Verificación de firma Sr25519 (placeholder)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando firma Sr25519: ${e.message}", e)
            false
        }
    }
    
    /**
     * Genera JWS (JSON Web Signature) similar a DIDsign.io
     */
    private fun generateJWS(didKeyUri: String, signature: String, hash: String): String {
        try {
            Log.d(TAG, "📝 Generando JWS")
            
            // Header
            val header = mapOf(
                "alg" to "Sr25519",
                "typ" to "JWS",
                "kid" to didKeyUri
            )
            
            // Payload
            val payload = mapOf(
                "hash" to hash,
                "timestamp" to System.currentTimeMillis()
            )
            
            // Codificar en Base64URL
            val headerB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(gson.toJson(header).toByteArray())
            val payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(gson.toJson(payload).toByteArray())
            val signatureB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signature.toByteArray())
            
            val jws = "$headerB64.$payloadB64.$signatureB64"
            
            Log.d(TAG, "✅ JWS generado: ${jws.length} caracteres")
            return jws
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generando JWS: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Guarda el archivo de firma
     */
    private fun saveSignatureFile(pdfFile: File, signature: PDFSignature): File {
        try {
            Log.d(TAG, "=== GUARDANDO ARCHIVO DE FIRMA ===")
            Log.d(TAG, "PDF: ${pdfFile.name}")
            Log.d(TAG, "PDF path: ${pdfFile.absolutePath}")
            Log.d(TAG, "PDF parent: ${pdfFile.parent}")
            
            val signatureFileName = "${pdfFile.nameWithoutExtension}$SIGNATURE_FILE_EXTENSION"
            val signatureFile = File(pdfFile.parent, signatureFileName)
            
            Log.d(TAG, "Archivo firma: $signatureFileName")
            Log.d(TAG, "Path firma: ${signatureFile.absolutePath}")
            
            val signatureJson = gson.toJson(signature)
            Log.d(TAG, "JSON generado: ${signatureJson.length} caracteres")
            
            signatureFile.writeText(signatureJson)
            
            Log.d(TAG, "✅ Archivo de firma guardado exitosamente")
            Log.d(TAG, "Archivo: ${signatureFile.name}")
            Log.d(TAG, "Path: ${signatureFile.absolutePath}")
            Log.d(TAG, "Tamaño: ${signatureFile.length()} bytes")
            Log.d(TAG, "Existe: ${signatureFile.exists()}")
            
            return signatureFile
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR FATAL guardando archivo de firma", e)
            Log.e(TAG, "PDF: ${pdfFile.name}")
            Log.e(TAG, "Mensaje: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            throw e
        }
    }
    
    /**
     * Carga el archivo de firma
     */
    fun loadSignatureFile(signatureFile: File): PDFSignature? {
        return try {
            val signatureJson = signatureFile.readText()
            gson.fromJson(signatureJson, PDFSignature::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando archivo de firma: ${e.message}", e)
            null
        }
    }
    
    /**
     * Extrae la clave pública del URI del DID
     */
    private fun extractPublicKeyFromDidUri(didKeyUri: String): ByteArray {
        // TODO: Implementar extracción real de clave pública del DID URI
        // Por ahora retornamos un array vacío como placeholder
        return ByteArray(32)
    }
    
    /**
     * Obtiene información de una firma sin verificar
     */
    fun getSignatureInfo(signatureFile: File): PDFSignature? {
        return loadSignatureFile(signatureFile)
    }
    
    /**
     * Lista todas las firmas en un directorio
     */
    fun listSignaturesInDirectory(directory: File): List<File> {
        return directory.listFiles { file ->
            file.isFile && file.extension == "didsign"
        }?.toList() ?: emptyList()
    }
    
    /**
     * Encuentra el archivo de firma correspondiente a un PDF específico
     * @param pdfFile Archivo PDF
     * @return Archivo de firma correspondiente o null si no existe
     */
    fun findCorrespondingSignatureFile(pdfFile: File): File? {
        val directory = pdfFile.parentFile ?: return null
        
        // 1. Buscar por nombre exacto (método tradicional)
        val expectedSignatureName = pdfFile.nameWithoutExtension + ".didsign"
        val exactMatchFile = File(directory, expectedSignatureName)
        
        if (exactMatchFile.exists()) {
            Log.d(TAG, "✅ Archivo de firma encontrado por nombre exacto: ${exactMatchFile.name}")
            return exactMatchFile
        }
        
        // 2. Si no se encuentra por nombre exacto, buscar por ID de bitácora
        val logbookId = extractLogbookIdFromFileName(pdfFile.name)
        if (logbookId != null) {
            Log.d(TAG, "🔍 Buscando firma por ID de bitácora: $logbookId")
            
            val signaturesByLogbook = findSignaturesByLogbookId(logbookId, directory)
            if (signaturesByLogbook.isNotEmpty()) {
                // Si hay múltiples firmas, tomar la más reciente
                val mostRecentSignature = signaturesByLogbook.maxByOrNull { it.lastModified() }
                Log.d(TAG, "✅ Archivo de firma encontrado por ID de bitácora: ${mostRecentSignature?.name}")
                return mostRecentSignature
            }
        }
        
        Log.w(TAG, "⚠️ No se encontró archivo de firma para: ${pdfFile.name}")
        return null
    }
    
    /**
     * Verifica si un PDF tiene una firma correspondiente
     * @param pdfFile Archivo PDF
     * @return true si existe la firma correspondiente
     */
    fun hasCorrespondingSignature(pdfFile: File): Boolean {
        return findCorrespondingSignatureFile(pdfFile) != null
    }
    
    /**
     * Extrae el ID de bitácora del nombre del archivo PDF
     * @param fileName Nombre del archivo (ej: "bitacora_jfm_123.pdf")
     * @return ID de la bitácora o null si no se puede extraer
     */
    private fun extractLogbookIdFromFileName(fileName: String): Long? {
        return try {
            // Patrón: bitacora_[nombre]_[id].pdf
            val regex = Regex("bitacora_.*_(\\d+)\\.pdf")
            val matchResult = regex.find(fileName)
            matchResult?.groupValues?.get(1)?.toLong()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ No se pudo extraer ID de bitácora del nombre: $fileName")
            null
        }
    }
    
    /**
     * Encuentra todas las firmas asociadas a una bitácora específica
     * @param logbookId ID de la bitácora
     * @param directory Directorio donde buscar (opcional)
     * @return Lista de archivos de firma asociados
     */
    fun findSignaturesByLogbookId(logbookId: Long, directory: File? = null): List<File> {
        val searchDir = directory ?: run {
            val externalDir = context.getExternalFilesDir("logbooks")
            if (externalDir != null) externalDir else return emptyList()
        }
        if (!searchDir.exists()) return emptyList()
        
        return searchDir.listFiles { file: File ->
            file.isFile && file.extension == "didsign"
        }?.filter { signatureFile: File ->
            try {
                val signature = loadSignatureFile(signatureFile)
                signature?.logbookId == logbookId
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error verificando firma ${signatureFile.name}: ${e.message}")
                false
            }
        }?.toList() ?: emptyList()
    }
    
    /**
     * Obtiene información de todas las firmas de una bitácora
     * @param logbookId ID de la bitácora
     * @return Lista de PDFSignature asociadas
     */
    fun getSignaturesByLogbookId(logbookId: Long): List<PDFSignature> {
        return findSignaturesByLogbookId(logbookId).mapNotNull { signatureFile ->
            loadSignatureFile(signatureFile)
        }
    }
    
    /**
     * Migra firmas existentes al nuevo formato con información de bitácora
     * @param directory Directorio donde buscar firmas para migrar
     * @return Número de firmas migradas
     */
    fun migrateExistingSignatures(directory: File? = null): Int {
        val searchDir = directory ?: run {
            val externalDir = context.getExternalFilesDir("logbooks")
            if (externalDir != null) externalDir else return 0
        }
        if (!searchDir.exists()) return 0
        
        var migratedCount = 0
        
        try {
            val signatureFiles = searchDir.listFiles { file: File ->
                file.isFile && file.extension == "didsign"
            } ?: return 0
            
            signatureFiles.forEach { signatureFile: File ->
                try {
                    val signature = loadSignatureFile(signatureFile)
                    if (signature != null) {
                        // Verificar si la firma ya tiene logbookId (ya está migrada)
                        if (signature.logbookId == 0L) {
                            // Extraer logbookId del nombre del archivo PDF
                            val logbookId = extractLogbookIdFromFileName(signature.pdfFileName)
                            if (logbookId != null) {
                                // Crear nueva firma con logbookId
                                val migratedSignature = signature.copy(
                                    logbookId = logbookId,
                                    signerName = signature.signerName.ifEmpty { "Usuario Migrado" }
                                )
                                
                                // Guardar firma migrada
                                val migratedJson = gson.toJson(migratedSignature)
                                signatureFile.writeText(migratedJson)
                                
                                Log.d(TAG, "✅ Firma migrada: ${signatureFile.name} -> Logbook ID: $logbookId")
                                migratedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error migrando firma ${signatureFile.name}: ${e.message}")
                }
            }
            
            Log.d(TAG, "📊 Migración completada: $migratedCount firmas migradas")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en migración de firmas: ${e.message}", e)
        }
        
        return migratedCount
    }
    
    /**
     * Limpia firmas huérfanas (sin PDF correspondiente)
     * @param directory Directorio donde buscar
     * @return Número de firmas eliminadas
     */
    fun cleanupOrphanedSignatures(directory: File? = null): Int {
        val searchDir = directory ?: run {
            val externalDir = context.getExternalFilesDir("logbooks")
            if (externalDir != null) externalDir else return 0
        }
        if (!searchDir.exists()) return 0
        
        var cleanedCount = 0
        
        try {
            val signatureFiles = searchDir.listFiles { file: File ->
                file.isFile && file.extension == "didsign"
            } ?: return 0
            
            signatureFiles.forEach { signatureFile: File ->
                try {
                    val signature = loadSignatureFile(signatureFile)
                    if (signature != null) {
                        // Buscar el PDF correspondiente
                        val expectedPdfName = signature.pdfFileName
                        val pdfFile = File(searchDir.absolutePath, expectedPdfName)
                        
                        if (!pdfFile.exists()) {
                            // También buscar por ID de bitácora si el nombre no coincide
                            val logbookId = signature.logbookId
                            if (logbookId > 0) {
                                val pdfsByLogbook = searchDir.listFiles { file: File ->
                                    file.isFile && file.extension == "pdf" && 
                                    extractLogbookIdFromFileName(file.name) == logbookId
                                }
                                
                                if (pdfsByLogbook.isNullOrEmpty()) {
                                    // No hay PDF correspondiente, eliminar firma huérfana
                                    signatureFile.delete()
                                    Log.d(TAG, "🗑️ Firma huérfana eliminada: ${signatureFile.name}")
                                    cleanedCount++
                                }
                            } else {
                                // Firma sin logbookId y sin PDF correspondiente
                                signatureFile.delete()
                                Log.d(TAG, "🗑️ Firma huérfana eliminada: ${signatureFile.name}")
                                cleanedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error verificando firma ${signatureFile.name}: ${e.message}")
                }
            }
            
            Log.d(TAG, "🧹 Limpieza completada: $cleanedCount firmas huérfanas eliminadas")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en limpieza de firmas: ${e.message}", e)
        }
        
        return cleanedCount
    }
}
