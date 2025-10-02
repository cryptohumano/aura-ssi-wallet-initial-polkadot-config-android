package com.aura.substratecryptotest.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.ui.debug.DebugFragment
import com.aura.substratecryptotest.data.user.DetailedMigrationStats
import com.aura.substratecryptotest.data.user.ProfileImageService
import com.aura.substratecryptotest.ui.components.UserProfileCard
import kotlinx.coroutines.launch

/**
 * Fragmento de configuración con modo desarrollador
 */
class SettingsFragment : Fragment() {
    
    private lateinit var appDatabaseManager: AppDatabaseManager
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var profileImageService: ProfileImageService
    
    // UI Elements
    private lateinit var developerModeSwitch: Switch
    private lateinit var developerSection: View
    private lateinit var debugInfoText: TextView
    private lateinit var btnOpenDebug: Button
    private lateinit var btnResetMigration: Button
    private lateinit var btnAssociateLogbooks: Button
    private lateinit var btnClearAllData: Button
    
    // User Profile Elements
    private lateinit var userProfileImage: android.widget.ImageView
    private lateinit var userNameText: TextView
    private lateinit var userWalletText: TextView
    private lateinit var userDidText: TextView
    private lateinit var biometricIcon: android.widget.ImageView
    
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        android.util.Log.d("SettingsFragment", "onCreateView llamado")
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("SettingsFragment", "onViewCreated llamado")
        
        appDatabaseManager = AppDatabaseManager(requireContext())
        sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        profileImageService = ProfileImageService(requireContext())
        
        initializeViews(view)
        setupDeveloperMode()
        loadSettings()
        setupClickListeners()
        loadUserProfile()
        
        android.util.Log.d("SettingsFragment", "SettingsFragment inicializado correctamente")
    }
    
    private fun initializeViews(view: View) {
        android.util.Log.d("SettingsFragment", "Inicializando vistas...")
        developerModeSwitch = view.findViewById(R.id.switch_developer_mode)
        developerSection = view.findViewById(R.id.developer_section)
        debugInfoText = view.findViewById(R.id.debug_info_text)
        btnOpenDebug = view.findViewById(R.id.btn_open_debug)
        btnResetMigration = view.findViewById(R.id.btn_reset_migration)
        btnAssociateLogbooks = view.findViewById(R.id.btn_associate_logbooks)
        btnClearAllData = view.findViewById(R.id.btn_clear_all_data)
        
        // User Profile Views
        userProfileImage = view.findViewById(R.id.user_profile_image)
        userNameText = view.findViewById(R.id.user_name_text)
        userWalletText = view.findViewById(R.id.user_wallet_text)
        userDidText = view.findViewById(R.id.user_did_text)
        biometricIcon = view.findViewById(R.id.biometric_icon)
        
        android.util.Log.d("SettingsFragment", "Vistas inicializadas correctamente")
    }
    
    private fun setupDeveloperMode() {
        developerModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean(KEY_DEVELOPER_MODE, isChecked).apply()
            updateDeveloperSectionVisibility(isChecked)
            if (isChecked) {
                loadDebugInfo()
            }
        }
    }
    
    private fun loadSettings() {
        val isDeveloperMode = sharedPrefs.getBoolean(KEY_DEVELOPER_MODE, false)
        developerModeSwitch.isChecked = isDeveloperMode
        updateDeveloperSectionVisibility(isDeveloperMode)
        
        if (isDeveloperMode) {
            loadDebugInfo()
        }
    }
    
    private fun updateDeveloperSectionVisibility(isVisible: Boolean) {
        developerSection.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
    
    private fun setupClickListeners() {
        btnOpenDebug.setOnClickListener {
            // Por ahora, mostrar un toast indicando que la funcionalidad está en desarrollo
            showToast("Debug completo disponible en modo desarrollador")
            // TODO: Implementar navegación al fragmento de debug
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
    }
    
    private fun loadDebugInfo() {
        lifecycleScope.launch {
            try {
                val stats = appDatabaseManager.migrationService.getDetailedMigrationStats()
                val currentUser = appDatabaseManager.userManagementService.getCurrentUser()
                
                val debugInfo = buildString {
                    appendLine("=== ESTADO DEL SISTEMA ===")
                    appendLine()
                    
                    appendLine("👤 USUARIOS:")
                    appendLine("  Total: ${stats.totalUsers}")
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
                    appendLine("  Total: ${stats.totalLogbooks}")
                    appendLine("  Asociadas: ${stats.associatedLogbooks}")
                    appendLine("  No asociadas: ${stats.unassociatedLogbooks}")
                    appendLine()
                    
                    appendLine("🔄 MIGRACIÓN:")
                    appendLine("  Completada: ${if (stats.migrationCompleted) "Sí" else "No"}")
                    appendLine()
                    
                    if (stats.unassociatedLogbooks > 0) {
                        appendLine("⚠️ HAY ${stats.unassociatedLogbooks} BITÁCORAS NO ASOCIADAS")
                        appendLine("  Usa 'Asociar Bitácoras' para solucionarlo")
                    }
                }
                
                debugInfoText.text = debugInfo
                
            } catch (e: Exception) {
                debugInfoText.text = "Error cargando información: ${e.message}"
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
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val currentUser = appDatabaseManager.userManagementService.getCurrentUser()
                
                if (currentUser != null) {
                    userNameText.text = currentUser.name
                    userWalletText.text = "Wallet: ${currentUser.walletAddress ?: "No disponible"}"
                    userDidText.text = "DID: ${currentUser.did ?: "No disponible"}"
                    
                    // Mostrar icono de biometría si está habilitada
                    biometricIcon.visibility = if (currentUser.biometricEnabled) View.VISIBLE else View.GONE
                    
                    // Cargar imagen de perfil
                    if (currentUser.profileImagePath != null) {
                        val profileImage = profileImageService.loadProfileImage(currentUser.profileImagePath)
                        if (profileImage != null) {
                            userProfileImage.setImageBitmap(profileImage)
                        } else {
                            // Si no se puede cargar la imagen, mostrar iniciales
                            showUserInitials(currentUser.name)
                        }
                    } else {
                        // Generar imagen de perfil si no existe
                        val newImagePath = profileImageService.generateDefaultProfileImage(currentUser.name)
                        if (newImagePath != null) {
                            val profileImage = profileImageService.loadProfileImage(newImagePath)
                            if (profileImage != null) {
                                userProfileImage.setImageBitmap(profileImage)
                            } else {
                                showUserInitials(currentUser.name)
                            }
                        } else {
                            showUserInitials(currentUser.name)
                        }
                    }
                } else {
                    userNameText.text = "Usuario no disponible"
                    userWalletText.text = "Wallet: No disponible"
                    userDidText.text = "DID: No disponible"
                    biometricIcon.visibility = View.GONE
                    showUserInitials("Usuario")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Error cargando perfil de usuario: ${e.message}", e)
                userNameText.text = "Error cargando usuario"
                userWalletText.text = "Error"
                userDidText.text = "Error"
                biometricIcon.visibility = View.GONE
                showUserInitials("Error")
            }
        }
    }
    
    private fun showUserInitials(userName: String) {
        val initials = getUserInitials(userName)
        userProfileImage.setImageDrawable(null)
        userProfileImage.setBackgroundColor(android.graphics.Color.parseColor("#FF6B6B"))
        
        // Crear un bitmap con las iniciales
        val bitmap = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 80f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        canvas.drawText(initials, 100f, 120f, paint)
        userProfileImage.setImageBitmap(bitmap)
    }
    
    private fun getUserInitials(userName: String): String {
        val words = userName.trim().split("\\s+".toRegex())
        return when {
            words.isEmpty() -> "U"
            words.size == 1 -> words[0].take(1).uppercase()
            else -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
        }
    }
}
