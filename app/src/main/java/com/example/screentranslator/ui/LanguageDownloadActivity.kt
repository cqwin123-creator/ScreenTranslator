package com.example.screentranslator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screentranslator.R
import com.example.screentranslator.helper.OCRHelper
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class LanguageDownloadActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotalSize: TextView
    private lateinit var btnBack: Button
    private lateinit var ocrHelper: OCRHelper
    private lateinit var adapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_download)

        ocrHelper = OCRHelper(this)

        recyclerView = findViewById(R.id.recycler_languages)
        tvTotalSize = findViewById(R.id.tv_total_size)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        updateTotalSize()
    }

    private fun setupRecyclerView() {
        adapter = LanguageAdapter(
            languages = ocrHelper.availableLanguages,
            isDownloaded = { ocrHelper.isLanguageDownloaded(it) },
            onDownload = { language, onProgress ->
                lifecycleScope.launch {
                    val success = ocrHelper.downloadLanguage(language.code) { progress ->
                        runOnUiThread { onProgress(progress) }
                    }
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this@LanguageDownloadActivity, 
                                "${language.name} 下载完成", Toast.LENGTH_SHORT).show()
                            updateTotalSize()
                        } else {
                            Toast.makeText(this@LanguageDownloadActivity, 
                                "${language.name} 下载失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDelete = { language ->
                val success = ocrHelper.deleteLanguage(language.code)
                if (success) {
                    Toast.makeText(this, "${language.name} 已删除", Toast.LENGTH_SHORT).show()
                    updateTotalSize()
                }
                success
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun updateTotalSize() {
        val size = ocrHelper.getDownloadedLanguagesSize()
        val formattedSize = formatFileSize(size)
        tvTotalSize.text = "已下载语言数据: $formattedSize"
    }

    private fun formatFileSize(size: Long): String {
        val df = DecimalFormat("#.00")
        return when {
            size >= 1024 * 1024 * 1024 -> "${df.format(size / (1024.0 * 1024.0 * 1024.0))} GB"
            size >= 1024 * 1024 -> "${df.format(size / (1024.0 * 1024.0))} MB"
            size >= 1024 -> "${df.format(size / 1024.0)} KB"
            else -> "$size B"
        }
    }

    // Adapter
    class LanguageAdapter(
        private val languages: List<OCRHelper.LanguageInfo>,
        private val isDownloaded: (String) -> Boolean,
        private val onDownload: (OCRHelper.LanguageInfo, (Int) -> Unit) -> Unit,
        private val onDelete: (OCRHelper.LanguageInfo) -> Boolean
    ) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_language_name)
            val tvSize: TextView = view.findViewById(R.id.tv_language_size)
            val tvStatus: TextView = view.findViewById(R.id.tv_language_status)
            val progressBar: ProgressBar = view.findViewById(R.id.progress_download)
            val btnAction: Button = view.findViewById(R.id.btn_action)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_language, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val language = languages[position]
            val downloaded = isDownloaded(language.code)

            holder.tvName.text = language.name
            holder.tvSize.text = "大小: ${language.size}"
            holder.progressBar.visibility = View.GONE

            if (downloaded) {
                holder.tvStatus.text = "状态: 已下载"
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
                holder.btnAction.text = "删除"
                holder.btnAction.setOnClickListener {
                    if (onDelete(language)) {
                        notifyItemChanged(position)
                    }
                }
            } else {
                holder.tvStatus.text = "状态: 未下载"
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                holder.btnAction.text = "下载"
                holder.btnAction.setOnClickListener {
                    holder.btnAction.isEnabled = false
                    holder.btnAction.text = "下载中..."
                    holder.progressBar.visibility = View.VISIBLE
                    holder.progressBar.progress = 0

                    onDownload(language) { progress ->
                        holder.progressBar.progress = progress
                    }
                }
            }
        }

        override fun getItemCount() = languages.size
    }
}
