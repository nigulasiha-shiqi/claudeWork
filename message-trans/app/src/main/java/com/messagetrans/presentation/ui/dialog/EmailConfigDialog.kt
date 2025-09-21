package com.messagetrans.presentation.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
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

            val config = EmailConfig(
                id = emailConfig?.id ?: java.util.UUID.randomUUID().toString(),
                displayName = displayName,
                emailAddress = emailAddress,
                smtpServer = smtpServer,
                smtpPort = smtpPort,
                username = username,
                password = password,
                useSSL = binding.switchSslEnabled.isChecked,
                isEnabled = binding.switchEnabled.isChecked
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