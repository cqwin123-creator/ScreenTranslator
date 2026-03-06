package com.example.screentranslator.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.example.screentranslator.helper.OCRHelper

class TranslationView(context: Context) : View(context) {

    enum class DisplayMode {
        OVERLAY, BOTTOM_PANEL
    }

    private var displayMode = DisplayMode.OVERLAY
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blocks = mutableListOf<OCRHelper.TextBlock>()
    private val translations = mutableListOf<String>()
    private var bgAlpha = 0.8f
    private var textSize = 14f

    init {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        bgAlpha = prefs.getFloat("bg_alpha", 0.8f)
        textSize = prefs.getFloat("text_size", 14f)
    }

    fun setDisplayMode(mode: DisplayMode) {
        displayMode = mode
        invalidate()
    }

    fun showOverlayTranslations(ocrResult: OCRHelper.OCRResult, translations: List<String>) {
        this.blocks.clear()
        this.blocks.addAll(ocrResult.blocks)
        this.translations.clear()
        this.translations.addAll(translations)
        invalidate()
    }

    fun showBottomPanelTranslations(ocrResult: OCRHelper.OCRResult, translations: List<String>) {
        // 底部面板模式简化处理
        this.blocks.clear()
        this.blocks.addAll(ocrResult.blocks)
        this.translations.clear()
        this.translations.addAll(translations)
        invalidate()
    }

    fun hide() {
        visibility = GONE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (displayMode == DisplayMode.OVERLAY) {
            drawOverlayMode(canvas)
        } else {
            drawBottomPanelMode(canvas)
        }
    }

    private fun drawOverlayMode(canvas: Canvas) {
        bgPaint.color = Color.argb((bgAlpha * 255).toInt(), 0, 0, 0)
        textPaint.color = Color.WHITE
        textPaint.textSize = textSize * resources.displayMetrics.scaledDensity

        for (i in blocks.indices) {
            val block = blocks[i]
            val translation = translations.getOrElse(i) { "" }
            val rect = block.boundingBox ?: continue

            // 绘制背景
            val padding = 8
            val bgRect = RectF(
                (rect.left - padding).toFloat(),
                (rect.top - padding).toFloat(),
                (rect.right + padding).toFloat(),
                (rect.bottom + padding).toFloat()
            )
            canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)

            // 绘制文字
            val textX = rect.left.toFloat()
            val textY = rect.top + textPaint.textSize
            canvas.drawText(translation, textX, textY, textPaint)
        }
    }

    private fun drawBottomPanelMode(canvas: Canvas) {
        // 绘制底部半透明面板
        val panelHeight = height / 3
        bgPaint.color = Color.argb((bgAlpha * 255).toInt(), 0, 0, 0)
        canvas.drawRect(0f, (height - panelHeight).toFloat(), width.toFloat(), height.toFloat(), bgPaint)

        // 绘制翻译文字
        textPaint.color = Color.WHITE
        textPaint.textSize = textSize * resources.displayMetrics.scaledDensity

        var y = (height - panelHeight + 50).toFloat()
        for (translation in translations) {
            canvas.drawText(translation, 50f, y, textPaint)
            y += textPaint.textSize * 2
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            hide()
        }
        return true
    }
}
