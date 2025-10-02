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
 * Fragmento de debug para mostrar el estado de usuarios y bitácoras
 */
class UserDebugFragment : Fragment() {
    
    private lateinit var appDatabaseManager: AppDatabaseManager
    private lateinit var debugText: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnResetMigration: Button
    private lateinit var btnAssociateLogbooks: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_debug, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        appDatabaseManager = AppDatabaseManager(requireContext())
        debugText = view.findViewById(R.id.debug_text)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnResetMigration = view.findViewById(R.id.btn_reset_migration)
        btnAssociateLogbooks = view.findViewById(R.id.btn_associate_logbooks)
        
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
                    Toast.makeText(requireContext(), "Migración reiniciada", Toast.LENGTH_SHORT).show()
                    loadDebugInfo()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        btnAssociateLogbooks.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val success = appDatabaseManager.migrationService.associateUnassociatedLogbooks()
                    if (success) {
                        Toast.makeText(requireContext(), "Bitácoras asociadas exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No se pudieron asociar las bitácoras", Toast.LENGTH_SHORT).show()
                    }
                    loadDebugInfo()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadDebugInfo() {
        lifecycleScope.launch {
            try {
                val stats = appDatabaseManager.migrationService.getDetailedMigrationStats()
                val currentUser = appDatabaseManager.userManagementService.getCurrentUser()
                
                val debugInfo = buildString {
                    appendLine("=== ESTADO DE USUARIOS Y BITÁCORAS ===")
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
                        appendLine("  Wallet: ${currentUser.walletAddress ?: "No disponible"}")
                        appendLine("  DID: ${currentUser.did ?: "No disponible"}")
                        appendLine("  Biometría: ${if (currentUser.biometricEnabled) "Habilitada" else "Deshabilitada"}")
                        appendLine()
                    } else {
                        appendLine("⚠️ NO HAY USUARIO ACTUAL")
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
                        appendLine("⚠️ HAY ${stats.unassociatedLogbooks} BITÁCORAS NO ASOCIADAS")
                        appendLine("  Estas bitácoras no aparecerán en la lista de documentos")
                        appendLine("  hasta que se asocien con un usuario")
                    }
                }
                
                debugText.text = debugInfo
                
            } catch (e: Exception) {
                debugText.text = "Error cargando información de debug: ${e.message}"
            }
        }
    }
}
