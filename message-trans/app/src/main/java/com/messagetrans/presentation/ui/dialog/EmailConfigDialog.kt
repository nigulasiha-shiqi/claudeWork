package com.messagetrans.presentation.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.databinding.DialogEmailConfigBinding

class EmailConfigDialog(
    private val emailConfig: EmailConfig? = null,
    private val onSave: (EmailConfig) -> Unit
) : DialogFragment() {

    private var _binding: DialogEmailConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEmailConfigBinding.inflate(LayoutInflater.from(context))
        
        setupViews()
        
        return AlertDialog.Builder(requireContext())
            .setTitle(if (emailConfig == null) "添加邮箱配置" else "编辑邮箱配置")
            .setView(binding.root)
            .setPositiveButton("保存") { _, _ ->
                saveEmailConfig()
            }
            .setNegativeButton("取消", null)
            .create()
    }

    private fun setupViews() {
        // 设置代理开关监听器
        binding.switchProxyEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutProxyConfig.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        emailConfig?.let { config ->
            binding.apply {
                editDisplayName.setText(config.displayName)
                editEmailAddress.setText(config.emailAddress)
                editSmtpServer.setText(config.smtpServer)
                editSmtpPort.setText(config.smtpPort.toString())
                editUsername.setText(config.username)
                editPassword.setText(config.password)
                switchSslEnabled.isChecked = config.useSSL
                switchEnabled.isChecked = config.isEnabled
                
                // 代理配置
                switchProxyEnabled.isChecked = config.useProxy
                layoutProxyConfig.visibility = if (config.useProxy) View.VISIBLE else View.GONE
                editProxyType.setText(config.proxyType)
                editProxyHost.setText(config.proxyHost)
                editProxyPort.setText(config.proxyPort.toString())
                editProxyUsername.setText(config.proxyUsername)
                editProxyPassword.setText(config.proxyPassword)
            }
        }
    }

    private fun saveEmailConfig() {
        try {
            val displayName = binding.editDisplayName.text.toString().trim()
            val emailAddress = binding.editEmailAddress.text.toString().trim()
            val smtpServer = binding.editSmtpServer.text.toString().trim()
            val smtpPortText = binding.editSmtpPort.text.toString().trim()
            val username = binding.editUsername.text.toString().trim()
            val password = binding.editPassword.text.toString()

            if (displayName.isEmpty() || emailAddress.isEmpty() || 
                smtpServer.isEmpty() || smtpPortText.isEmpty() ||
                username.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "请填写所有必填字段", Toast.LENGTH_SHORT).show()
                return
            }

            val smtpPort = smtpPortText.toIntOrNull()
            if (smtpPort == null || smtpPort !in 1..65535) {
                Toast.makeText(context, "请输入有效的端口号 (1-65535)", Toast.LENGTH_SHORT).show()
                return
            }

            // 获取代理配置
            val useProxy = binding.switchProxyEnabled.isChecked
            val proxyType = binding.editProxyType.text.toString()
            val proxyHost = binding.editProxyHost.text.toString().trim()
            val proxyPortText = binding.editProxyPort.text.toString().trim()
            val proxyPort = proxyPortText.toIntOrNull() ?: 8080
            val proxyUsername = binding.editProxyUsername.text.toString().trim()
            val proxyPassword = binding.editProxyPassword.text.toString()
            
            // 验证代理配置
            if (useProxy) {
                if (proxyHost.isEmpty()) {
                    Toast.makeText(context, "代理服务器地址不能为空", Toast.LENGTH_SHORT).show()
                    return
                }
                if (proxyPort !in 1..65535) {
                    Toast.makeText(context, "请输入有效的代理端口号 (1-65535)", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val config = EmailConfig(
                id = emailConfig?.id ?: java.util.UUID.randomUUID().toString(),
                displayName = displayName,
                emailAddress = emailAddress,
                smtpServer = smtpServer,
                smtpPort = smtpPort,
                username = username,
                password = password,
                useSSL = binding.switchSslEnabled.isChecked,
                isEnabled = binding.switchEnabled.isChecked,
                useProxy = useProxy,
                proxyType = proxyType,
                proxyHost = proxyHost,
                proxyPort = proxyPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword
            )

            onSave(config)
            
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}