package com.messagetrans.presentation.ui.settings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messagetrans.data.database.entities.SimConfig
import com.messagetrans.databinding.ItemSimConfigBinding

class SimConfigAdapter(
    private val onToggleClick: (SimConfig) -> Unit
) : ListAdapter<SimConfig, SimConfigAdapter.ViewHolder>(SimConfigDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimConfigBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSimConfigBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(simConfig: SimConfig) {
            binding.apply {
                textDisplayName.text = simConfig.displayName
                textCarrierName.text = simConfig.carrierName
                textPhoneNumber.text = simConfig.phoneNumber ?: "未知号码"
                textSimType.text = when(simConfig.simType) {
                    "esim" -> "eSIM"
                    "physical" -> "实体卡"
                    else -> "未知类型"
                }
                
                switchEnabled.isChecked = simConfig.isEnabled
                switchEnabled.setOnCheckedChangeListener { _, _ ->
                    onToggleClick(simConfig)
                }
            }
        }
    }

    private class SimConfigDiffCallback : DiffUtil.ItemCallback<SimConfig>() {
        override fun areItemsTheSame(oldItem: SimConfig, newItem: SimConfig): Boolean {
            return oldItem.slotIndex == newItem.slotIndex
        }

        override fun areContentsTheSame(oldItem: SimConfig, newItem: SimConfig): Boolean {
            return oldItem == newItem
        }
    }
}