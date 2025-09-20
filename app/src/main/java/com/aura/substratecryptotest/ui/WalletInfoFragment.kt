package com.aura.substratecryptotest.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aura.substratecryptotest.MainActivity
import com.aura.substratecryptotest.databinding.FragmentWalletInfoBinding
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.wallet.WalletInfo

class WalletInfoFragment : Fragment() {
    
    private var _binding: FragmentWalletInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var walletManager: WalletManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletInfoBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener WalletManager desde la actividad principal
        walletManager = (activity as? MainActivity)?.walletManager 
            ?: throw IllegalArgumentException("WalletManager es requerido")
        
        setupUI()
        loadWalletInfo()
    }
    
    private fun setupUI() {
        // Configurar botones de copia
        binding.btnCopyMnemonic.setOnClickListener {
            copyMnemonic()
        }
        
        binding.btnCopyAddress.setOnClickListener {
            copyAddress()
        }
        
        binding.btnCopyPublicKey.setOnClickListener {
            copyPublicKey()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadWalletInfo()
        }
    }
    
    private fun loadWalletInfo() {
        val walletInfo = walletManager.getCurrentWalletInfo()
        if (walletInfo != null) {
            displayWalletInfo(walletInfo)
        } else {
            showNoWalletMessage()
        }
    }
    
    private fun displayWalletInfo(walletInfo: WalletInfo) {
        // Información básica
        binding.textWalletName.text = walletInfo.name
        binding.textWalletId.text = "ID: ${walletInfo.id}"
        binding.textCryptoType.text = "Tipo: ${walletInfo.cryptoType}"
        binding.textDerivationPath.text = "Ruta: ${walletInfo.getFormattedDerivationPath()}"
        binding.textCreatedAt.text = "Creado: ${walletInfo.getFormattedCreatedAt()}"
        
        // Mnemonic formateado
        binding.textMnemonicFormatted.text = walletInfo.getFormattedMnemonic()
        binding.textMnemonicSingleLine.text = walletInfo.mnemonic
        
        // Clave pública
        binding.textPublicKey.text = walletInfo.publicKey
        
        // Dirección
        binding.textAddress.text = walletInfo.address
        
        // Mostrar el contenedor de información
        binding.containerWalletInfo.visibility = View.VISIBLE
        binding.containerNoWallet.visibility = View.GONE
    }
    
    private fun showNoWalletMessage() {
        binding.containerWalletInfo.visibility = View.GONE
        binding.containerNoWallet.visibility = View.VISIBLE
    }
    
    private fun copyMnemonic() {
        val mnemonic = walletManager.getCurrentWalletMnemonic()
        if (mnemonic != null) {
            copyToClipboard("Mnemonic", mnemonic)
        } else {
            showToast("❌ No hay wallet actual")
        }
    }
    
    private fun copyAddress() {
        val address = walletManager.getCurrentWalletAddress()
        if (address != null) {
            copyToClipboard("Dirección", address)
        } else {
            showToast("❌ No hay wallet actual")
        }
    }
    
    private fun copyPublicKey() {
        val publicKey = walletManager.getCurrentWalletPublicKey()
        if (publicKey != null) {
            copyToClipboard("Clave Pública", publicKey)
        } else {
            showToast("❌ No hay wallet actual")
        }
    }
    
    private fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast("✅ $label copiado al portapapeles")
        } catch (e: Exception) {
            showToast("❌ Error copiando $label")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): WalletInfoFragment {
            return WalletInfoFragment()
        }
    }
}
