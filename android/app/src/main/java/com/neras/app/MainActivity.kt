package com.neras.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var web: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#FBF8F2"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true          // saves the child's profile, PIN, stars
            settings.mediaPlaybackRequiresUserGesture = false
            settings.textZoom = 100
            addJavascriptInterface(Bridge(), "AndroidTTS")
            webViewClient = object : WebViewClient() {
                // Phone, WhatsApp, email, TikTok and website links open in their own apps
                override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                    val url = req.url.toString()
                    if (url.startsWith("file:")) return false
                    return try {
                        val action = if (url.startsWith("tel:")) Intent.ACTION_DIAL else Intent.ACTION_VIEW
                        startActivity(Intent(action, Uri.parse(url)))
                        true
                    } catch (e: Exception) { true }
                }
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(web)

        // Back button: inside a game → return to the child's home; otherwise leave the app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.onAndroidBack && window.onAndroidBack()) ? 'y' : 'n'") { r ->
                    if (r?.contains("n") == true) { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val r = tts?.setLanguage(Locale("ar", "SA"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts?.setLanguage(Locale("ar"))
            tts?.setSpeechRate(0.85f)
            ttsReady = true
        }
    }

    inner class Bridge {
        @JavascriptInterface fun speak(text: String) {
            if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "neras")
        }
        @JavascriptInterface fun stop() { tts?.stop() }
        @JavascriptInterface fun copy(text: String) {
            runOnUiThread {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Neras", text))
            }
        }
    }

    override fun onPause() { tts?.stop(); super.onPause() }
    override fun onDestroy() { tts?.shutdown(); web.destroy(); super.onDestroy() }
}
