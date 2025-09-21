package com.messagetrans.presentation.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.messagetrans.databinding.FragmentHomeBinding
import com.messagetrans.presentation.viewmodel.HomeViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.serviceStatus.observe(viewLifecycleOwner) { isRunning ->
            updateServiceStatus(isRunning)
        }
        
        viewModel.totalSmsCount.observe(viewLifecycleOwner) { count ->
            binding.textTotalSms.text = count.toString()
        }
        
        viewModel.forwardedSmsCount.observe(viewLifecycleOwner) { count ->
            binding.textForwardedSms.text = count.toString()
        }
    }

    private fun setupClickListeners() {
        binding.buttonToggleService.setOnClickListener {
            viewModel.toggleService()
        }
    }

    private fun updateServiceStatus(isRunning: Boolean) {
        if (isRunning) {
            binding.textServiceStatus.text = "服务运行中"
            binding.buttonToggleService.text = "停止服务"
            binding.indicatorStatus.setBackgroundResource(android.R.color.holo_green_dark)
        } else {
            binding.textServiceStatus.text = "服务已停止"
            binding.buttonToggleService.text = "启动服务"
            binding.indicatorStatus.setBackgroundResource(android.R.color.holo_red_dark)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}