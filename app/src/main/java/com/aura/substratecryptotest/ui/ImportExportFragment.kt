package com.aura.substratecryptotest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.databinding.FragmentImportExportBinding
import com.aura.substratecryptotest.wallet.WalletManager
import com.google.android.material.snackbar.Snackbar

class ImportExportFragment : Fragment() {

    private var _binding: FragmentImportExportBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var walletManager: WalletManager

    companion object {
        fun newInstance(walletManager: WalletManager): ImportExportFragment {
            val fragment = ImportExportFragment()
            fragment.walletManager = walletManager
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeWalletManager()
    }

    private fun setupClickListeners() {
        binding.buttonImportFromMnemonic.setOnClickListener {
            importFromMnemonic()
        }

        binding.buttonImportFromJson.setOnClickListener {
            importFromJson()
        }

        binding.buttonExportToJson.setOnClickListener {
            exportToJson()
        }
    }

    private fun observeWalletManager() {
        walletManager.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                walletManager.clearError()
            }
        })
    }

    private fun importFromMnemonic() {
        val name = binding.editTextImportName.text.toString().trim()
        val mnemonic = binding.editTextImportMnemonic.text.toString().trim()
        val password = binding.editTextImportPassword.text.toString().trim()

        if (name.isEmpty() || mnemonic.isEmpty()) {
            Snackbar.make(binding.root, "Completa el nombre y el mnemonic", Snackbar.LENGTH_SHORT).show()
            return
        }

        // walletManager.importWalletFromMnemonic(
        //     name = name,
        //     mnemonic = mnemonic,
        //     password = if (password.isNotEmpty()) password else null
        // )
        // Implementación temporal - usar importWalletFromJson
        walletManager.importWalletFromJson(
            name = name,
            jsonString = "{\"mnemonic\":\"$mnemonic\"}", // JSON temporal
            password = if (password.isNotEmpty()) password else null
        )
    }

    private fun importFromJson() {
        val name = binding.editTextImportJsonName.text.toString().trim()
        val json = binding.editTextImportJson.text.toString().trim()
        val password = binding.editTextImportJsonPassword.text.toString().trim()

        if (name.isEmpty() || json.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Completa todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }

        walletManager.importWalletFromJson(
            name = name,
            jsonString = json,
            password = password
        )
    }

    private fun exportToJson() {
        val password = binding.editTextExportPassword.text.toString().trim()
        
        if (password.isEmpty()) {
            Snackbar.make(binding.root, "Ingresa una contraseña para exportar", Snackbar.LENGTH_SHORT).show()
            return
        }

        // En una implementación real, necesitarías seleccionar qué wallet exportar
        Snackbar.make(binding.root, "Selecciona un wallet para exportar", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
