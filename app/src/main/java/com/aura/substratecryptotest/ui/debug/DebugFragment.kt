package com.aura.substratecryptotest.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.data.user.DetailedMigrationStats
import kotlinx.coroutines.launch

/**
 * Fragmento de debug completo para desarrollo
 */
class DebugFragment : Fragment() {
    
    private lateinit var appDatabaseManager: AppDatabaseManager
    private lateinit var debugText: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnResetMigration: Button
    private lateinit var btnAssociateLogbooks: Button
    private lateinit var btnClearAllData: Button
    private lateinit var btnExportLogs: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_debug, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        appDatabaseManager = AppDatabaseManager(requireContext())
        debugText = view.findViewById(R.id.debug_text)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnResetMigration = view.findViewById(R.id.btn_reset_migration)
        btnAssociateLogbooks = view.findViewById(R.id.btn_associate_logbooks)
        btnClearAllData = view.findViewById(R.id.btn_clear_all_data)
        btnExportLogs = view.findViewById(R.id.btn_export_logs)
        
        setupButtons()
        loadDebugInfo()
    }
    
    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            loadDebugInfo()
        }
        
        btnResetMigration.setOnClickListener {
            lifecycleScope.launch {
                try {
                    appDatabaseManager.migrationService.resetMigration()
                    showToast("Migración reiniciada")
                    loadDebugInfo()
                } catch (e: Exception) {
                    showToast("Error: ${e.message}")
                }
            }
        }
        
        btnAssociateLogbooks.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val success = appDatabaseManager.migrationService.associateUnassociatedLogbooks()
                    if (success) {
                        showToast("Bitácoras asociadas exitosamente")
                    } else {
                        showToast("No se pudieron asociar las bitácoras")
                    }
                    loadDebugInfo()
                } catch (e: Exception) {
                    showToast("Error: ${e.message}")
                }
            }
        }
        
        btnClearAllData.setOnClickListener {
            showClearDataConfirmation()
        }
        
        btnExportLogs.setOnClickListener {
            exportDebugLogs()
        }
    }
    
    private fun loadDebugInfo() {
        lifecycleScope.launch {
            try {
                val stats = appDatabaseManager.migrationService.getDetailedMigrationStats()
                val currentUser = appDatabaseManager.userManagementService.getCurrentUser()
                
                val debugInfo = buildString {
                    appendLine("=== DEBUG COMPLETO DEL SISTEMA ===")
                    appendLine("Timestamp: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine()
                    
                    appendLine("👤 USUARIOS:")
                    appendLine("  Total usuarios: ${stats.totalUsers}")
                    appendLine("  Con biometría: ${stats.usersWithBiometric}")
                    appendLine("  Con wallet: ${stats.usersWithWallet}")
                    appendLine("  Con DID: ${stats.usersWithDID}")
                    appendLine()
                    
                    if (currentUser != null) {
                        appendLine("👤 USUARIO ACTUAL:")
                        appendLine("  Nombre: ${currentUser.name}")
                        appendLine("  ID: ${currentUser.id}")
                        appendLine("  Email: ${currentUser.email ?: "No disponible"}")
                        appendLine("  Wallet: ${currentUser.walletAddress ?: "No disponible"}")
                        appendLine("  DID: ${currentUser.did ?: "No disponible"}")
                        appendLine("  Biometría: ${if (currentUser.biometricEnabled) "Habilitada" else "Deshabilitada"}")
                        appendLine("  Activo: ${if (currentUser.isActive) "Sí" else "No"}")
                        appendLine("  Creado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(currentUser.createdAt)}")
                        appendLine("  Último login: ${currentUser.lastLogin?.let { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(it) } ?: "Nunca"}")
                        appendLine()
                    } else {
                        appendLine("⚠️ NO HAY USUARIO ACTUAL")
                        appendLine("  Esto explica por qué no aparecen documentos")
                        appendLine()
                    }
                    
                    appendLine("📚 BITÁCORAS:")
                    appendLine("  Total bitácoras: ${stats.totalLogbooks}")
                    appendLine("  Asociadas: ${stats.associatedLogbooks}")
                    appendLine("  No asociadas: ${stats.unassociatedLogbooks}")
                    appendLine()
                    
                    appendLine("🔄 MIGRACIÓN:")
                    appendLine("  Completada: ${if (stats.migrationCompleted) "Sí" else "No"}")
                    appendLine()
                    
                    if (stats.unassociatedLogbooks > 0) {
                        appendLine("⚠️ PROBLEMA DETECTADO:")
                        appendLine("  Hay ${stats.unassociatedLogbooks} bitácoras no asociadas")
                        appendLine("  Estas bitácoras no aparecerán en la lista de documentos")
                        appendLine("  hasta que se asocien con un usuario")
                        appendLine()
                        appendLine("💡 SOLUCIÓN:")
                        appendLine("  Presiona 'Asociar Bitácoras' para solucionarlo")
                    } else if (stats.totalLogbooks == 0) {
                        appendLine("ℹ️ INFORMACIÓN:")
                        appendLine("  No hay bitácoras en la base de datos")
                        appendLine("  Los PDFs que ves son archivos antiguos")
                        appendLine("  Crea una nueva bitácora para probar el sistema")
                    } else {
                        appendLine("✅ SISTEMA FUNCIONANDO CORRECTAMENTE")
                        appendLine("  Todas las bitácoras están asociadas con usuarios")
                    }
                }
                
                debugText.text = debugInfo
                
            } catch (e: Exception) {
                debugText.text = "Error cargando información de debug: ${e.message}\n\nStack trace:\n${e.stackTraceToString()}"
            }
        }
    }
    
    private fun showClearDataConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar TODOS los datos?\n\nEsto incluye:\n• Todas las bitácoras\n• Todos los usuarios\n• Todas las fotos\n• Todos los PDFs\n\nEsta acción NO se puede deshacer.")
            .setPositiveButton("ELIMINAR TODO") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                // TODO: Implementar limpieza completa de datos
                showToast("Función de limpieza en desarrollo")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    
    private fun exportDebugLogs() {
        try {
            val debugInfo = debugText.text.toString()
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "debug_log_$timestamp.txt"
            
            // TODO: Implementar exportación de logs
            showToast("Función de exportación en desarrollo")
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
