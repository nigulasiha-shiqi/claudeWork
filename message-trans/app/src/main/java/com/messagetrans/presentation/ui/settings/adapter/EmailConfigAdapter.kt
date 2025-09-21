package com.messagetrans.presentation.ui.settings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.databinding.ItemEmailConfigBinding

class EmailConfigAdapter(
    private val onEditClick: (EmailConfig) -> Unit,
    private val onDeleteClick: (EmailConfig) -> Unit,
    private val onToggleClick: (EmailConfig) -> Unit
) : ListAdapter<EmailConfig, EmailConfigAdapter.ViewHolder>(EmailConfigDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmailConfigBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemEmailConfigBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(emailConfig: EmailConfig) {
            binding.apply {
                textDisplayName.text = emailConfig.displayName
                textEmailAddress.text = emailConfig.emailAddress
                textSmtpServer.text = "${emailConfig.smtpServer}:${emailConfig.smtpPort}"
                
                switchEnabled.isChecked = emailConfig.isEnabled
                switchEnabled.setOnCheckedChangeListener { _, _ ->
                    onToggleClick(emailConfig)
                }
                
                buttonEdit.setOnClickListener {
                    onEditClick(emailConfig)
                }
                
                buttonDelete.setOnClickListener {
                    onDeleteClick(emailConfig)
                }
            }
        }
    }

    private class EmailConfigDiffCallback : DiffUtil.ItemCallback<EmailConfig>() {
        override fun areItemsTheSame(oldItem: EmailConfig, newItem: EmailConfig): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EmailConfig, newItem: EmailConfig): Boolean {
            return oldItem == newItem
        }
    }
}