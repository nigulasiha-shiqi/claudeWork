package com.messagetrans.presentation.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.messagetrans.databinding.FragmentSettingsBinding
import com.messagetrans.presentation.ui.settings.adapter.EmailConfigAdapter
import com.messagetrans.presentation.ui.settings.adapter.SimConfigAdapter
import com.messagetrans.presentation.ui.dialog.EmailConfigDialog
import com.messagetrans.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

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
        setupProxySettings()
    }

    private fun setupRecyclerViews() {
        emailAdapter = EmailConfigAdapter(
            onEditClick = { config -> 
                showEmailConfigDialog(config)
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
            showEmailConfigDialog()
        }
        
        binding.buttonRefreshSim.setOnClickListener {
            viewModel.refreshSimConfigs()
        }
        
        binding.buttonExportConfig.setOnClickListener {
            exportEmailConfigs()
        }
        
        binding.buttonImportConfig.setOnClickListener {
            importEmailConfigs()
        }
        
        binding.buttonTestProxy.setOnClickListener {
            testProxyConnection()
        }
    }

    private fun showEmailConfigDialog(emailConfig: com.messagetrans.data.database.entities.EmailConfig? = null) {
        val dialog = EmailConfigDialog(emailConfig) { config ->
            if (emailConfig == null) {
                viewModel.addEmailConfig(config)
            } else {
                viewModel.updateEmailConfig(config)
            }
        }
        dialog.show(parentFragmentManager, "EmailConfigDialog")
    }

    private fun exportEmailConfigs() {
        val exportString = viewModel.exportEmailConfigs()
        if (exportString != null) {
            // 复制到剪贴板
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("邮件配置", exportString)
            clipboard.setPrimaryClip(clip)
            
            // 显示导出成功对话框
            AlertDialog.Builder(requireContext())
                .setTitle("配置导出成功")
                .setMessage("邮件配置已复制到剪贴板，请保存到安全位置。\n\n注意：配置包含敏感信息，请妥善保管。")
                .setPositiveButton("确定", null)
                .setNeutralButton("查看") { _, _ ->
                    showExportDataDialog(exportString)
                }
                .show()
            
            Toast.makeText(context, "配置已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "没有可导出的配置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importEmailConfigs() {
        // 从剪贴板获取数据
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        val clipboardText = if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).text?.toString() ?: ""
        } else {
            ""
        }
        
        // 显示导入对话框
        val editText = EditText(requireContext()).apply {
            setText(clipboardText)
            hint = "请粘贴邮件配置数据"
            maxLines = 5
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("导入邮件配置")
            .setMessage("请粘贴之前导出的邮件配置数据：")
            .setView(editText)
            .setPositiveButton("导入") { _, _ ->
                val importString = editText.text.toString().trim()
                if (importString.isNotEmpty()) {
                    if (viewModel.importEmailConfigs(importString)) {
                        Toast.makeText(context, "配置导入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "配置导入失败，请检查数据格式", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "请输入配置数据", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showExportDataDialog(exportString: String) {
        val editText = EditText(requireContext()).apply {
            setText(exportString)
            isEnabled = false
            maxLines = 10
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("导出的配置数据")
            .setView(editText)
            .setPositiveButton("复制") { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("邮件配置", exportString)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun setupProxySettings() {
        // 监听代理开关状态
        binding.switchProxyEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutProxyConfig.visibility = if (isChecked) View.VISIBLE else View.GONE
            viewModel.updateProxyEnabled(isChecked)
        }
        
        // 监听代理配置变化
        setupProxyConfigListeners()
    }
    
    private fun setupProxyConfigListeners() {
        binding.editProxyType.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val proxyType = binding.editProxyType.text.toString().trim()
                viewModel.updateProxyType(proxyType)
            }
        }
        
        binding.editProxyHost.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val host = binding.editProxyHost.text.toString().trim()
                viewModel.updateProxyHost(host)
            }
        }
        
        binding.editProxyPort.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val portText = binding.editProxyPort.text.toString().trim()
                val port = portText.toIntOrNull() ?: 8080
                viewModel.updateProxyPort(port)
            }
        }
        
        binding.editProxyUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val username = binding.editProxyUsername.text.toString().trim()
                viewModel.updateProxyUsername(username)
            }
        }
        
        binding.editProxyPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = binding.editProxyPassword.text.toString()
                viewModel.updateProxyPassword(password)
            }
        }
    }
    
    private fun testProxyConnection() {
        // 先保存当前的代理配置
        saveCurrentProxyConfig()
        
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("测试代理连接")
            .setMessage("正在测试代理连接，请稍候...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                val result = viewModel.testProxyConnection()
                progressDialog.dismiss()
                
                val (isSuccess, message) = result
                
                AlertDialog.Builder(requireContext())
                    .setTitle(if (isSuccess) "测试成功" else "测试失败")
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .show()
                    
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(requireContext())
                    .setTitle("测试失败")
                    .setMessage("代理测试发生异常: ${e.message}")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }
    
    private fun saveCurrentProxyConfig() {
        val isEnabled = binding.switchProxyEnabled.isChecked
        val proxyType = binding.editProxyType.text.toString()
        val host = binding.editProxyHost.text.toString().trim()
        val portText = binding.editProxyPort.text.toString().trim()
        val port = portText.toIntOrNull() ?: 8080
        val username = binding.editProxyUsername.text.toString().trim()
        val password = binding.editProxyPassword.text.toString()
        
        viewModel.saveProxyConfig(isEnabled, proxyType, host, port, username, password)
    }
    
    private fun loadProxyConfig() {
        viewModel.currentEmailConfig.observe(viewLifecycleOwner) { emailConfig ->
            emailConfig?.let { config ->
                binding.switchProxyEnabled.isChecked = config.useProxy
                binding.layoutProxyConfig.visibility = if (config.useProxy) View.VISIBLE else View.GONE
                binding.editProxyType.setText(config.proxyType)
                binding.editProxyHost.setText(config.proxyHost)
                binding.editProxyPort.setText(config.proxyPort.toString())
                binding.editProxyUsername.setText(config.proxyUsername)
                binding.editProxyPassword.setText(config.proxyPassword)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProxyConfig()
    }
    
    override fun onPause() {
        super.onPause()
        saveCurrentProxyConfig()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}