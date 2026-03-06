package com.example.screentranslator.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.screentranslator.databinding.ActivityMainBinding
import com.example.screentranslator.service.FloatingWindowService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkOverlayPermission()
        } else {
            // 权限被拒绝，引导用户去设置中开启
            showNotificationPermissionDialog()
        }
    }
    
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startFloatingService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "需要屏幕录制权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnStart.setOnClickListener {
            checkPermissionsAndStart()
        }
        
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnLanguage.setOnClickListener {
            startActivity(Intent(this, LanguageDownloadActivity::class.java))
        }
        
        updateUIState()
    }
    
    override fun onResume() {
        super.onResume()
        updateUIState()
        
        // 如果从设置页面返回，检查权限是否已开启
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 用户已在设置中开启权限，继续检查其他权限
                // 注意：这里不自动启动服务，避免循环跳转
            }
        }
    }
    
    private fun updateUIState() {
        val isRunning = FloatingWindowService.isRunning
        binding.btnStart.text = if (isRunning) "翻译服务运行中" else "启动翻译"
        binding.btnStart.isEnabled = !isRunning
    }
    
    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，继续检查悬浮窗权限
                    checkOverlayPermission()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 可以显示权限说明
                    AlertDialog.Builder(this)
                        .setTitle("需要通知权限")
                        .setMessage("翻译服务需要在后台运行，需要通知权限来保持服务运行。")
                        .setPositiveButton("允许") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                else -> {
                    // 直接请求权限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            checkOverlayPermission()
        }
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("通知权限被拒绝")
            .setMessage("翻译服务需要通知权限才能在后台运行。请在设置中手动开启通知权限。")
            .setPositiveButton("去设置") { _, _ ->
                // 跳转到应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("请允许应用显示在其他应用上方")
                .setPositiveButton("去设置") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            requestScreenCapture()
        }
    }
    
    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
    
    private fun startFloatingService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_START
            putExtra(FloatingWindowService.EXTRA_RESULT_CODE, resultCode)
            putExtra(FloatingWindowService.EXTRA_DATA, data)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, "翻译服务已启动", Toast.LENGTH_SHORT).show()
        updateUIState()
    }
}
