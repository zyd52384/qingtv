package com.qingkan.tv

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.qingkan.tv.databinding.ActivityWebBinding

class WebActivity : ComponentActivity() {

    private lateinit var binding: ActivityWebBinding
    private lateinit var webView: WebView
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channelName = intent.getStringExtra("channel_name") ?: "未知频道"
        val channelUrl = intent.getStringExtra("channel_url") ?: return

        binding.tvChannelName.text = channelName
        webView = binding.webview

        setupWebView(channelUrl)
        setupControls()

        // 免费版：默认不隐身，显示底部栏
        if (!BuildConfig.IS_PRO) {
            hideSystemBars(false)
        } else {
            // Pro版：默认隐身全屏
            hideSystemBars(true)
        }
    }

    private fun setupWebView(url: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Linux; Android 11; MIBOX4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
            }
        }

        webView.loadUrl(url)
    }

    private fun setupControls() {
        // 免费版 - 显示升级提示栏
        if (!BuildConfig.IS_PRO) {
            binding.bottomBar.visibility = View.VISIBLE
            binding.btnUpgrade.setOnClickListener {
                showUpgradeDialog()
            }
            binding.tvVersionInfo.text = "轻看TV · 免费版"
        } else {
            binding.bottomBar.visibility = View.GONE
        }

        // 返回键
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 全屏切换
        binding.btnFullscreen.setOnClickListener {
            if (BuildConfig.IS_PRO) {
                toggleFullscreen()
            } else {
                showUpgradeDialog()
            }
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        hideSystemBars(isFullscreen)
    }

    private fun hideSystemBars(hide: Boolean) {
        val window = window
        if (hide) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
            binding.topBar.visibility = View.GONE
            if (!BuildConfig.IS_PRO) binding.bottomBar.visibility = View.GONE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            binding.topBar.visibility = View.VISIBLE
            if (!BuildConfig.IS_PRO) binding.bottomBar.visibility = View.VISIBLE
        }
    }

    private fun showUpgradeDialog() {
        AlertDialog.Builder(this)
            .setTitle("解锁全屏播放")
            .setMessage("加入轻看TV社群，解锁完整体验：\n\n" +
                "✅ 全屏播放\n" +
                "✅ 一键数字换台\n" +
                "✅ 频道自定义排序\n" +
                "✅ 专属频道源更新\n\n" +
                "轻看TV · 社群版：让电视回归简单")
            .setPositiveButton("了解详情") { _, _ ->
                // 这里引导用户添加微信
                showContactDialog()
            }
            .setNegativeButton("继续试用", null)
            .show()
    }

    private fun showContactDialog() {
        AlertDialog.Builder(this)
            .setTitle("联系社群")
            .setMessage("请添加微信：\n\nQingKanTV\n\n" +
                "备注「轻看」加入社群\n" +
                "社群会员可获取Pro版APK")
            .setPositiveButton("好的", null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (webView.canGoBack()) {
                    webView.goBack()
                    true
                } else {
                    finish()
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // 遥控器确认键切换全屏（Pro版）
                if (BuildConfig.IS_PRO) {
                    toggleFullscreen()
                }
                true
            }
            KeyEvent.KEYCODE_MENU -> {
                if (BuildConfig.IS_PRO) {
                    // 菜单键显示/隐藏顶部栏
                    binding.topBar.visibility =
                        if (binding.topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
