package com.aura.substratecryptotest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import com.aura.substratecryptotest.databinding.ActivityMainBinding
import com.aura.substratecryptotest.ui.WalletListFragment
import com.aura.substratecryptotest.ui.WalletInfoFragment
import com.aura.substratecryptotest.ui.ImportExportFragment
import com.aura.substratecryptotest.ui.SS58ToolsFragment
import com.aura.substratecryptotest.ui.NetworkStatusFragment
import com.aura.substratecryptotest.ui.SigningFragment
import com.aura.substratecryptotest.ui.TestCryptoFragment
import com.aura.substratecryptotest.utils.Logger
// import com.aura.substratecryptotest.ui.SDKVerificationFragment
// import com.aura.substratecryptotest.crypto.SubstrateCryptoManager
import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var walletManager: com.aura.substratecryptotest.wallet.WalletManager
    lateinit var networkManager: com.aura.substratecryptotest.network.NetworkManager
    // private lateinit var cryptoManager: SubstrateCryptoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar gestores
        walletManager = com.aura.substratecryptotest.wallet.WalletManager(this)
        networkManager = com.aura.substratecryptotest.network.NetworkManager(this)
        // cryptoManager = SubstrateCryptoManager()

        setupViewPager()
        setupFAB()
        setupLogging()
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_wallets)
                1 -> getString(R.string.tab_wallet_info)
                2 -> getString(R.string.tab_import_export)
                3 -> "SS58 Tools"
                4 -> "Redes"
                5 -> "Firmar"
                6 -> "Prueba ETH"
                else -> ""
            }
        }.attach()
    }

    private fun setupFAB() {
        binding.fabCreateWallet.setOnClickListener {
            showCreateWalletDialog()
        }
    }

    private fun showCreateWalletDialog() {
        // Implementar diálogo para crear wallet
        // Por ahora, crear un wallet de prueba usando el sistema existente
        try {
            // Crear wallet usando el WalletManager existente
            walletManager.createWallet(
                name = "Mi Wallet ${System.currentTimeMillis()}",
                password = null,
                mnemonicLength = Mnemonic.Length.TWELVE,
                cryptoType = EncryptionAlgorithm.SR25519
            )
            
            // Mostrar información básica
            println("Wallet creada exitosamente")
            
            // Mostrar información detallada de la wallet creada después de un delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                showWalletInfo()
            }, 500) // Esperar 500ms para que la wallet se establezca
            
        } catch (e: Exception) {
            // Manejar error
            e.printStackTrace()
        }
    }
    
    /**
     * Muestra información básica de la wallet creada
     */
    private fun showWalletInfo() {
        val walletInfo = walletManager.getCurrentWalletInfo()
        if (walletInfo != null) {
            // Mostrar información básica en logs
            Logger.i("MainActivity", "Wallet creada exitosamente: ${walletInfo.name}")
            
            // Mostrar información de DIDs KILT si están disponibles
            if (walletInfo.kiltDid != null) {
                Logger.success("MainActivity", "🆔 DID KILT generado", walletInfo.kiltDid)
                Logger.debug("MainActivity", "Dirección KILT", walletInfo.kiltAddress ?: "N/A")
                
                if (walletInfo.kiltDids != null && walletInfo.kiltDids.isNotEmpty()) {
                    Logger.debug("MainActivity", "DIDs KILT disponibles", 
                        walletInfo.kiltDids.keys.joinToString(", "))
                }
            } else {
                Logger.warning("MainActivity", "⚠️ No se generaron DIDs KILT", 
                    "La wallet se creó sin soporte KILT")
            }
            
            // Copiar automáticamente el mnemonic al portapapeles
            copyToClipboard("Mnemonic", walletInfo.mnemonic)
            
            // Mostrar toast de confirmación con información KILT
            val kiltInfo = if (walletInfo.kiltDid != null) {
                "\n🆔 DID KILT: ${walletInfo.kiltDid.take(20)}..."
            } else {
                "\n⚠️ Sin DIDs KILT"
            }
            Toast.makeText(this, "✅ Wallet creada: ${walletInfo.name}$kiltInfo", Toast.LENGTH_LONG).show()
        } else {
            Logger.e("MainActivity", "No hay wallet actual disponible")
            Toast.makeText(this, "❌ Error creando wallet", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Copia texto al portapapeles
     */
    private fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(this, "✅ $label copiado al portapapeles", Toast.LENGTH_SHORT).show()
            println("✅ $label copiado al portapapeles: $text")
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error copiando $label", Toast.LENGTH_SHORT).show()
            println("❌ Error copiando $label: ${e.message}")
        }
    }
    
    /**
     * Copia el mnemonic de la wallet actual
     */
    fun copyCurrentWalletMnemonic() {
        val mnemonic = walletManager.getCurrentWalletMnemonic()
        if (mnemonic != null) {
            copyToClipboard("Mnemonic", mnemonic)
        } else {
            Toast.makeText(this, "❌ No hay wallet actual", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Copia la dirección de la wallet actual
     */
    fun copyCurrentWalletAddress() {
        val address = walletManager.getCurrentWalletAddress()
        if (address != null) {
            copyToClipboard("Dirección", address)
        } else {
            Toast.makeText(this, "❌ No hay wallet actual", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Copia la clave pública de la wallet actual
     */
    fun copyCurrentWalletPublicKey() {
        val publicKey = walletManager.getCurrentWalletPublicKey()
        if (publicKey != null) {
            copyToClipboard("Clave Pública", publicKey)
        } else {
            Toast.makeText(this, "❌ No hay wallet actual", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Copia el DID KILT de la wallet actual
     */
    fun copyCurrentWalletKiltDid() {
        val kiltDid = walletManager.getCurrentWalletKiltDid()
        if (kiltDid != null) {
            copyToClipboard("DID KILT", kiltDid)
        } else {
            Toast.makeText(this, "❌ No hay DID KILT disponible", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Copia la dirección KILT de la wallet actual
     */
    fun copyCurrentWalletKiltAddress() {
        val kiltAddress = walletManager.getCurrentWalletKiltAddress()
        if (kiltAddress != null) {
            copyToClipboard("Dirección KILT", kiltAddress)
        } else {
            Toast.makeText(this, "❌ No hay dirección KILT disponible", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Configura el sistema de logging según el modo de compilación
     */
    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            // En modo debug, habilitar logging detallado
            Logger.enableDebugMode()
            Logger.logStatus()
        } else {
            // En modo release, solo errores y advertencias
            Logger.enableProductionMode()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar recursos del NetworkManager
        networkManager.cleanup()
    }
    
    // private fun showWalletInfo(walletInfo: SubstrateCryptoManager.WalletInfo) {
    //     // Mostrar información de la wallet creada
    //     // TODO: Implementar diálogo o notificación
    //     println("Wallet creada:")
    //     println("Mnemonic: ${walletInfo.mnemonic.words}")
    //     println("Válido: ${walletInfo.mnemonic.isValid}")
    //     println("Algoritmos soportados: ${walletInfo.keyPairs.keys}")
    //     println("Direcciones generadas: ${walletInfo.addresses.size}")
    // }

    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 7

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WalletListFragment.newInstance()
                1 -> WalletInfoFragment.newInstance()
                2 -> ImportExportFragment.newInstance()
                3 -> SS58ToolsFragment.newInstance()
                4 -> NetworkStatusFragment.newInstance()
                5 -> SigningFragment.newInstance()
                6 -> TestCryptoFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
