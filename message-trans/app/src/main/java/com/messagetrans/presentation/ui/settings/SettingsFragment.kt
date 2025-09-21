package com.messagetrans.presentation.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.messagetrans.databinding.FragmentSettingsBinding
import com.messagetrans.presentation.ui.settings.adapter.EmailConfigAdapter
import com.messagetrans.presentation.ui.settings.adapter.SimConfigAdapter
import com.messagetrans.presentation.viewmodel.SettingsViewModel

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SettingsViewModel
    private lateinit var emailAdapter: EmailConfigAdapter
    private lateinit var simAdapter: SimConfigAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        emailAdapter = EmailConfigAdapter(
            onEditClick = { config -> 
                // TODO: 打开编辑邮箱对话框
            },
            onDeleteClick = { config ->
                viewModel.deleteEmailConfig(config)
            },
            onToggleClick = { config ->
                viewModel.toggleEmailConfig(config)
            }
        )
        
        simAdapter = SimConfigAdapter(
            onToggleClick = { config ->
                viewModel.toggleSimConfig(config)
            }
        )
        
        binding.recyclerEmailConfigs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = emailAdapter
        }
        
        binding.recyclerSimConfigs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = simAdapter
        }
    }

    private fun setupObservers() {
        viewModel.emailConfigs.observe(viewLifecycleOwner) { configs ->
            emailAdapter.submitList(configs)
        }
        
        viewModel.simConfigs.observe(viewLifecycleOwner) { configs ->
            simAdapter.submitList(configs)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEmail.setOnClickListener {
            // TODO: 打开添加邮箱对话框
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}