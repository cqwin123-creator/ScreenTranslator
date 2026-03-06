package com.example.screentranslator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.screentranslator.R
import com.example.screentranslator.helper.OCRHelper
import com.example.screentranslator.helper.ScreenCaptureHelper
import com.example.screentranslator.helper.TranslationHelper
import com.example.screentranslator.ui.MainActivity
import com.example.screentranslator.view.TranslationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_CHANNEL_ID = "screen_translator_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"

        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var windowManager: WindowManager
    private var floatingButtonView: View? = null
    private var floatingButtonParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var translationView: TranslationView? = null
    private var progressView: View? = null

    private lateinit var screenCaptureHelper: ScreenCaptureHelper
    private lateinit var ocrHelper: OCRHelper
    private var translationHelper: TranslationHelper? = null

    private var screenWidth = 0
    private var screenHeight = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenCaptureHelper = ScreenCaptureHelper(this)
        ocrHelper = OCRHelper()

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        screenWidth = size.x
        screenHeight = size.y

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isNotBlank()) {
            translationHelper = TranslationHelper(apiKey)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)

                if (resultCode != -1 && data != null) {
                    screenCaptureHelper.initMediaProjection(resultCode, data)
                    startForegroundService()
                    showFloatingButton()
                    isRunning = true
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        removeAllViews()
        ocrHelper.release()
        screenCaptureHelper.release()
        isRunning = false
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("屏幕翻译")
            .setContentText("翻译服务正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "屏幕翻译服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持屏幕翻译服务运行"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopService() {
        removeAllViews()
        stopForeground(true)
        stopSelf()
    }

    private fun removeAllViews() {
        try {
            floatingButtonView?.let { windowManager.removeView(it) }
            floatingButtonView = null
        } catch (e: Exception) { }

        try {
            translationView?.let { windowManager.removeView(it) }
            translationView = null
        } catch (e: Exception) { }

        try {
            progressView?.let { windowManager.removeView(it) }
            progressView = null
        } catch (e: Exception) { }
    }

    private fun showFloatingButton() {
        if (floatingButtonView != null) return

        val inflater = LayoutInflater.from(this)
        floatingButtonView = inflater.inflate(R.layout.layout_floating_button, null)

        setupFloatingButtonInteractions()

        floatingButtonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - dpToPx(80f)
            y = screenHeight / 2
        }

        windowManager.addView(floatingButtonView, floatingButtonParams)
    }

    private fun setupFloatingButtonInteractions() {
        floatingButtonView?.let { view ->
            val buttonLayout = view.findViewById<FrameLayout>(R.id.floating_button_layout)
            val translateButton = view.findViewById<ImageButton>(R.id.btn_translate)
            val closeButton = view.findViewById<ImageButton>(R.id.btn_close)

            translateButton?.setOnClickListener {
                performTranslation()
            }

            closeButton?.setOnClickListener {
                stopService()
            }

            buttonLayout?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = floatingButtonParams?.x ?: 0
                        initialY = floatingButtonParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        floatingButtonParams?.x = initialX + deltaX
                        floatingButtonParams?.y = initialY + deltaY
                        windowManager.updateViewLayout(floatingButtonButton, floatingButtonParams)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun performTranslation() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""

        if (apiKey.isBlank()) {
            Toast.makeText(this, "请先配置Kimi API Key", Toast.LENGTH_LONG).show()
            return
        }

        if (translationHelper == null) {
            translationHelper = TranslationHelper(apiKey)
        }

        floatingButtonView?.visibility = View.GONE
        showProgressView()

        serviceScope.launch {
            try {
                kotlinx.coroutines.delay(300)

                val screenshot = screenCaptureHelper.captureScreen()

                if (screenshot == null) {
                    withContext(Dispatchers.Main) {
                        hideProgressView()
                        floatingButtonView?.visibility = View.VISIBLE
                        Toast.makeText(this@FloatingWindowService, "屏幕捕获失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val ocrResult = ocrHelper.recognizeText(screenshot)

                if (ocrResult.text.isBlank()) {
                    withContext(Dispatchers.Main) {
                        hideProgressView()
                        floatingButtonView?.visibility = View.VISIBLE
                        Toast.makeText(this@FloatingWindowService, "未识别到文字", Toast.LENGTH_SHORT).show()
                    }
                    screenshot.recycle()
                    return@launch
                }

                val displayMode = prefs.getString("display_mode", "overlay") ?: "overlay"
                val translations = mutableListOf<String>()

                for (block in ocrResult.blocks) {
                    val result = translationHelper?.translate(block.text)
                    if (result?.isSuccess == true) {
                        translations.add(result.translatedText)
                    } else {
                        translations.add("[翻译失败]")
                    }
                }

                withContext(Dispatchers.Main) {
                    hideProgressView()
                    floatingButtonView?.visibility = View.VISIBLE

                    if (displayMode == "overlay") {
                        showOverlayTranslation(ocrResult, translations)
                    } else {
                        showBottomPanelTranslation(ocrResult, translations)
                    }
                }

                screenshot.recycle()

            } catch (e: Exception) {
                Log.e(TAG, "Translation error", e)
                withContext(Dispatchers.Main) {
                    hideProgressView()
                    floatingButtonView?.visibility = View.VISIBLE
                    Toast.makeText(this@FloatingWindowService, "翻译失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showOverlayTranslation(
        ocrResult: OCRHelper.OCRResult,
        translations: List<String>
    ) {
        translationView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
        }

        translationView = TranslationView(this).apply {
            setDisplayMode(TranslationView.DisplayMode.OVERLAY)
            showOverlayTranslations(ocrResult, translations)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(translationView, params)

        mainHandler.postDelayed({
            translationView?.hide()
        }, 5000)
    }

    private fun showBottomPanelTranslation(
        ocrResult: OCRHelper.OCRResult,
        translations: List<String>
    ) {
        translationView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
        }

        translationView = TranslationView(this).apply {
            setDisplayMode(TranslationView.DisplayMode.BOTTOM_PANEL)
            showBottomPanelTranslations(ocrResult, translations)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(translationView, params)
    }

    private fun showProgressView() {
        if (progressView != null) return

        progressView = LayoutInflater.from(this).inflate(R.layout.layout_progress, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(progressView, params)
    }

    private fun hideProgressView() {
        try {
            progressView?.let { windowManager.removeView(it) }
            progressView = null
        } catch (e: Exception) { }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
