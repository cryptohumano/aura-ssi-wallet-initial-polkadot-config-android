package com.aura.substratecryptotest.examples

import android.content.Context
import android.util.Log
import com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager
import com.aura.substratecryptotest.security.SecureWalletFlowManager
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Ejemplo de uso del sistema de firma criptográfica de PDFs
 * Demuestra cómo usar Sr25519 con cuenta derivada //did//0
 */
class PDFCryptographicSignatureExample(private val context: Context) {
    
    companion object {
        private const val TAG = "PDFSignatureExample"
    }
    
    private val pdfSignatureManager = PDFSignatureManager(context)
    
    /**
     * Ejemplo completo de firma y verificación de PDF
     */
    fun demonstratePDFSignatureFlow() {
        runBlocking {
            try {
                Log.d(TAG, "🔐 === DEMOSTRACIÓN DE FIRMA CRIPTOGRÁFICA DE PDF ===")
                
                // 1. Crear un PDF de prueba
                val testPDF = createTestPDF()
                Log.d(TAG, "📄 PDF de prueba creado: ${testPDF.name}")
                
                // 2. Mnemonic de prueba (en producción vendría del SecureWalletManager)
                val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
                
                // 3. Firmar el PDF con Sr25519 usando cuenta derivada //did//0
                Log.d(TAG, "✍️ Firmando PDF con Sr25519...")
                val signatureResult = pdfSignatureManager.signPDF(
                    pdfFile = testPDF,
                    mnemonic = testMnemonic,
                    signerName = "Usuario Test",
                    logbookId = 1L
                )
                
                when (signatureResult) {
                    is PDFSignatureManager.SignatureResult.Success -> {
                        Log.d(TAG, "✅ PDF firmado exitosamente!")
                        Log.d(TAG, "🔑 DID URI: ${signatureResult.signature.didKeyUri}")
                        Log.d(TAG, "📍 Dirección: ${signatureResult.signature.signerAddress}")
                        Log.d(TAG, "📁 Archivo de firma: ${signatureResult.signatureFile.name}")
                        Log.d(TAG, "📝 JWS: ${signatureResult.signature.jws.take(50)}...")
                        
                        // 4. Verificar la firma
                        Log.d(TAG, "🔍 Verificando firma...")
                        val verificationResult = pdfSignatureManager.verifyPDFSignature(
                            pdfFile = testPDF,
                            signatureFile = signatureResult.signatureFile
                        )
                        
                        when (verificationResult) {
                            is PDFSignatureManager.VerificationResult.Valid -> {
                                Log.d(TAG, "✅ Firma verificada exitosamente!")
                                Log.d(TAG, "👤 Firmante: ${verificationResult.signerInfo.address}")
                                Log.d(TAG, "🔐 Algoritmo: ${verificationResult.signerInfo.algorithm}")
                            }
                            is PDFSignatureManager.VerificationResult.Invalid -> {
                                Log.e(TAG, "❌ Firma inválida: ${verificationResult.reason}")
                            }
                            is PDFSignatureManager.VerificationResult.Error -> {
                                Log.e(TAG, "❌ Error en verificación: ${verificationResult.message}")
                            }
                        }
                    }
                    
                    is PDFSignatureManager.SignatureResult.Error -> {
                        Log.e(TAG, "❌ Error firmando PDF: ${signatureResult.message}")
                    }
                }
                
                // 5. Demostrar firma en lote
                demonstrateBatchSigning(testMnemonic)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en demostración: ${e.message}", e)
            }
        }
    }
    
    /**
     * Demuestra la firma en lote de múltiples PDFs
     */
    private suspend fun demonstrateBatchSigning(mnemonic: String) {
        try {
            Log.d(TAG, "📦 === DEMOSTRACIÓN DE FIRMA EN LOTE ===")
            
            // Crear múltiples PDFs de prueba
            val testPDFs = listOf(
                createTestPDF("documento1.pdf"),
                createTestPDF("documento2.pdf"),
                createTestPDF("documento3.pdf")
            )
            
            Log.d(TAG, "📄 Creando ${testPDFs.size} PDFs de prueba...")
            
            // Firmar todos los PDFs
            val results = pdfSignatureManager.signMultiplePDFs(
                pdfFiles = testPDFs.map { it to 1L }, // Convertir a pares (File, Long)
                mnemonic = mnemonic,
                signerName = "Usuario Test"
            )
            
            val successCount = results.count { it is PDFSignatureManager.SignatureResult.Success }
            val errorCount = results.count { it is PDFSignatureManager.SignatureResult.Error }
            
            Log.d(TAG, "📊 Resultados de firma en lote:")
            Log.d(TAG, "✅ Exitosas: $successCount")
            Log.d(TAG, "❌ Errores: $errorCount")
            
            // Mostrar detalles de cada resultado
            results.forEachIndexed { index, result ->
                when (result) {
                    is PDFSignatureManager.SignatureResult.Success -> {
                        Log.d(TAG, "📄 PDF ${index + 1}: ✅ Firmado - ${result.signatureFile.name}")
                    }
                    is PDFSignatureManager.SignatureResult.Error -> {
                        Log.e(TAG, "📄 PDF ${index + 1}: ❌ Error - ${result.message}")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en firma en lote: ${e.message}", e)
        }
    }
    
    /**
     * Crea un PDF de prueba para demostración
     */
    private fun createTestPDF(fileName: String = "test_document.pdf"): File {
        val testContent = """
            DOCUMENTO DE PRUEBA PARA FIRMA CRIPTOGRÁFICA
            
            Este es un documento de prueba generado para demostrar
            la funcionalidad de firma criptográfica usando Sr25519
            con cuenta derivada //did//0.
            
            Características:
            - Algoritmo: Sr25519 (Substrate)
            - Derivación: //did//0
            - Formato: JWS (JSON Web Signature)
            - Hash: SHA-256
            
            Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            
            Este documento puede ser firmado criptográficamente
            para garantizar su autenticidad e integridad.
        """.trimIndent()
        
        val file = File(context.cacheDir, fileName)
        file.writeText(testContent)
        
        return file
    }
    
    /**
     * Demuestra la integración con SecureWalletFlowManager
     */
    fun demonstrateSecureWalletIntegration() {
        runBlocking {
            try {
                Log.d(TAG, "🔐 === DEMOSTRACIÓN DE INTEGRACIÓN CON SECURE WALLET ===")
                
                // Crear wallet segura usando SecureWalletFlowManager
                val secureWalletManager = SecureWalletFlowManager(context)
                
                // En un caso real, esto vendría de la UI del usuario
                val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
                
                Log.d(TAG, "🔑 Creando cuenta de identidad con derivación //did//0...")
                
                // Crear cuenta de fondos primero
                val fundsAccountResult = secureWalletManager.createFundsAccount(
                    activity = context as androidx.fragment.app.FragmentActivity,
                    accountName = "Cuenta Test",
                    mnemonic = testMnemonic
                )
                
                when (fundsAccountResult) {
                    is SecureWalletFlowManager.FundsAccountResult.Success -> {
                        Log.d(TAG, "✅ Cuenta de fondos creada: ${fundsAccountResult.account.name}")
                        
                        // Crear cuenta de identidad con derivación //did//0
                        val identityAccountResult = secureWalletManager.createIdentityAccount(
                            activity = context as androidx.fragment.app.FragmentActivity,
                            legalName = "Usuario Test",
                            fundsAccount = fundsAccountResult.account
                        )
                        
                        when (identityAccountResult) {
                            is SecureWalletFlowManager.IdentityAccountResult.Success -> {
                                Log.d(TAG, "✅ Cuenta de identidad creada: ${identityAccountResult.wallet.identityAccount.legalName}")
                                Log.d(TAG, "🔑 Clave pública: ${identityAccountResult.wallet.identityAccount.publicKey.size} bytes")
                                Log.d(TAG, "📍 Direcciones generadas: ${identityAccountResult.wallet.identityAccount.addresses.keys.joinToString(", ")}")
                                
                                // Ahora podemos usar esta cuenta para firmar PDFs
                                demonstratePDFSignatureWithWallet(identityAccountResult.wallet)
                            }
                            is SecureWalletFlowManager.IdentityAccountResult.Error -> {
                                Log.e(TAG, "❌ Error creando cuenta de identidad: ${identityAccountResult.message}")
                            }
                        }
                    }
                    is SecureWalletFlowManager.FundsAccountResult.Error -> {
                        Log.e(TAG, "❌ Error creando cuenta de fondos: ${fundsAccountResult.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en integración con Secure Wallet: ${e.message}", e)
            }
        }
    }
    
    /**
     * Demuestra el uso de la wallet completa para firmar PDFs
     */
    private suspend fun demonstratePDFSignatureWithWallet(wallet: SecureWalletFlowManager.CompleteWallet) {
        try {
            Log.d(TAG, "📄 === FIRMANDO PDF CON WALLET COMPLETA ===")
            
            val testPDF = createTestPDF("wallet_signed_document.pdf")
            
            // Usar el mnemonic de la wallet para firmar
            val signatureResult = pdfSignatureManager.signPDF(
                pdfFile = testPDF,
                mnemonic = wallet.fundsAccount.mnemonic,
                signerName = wallet.identityAccount.legalName,
                logbookId = 1L
            )
            
            when (signatureResult) {
                is PDFSignatureManager.SignatureResult.Success -> {
                    Log.d(TAG, "✅ PDF firmado con wallet completa!")
                    Log.d(TAG, "👤 Firmante: ${wallet.identityAccount.legalName}")
                    Log.d(TAG, "🔑 DID URI: ${signatureResult.signature.didKeyUri}")
                    Log.d(TAG, "📍 Dirección KILT: ${wallet.identityAccount.addresses["kilt"]}")
                }
                is PDFSignatureManager.SignatureResult.Error -> {
                    Log.e(TAG, "❌ Error firmando con wallet: ${signatureResult.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en demostración con wallet: ${e.message}", e)
        }
    }
    
    /**
     * Ejecuta todos los ejemplos
     */
    fun runAllExamples() {
        Log.d(TAG, "🚀 === INICIANDO TODOS LOS EJEMPLOS DE FIRMA CRIPTOGRÁFICA ===")
        
        // Ejemplo básico de firma y verificación
        demonstratePDFSignatureFlow()
        
        // Ejemplo de integración con Secure Wallet
        demonstrateSecureWalletIntegration()
        
        Log.d(TAG, "🏁 === TODOS LOS EJEMPLOS COMPLETADOS ===")
    }
}
