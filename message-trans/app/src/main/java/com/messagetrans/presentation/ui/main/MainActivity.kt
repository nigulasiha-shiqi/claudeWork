package com.messagetrans.presentation.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.messagetrans.R
import com.messagetrans.databinding.ActivityMainBinding
import com.messagetrans.utils.PermissionManager
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        val permissionStatus = PermissionManager.checkAllPermissions(this)
        Log.d(TAG, "Permission status: $permissionStatus")
        
        if (!permissionStatus.hasBasicPermissions) {
            Log.d(TAG, "Requesting missing permissions")
            PermissionManager.requestBasicPermissions(this, PERMISSION_REQUEST_CODE)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val permissionStatus = PermissionManager.checkAllPermissions(this)
            Log.d(TAG, "Permission result: $permissionStatus")
            
            if (!permissionStatus.hasBasicPermissions) {
                Log.w(TAG, "Some permissions were denied: ${permissionStatus.missingPermissions}")
                // 可以在这里显示解释对话框或引导用户到设置
            }
        }
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
    }
}