package com.messagetrans.presentation.ui.permission

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.messagetrans.R
import com.messagetrans.databinding.ActivityPermissionGuideBinding
import com.messagetrans.presentation.ui.main.MainActivity
import com.messagetrans.utils.PermissionManager

class PermissionGuideActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private lateinit var binding: ActivityPermissionGuideBinding
    private var currentStep = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        checkPermissionsAndUpdateUI()
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateUI()
    }
    
    private fun setupViews() {
        binding.buttonNext.setOnClickListener {
            handleNextButtonClick()
        }
        
        binding.buttonSkip.setOnClickListener {
            finishGuide()
        }
    }
    
    private fun checkPermissionsAndUpdateUI() {
        val status = PermissionManager.checkAllPermissions(this)
        
        when {
            !status.hasBasicPermissions -> {
                showBasicPermissionStep()
            }
            !status.hasBatteryOptimization -> {
                showBatteryOptimizationStep()
            }
            !status.hasNotificationPermission -> {
                showNotificationPermissionStep()
            }
            else -> {
                showCompletionStep()
            }
        }
    }
    
    private fun showBasicPermissionStep() {
        currentStep = 1
        binding.apply {
            textStepTitle.text = "步骤 1: 授予基础权限"
            textStepDescription.text = "应用需要短信读取和接收权限才能正常工作"
            iconStep.setImageResource(R.drawable.ic_sim_card)
            buttonNext.text = "授予权限"
            buttonSkip.visibility = View.GONE
            progressStep.progress = 25
        }
    }
    
    private fun showBatteryOptimizationStep() {
        currentStep = 2
        binding.apply {
            textStepTitle.text = "步骤 2: 关闭电池优化"
            textStepDescription.text = "为了保证应用在后台正常运行，请将应用添加到电池优化白名单"
            iconStep.setImageResource(R.drawable.ic_play_arrow)
            buttonNext.text = "设置电池优化"
            buttonSkip.visibility = View.VISIBLE
            progressStep.progress = 50
        }
    }
    
    private fun showNotificationPermissionStep() {
        currentStep = 3
        binding.apply {
            textStepTitle.text = "步骤 3: 允许通知"
            textStepDescription.text = "允许应用发送通知以显示服务状态"
            iconStep.setImageResource(R.drawable.ic_email)
            buttonNext.text = "允许通知"
            buttonSkip.visibility = View.VISIBLE
            progressStep.progress = 75
        }
    }
    
    private fun showAutoStartStep() {
        currentStep = 4
        binding.apply {
            textStepTitle.text = "步骤 4: 设置自启动"
            textStepDescription.text = "为了确保应用开机自启，请在系统设置中允许自启动"
            iconStep.setImageResource(R.drawable.ic_settings)
            buttonNext.text = "设置自启动"
            buttonSkip.visibility = View.VISIBLE
            progressStep.progress = 90
        }
    }
    
    private fun showCompletionStep() {
        currentStep = 5
        binding.apply {
            textStepTitle.text = "设置完成"
            textStepDescription.text = "所有权限已授予，应用可以正常工作了！"
            iconStep.setImageResource(R.drawable.ic_home)
            iconStep.setColorFilter(ContextCompat.getColor(this@PermissionGuideActivity, R.color.success))
            buttonNext.text = "开始使用"
            buttonSkip.visibility = View.GONE
            progressStep.progress = 100
        }
    }
    
    private fun handleNextButtonClick() {
        when (currentStep) {
            1 -> {
                PermissionManager.requestBasicPermissions(this, PERMISSION_REQUEST_CODE)
            }
            2 -> {
                PermissionManager.requestDisableBatteryOptimization(this)
            }
            3 -> {
                PermissionManager.requestBasicPermissions(this, PERMISSION_REQUEST_CODE)
            }
            4 -> {
                PermissionManager.openAutoStartSettings(this)
            }
            5 -> {
                finishGuide()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissionsAndUpdateUI()
        }
    }
    
    private fun finishGuide() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}