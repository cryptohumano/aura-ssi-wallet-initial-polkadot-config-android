package com.aura.substratecryptotest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import com.aura.substratecryptotest.databinding.ActivityMainBinding
import com.aura.substratecryptotest.ui.WalletListFragment
import com.aura.substratecryptotest.ui.ImportExportFragment
import com.aura.substratecryptotest.ui.SDKVerificationFragment
// import com.aura.substratecryptotest.crypto.SubstrateCryptoManager
import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var walletManager: com.aura.substratecryptotest.wallet.WalletManager
    // private lateinit var cryptoManager: SubstrateCryptoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar gestores
        walletManager = com.aura.substratecryptotest.wallet.WalletManager(this)
        // cryptoManager = SubstrateCryptoManager()

        setupViewPager()
        setupFAB()
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_wallets)
                1 -> getString(R.string.tab_import_export)
                2 -> "Verificar SDK"
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
            
        } catch (e: Exception) {
            // Manejar error
            e.printStackTrace()
        }
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
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WalletListFragment.newInstance(walletManager)
                1 -> ImportExportFragment.newInstance(walletManager)
                2 -> SDKVerificationFragment.newInstance()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
