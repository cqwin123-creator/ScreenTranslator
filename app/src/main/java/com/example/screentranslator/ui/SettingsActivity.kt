package com.example.screentranslator.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.screentranslator.databinding.ActivitySettingsBinding
import com.example.screentranslator.helper.TranslationHelper
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        binding.etApiKey.setText(prefs.getString("api_key", ""))
        
        val displayMode = prefs.getString("display_mode", "overlay")
        if (displayMode == "overlay") {
            binding.rbOverlay.isChecked = true
        } else {
            binding.rbBottom.isChecked = true
        }
        
        binding.sliderAlpha.value = prefs.getFloat("bg_alpha", 0.8f)
        binding.sliderTextSize.value = prefs.getFloat("text_size", 14f)
    }
    
    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        binding.btnTestApi.setOnClickListener {
            testApiKey()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun saveSettings() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("api_key", binding.etApiKey.text.toString().trim())
            putString("display_mode", if (binding.rbOverlay.isChecked) "overlay" else "bottom")
            putFloat("bg_alpha", binding.sliderAlpha.value)
            putFloat("text_size", binding.sliderTextSize.value)
            apply()
        }
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun testApiKey() {
        val apiKey = binding.etApiKey.text.toString().trim()
        
        if (apiKey.isBlank()) {
            Toast.makeText(this, "请输入API Key", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.btnTestApi.isEnabled = false
        binding.btnTestApi.text = "验证中..."
        
        lifecycleScope.launch {
            val helper = TranslationHelper(apiKey)
            val isValid = helper.validateApiKey()
            
            binding.btnTestApi.isEnabled = true
            binding.btnTestApi.text = "测试API Key"
            
            if (isValid) {
                Toast.makeText(this@SettingsActivity, "API Key有效", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SettingsActivity, "API Key无效", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
