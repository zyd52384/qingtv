package com.qingkan.tv

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.qingkan.tv.data.ChannelData
import com.qingkan.tv.databinding.ActivityWebBinding

class WebActivity : ComponentActivity() {

    private lateinit var binding: ActivityWebBinding
    private lateinit var webView: WebView
    private var isFullscreen = false
    private var showOverlay = true // 初始显示频道列表覆盖层
    private var currentChannelIndex = 0
    private var pageLoaded = false

    companion object {
        private const val JS_CLEAN_PAGE = """
            javascript:(function() {
                // 隐藏所有导航栏、顶部菜单、底部信息
                var css = document.createElement('style');
                css.type = 'text/css';
                css.innerHTML = `
                    .topnav, .header, .footer, .nav, .navbar,
                    .cctv-nav, .cctv-top, .cctv-footer,
                    .g-nav, .g-topnav, .g-footer,
                    .side-nav, .sidenav, .sidebar,
                    .login-box, .login-pop, .popup-layer,
                    .banner, .ad-banner, .ad-box,
                    .zqj-menu, .menu-box,
                    .hot-words, .hotwords,
                    .link-box, .links-box,
                    .recommend-box, .relate-box,
                    [class*="nav"], [class*="menu"],
                    [class*="sidebar"], [class*="sidebar"],
                    [class*="login"], [class*="popup"],
                    [class*="ad-"], [class*="banner"],
                    [class*="footer"], [class*="header"],
                    [class*="recommend"], [class*="relate"],
                    #popup, #bpopup, #nav, #header, #footer,
                    .login, .popup, .advertisement,
                    .copyright, .copy-right
                { display: none !important; }
                    
                body { 
                    margin: 0 !important; 
                    padding: 0 !important; 
                    background: #000 !important;
                    overflow: hidden !important;
                }
                
                /* 让视频容器充满屏幕 */
                .video-box, .player-box, .live-box, 
                [class*="player"], [class*="video"],
                [class*="live"], [class*="stream"],
                #player, #video, #live,
                .cctv-player, .cctv-live,
                .content-box, .main-box,
                .container, .wrap
                {
                    position: fixed !important;
                    top: 0 !important;
                    left: 0 !important;
                    width: 100% !important;
                    height: 100% !important;
                    max-width: 100% !important;
                    max-height: 100% !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    z-index: 1 !important;
                    background: #000 !important;
                }
                
                video, iframe, embed, object {
                    width: 100% !important;
                    height: 100% !important;
                    max-width: 100% !important;
                    max-height: 100% !important;
                }
                `;
                document.head.appendChild(css);
                
                // 关闭弹窗
                var closeBtns = document.querySelectorAll('.close, .popup-close, [class*="close"]');
                closeBtns.forEach(function(b) { b.click(); });
                
                // 自动播放视频
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) { v.muted = false; v.play(); });
                
                // 移除背景滚动
                document.documentElement.style.overflow = 'hidden';
                document.body.style.overflow = 'hidden';
            })();
        """
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取启动的频道
        val channelIndex = intent.getIntExtra("channel_index", 0)
        currentChannelIndex = channelIndex.coerceIn(0, ChannelData.channels.size - 1)

        webView = binding.webview
        setupWebView()
        setupChannelOverlay()

        // 加载第一个频道
        loadChannel(currentChannelIndex)

        // 默认全屏（Pro版）
        if (BuildConfig.IS_PRO) {
            hideSystemBars(true)
            isFullscreen = true
        }

        // 顶部按钮
        binding.btnBack.setOnClickListener { finish() }

        // 免费版 - 升级按钮
        if (!BuildConfig.IS_PRO) {
            binding.bottomBar.visibility = View.VISIBLE
            binding.btnUpgrade.setOnClickListener {
                showUpgradeDialog()
            }
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Linux; Android 11; MIBOX4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Safari/537.36"
            allowFileAccess = true
            loadWithOverviewMode = true
        }

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onPageReady() {
                Handler(Looper.getMainLooper()).post {
                    pageLoaded = true
                    // 页面加载完后清理和自动播放
                    webView.evaluateJavascript(JS_CLEAN_PAGE, null)
                }
            }
        }, "androidApp")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                pageLoaded = false
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                // 延迟清理页面，等页面完全渲染
                Handler(Looper.getMainLooper()).postDelayed({
                    webView.evaluateJavascript(JS_CLEAN_PAGE, null)
                }, 1500)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
            }
        }
    }

    private fun loadChannel(index: Int) {
        val channels = ChannelData.channels
        if (index < 0 || index >= channels.size) return

        currentChannelIndex = index
        val channel = channels[index]

        // 更新顶部信息
        binding.tvChannelName.text = channel.name
        binding.tvChannelInfo.text = "${channel.id} · ${channel.name}"

        // 隐藏覆盖层
        binding.channelOverlay.visibility = View.GONE
        showOverlay = false

        // 加载
        webView.loadUrl(channel.url)
    }

    private fun setupChannelOverlay() {
        val channels = ChannelData.channels
        binding.channelGrid.layoutManager = GridLayoutManager(this, 4)
        binding.channelGrid.adapter = object : RecyclerView.Adapter<ChannelViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
                val view = layoutInflater.inflate(R.layout.item_channel, parent, false)
                return ChannelViewHolder(view)
            }

            override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
                val ch = channels[position]
                holder.tvId.text = ch.id.toString()
                holder.tvName.text = ch.name
                val isCurrent = position == currentChannelIndex
                holder.itemView.setBackgroundResource(
                    if (isCurrent) R.drawable.btn_bg_selected
                    else R.drawable.btn_bg
                )
                holder.itemView.setOnClickListener {
                    loadChannel(position)
                }
            }

            override fun getItemCount() = channels.size
        }
    }

    private fun toggleChannelOverlay() {
        showOverlay = !showOverlay
        binding.channelOverlay.visibility = if (showOverlay) View.VISIBLE else View.GONE
        binding.topBar.visibility = if (showOverlay) View.VISIBLE else View.GONE
        if (!BuildConfig.IS_PRO) {
            binding.bottomBar.visibility = if (showOverlay) View.VISIBLE else View.GONE
        }
        if (showOverlay) {
            hideSystemBars(false)
        } else if (BuildConfig.IS_PRO) {
            hideSystemBars(true)
        }
    }

    private fun channelUp() {
        val next = (currentChannelIndex - 1 + ChannelData.channels.size) % ChannelData.channels.size
        loadChannel(next)
    }

    private fun channelDown() {
        val next = (currentChannelIndex + 1) % ChannelData.channels.size
        loadChannel(next)
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
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // 中键/OK/Enter → 切换频道菜单
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                toggleChannelOverlay()
                true
            }

            // 上键 → 上一频道
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (showOverlay) {
                    // 在菜单模式，让RecyclerView处理
                    super.onKeyDown(keyCode, event)
                } else {
                    channelUp()
                    true
                }
            }

            // 下键 → 下一频道
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (showOverlay) {
                    super.onKeyDown(keyCode, event)
                } else {
                    channelDown()
                    true
                }
            }

            // 左右键 → 音量控制（留给系统）
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (showOverlay) {
                    super.onKeyDown(keyCode, event)
                } else {
                    true // 播放模式下忽略左右
                }
            }

            // 返回 → 回到频道列表
            KeyEvent.KEYCODE_BACK -> {
                if (showOverlay) {
                    // 隐藏菜单
                    toggleChannelOverlay()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
                true
            }

            // 菜单键 → 显示频道菜单
            KeyEvent.KEYCODE_MENU -> {
                toggleChannelOverlay()
                true
            }

            // 数字键快速换台
            KeyEvent.KEYCODE_0 -> { loadChannel(18); true } // CCTV新闻
            KeyEvent.KEYCODE_1 -> { loadChannel(0); true }
            KeyEvent.KEYCODE_2 -> { loadChannel(1); true }
            KeyEvent.KEYCODE_3 -> { loadChannel(2); true }
            KeyEvent.KEYCODE_4 -> { loadChannel(3); true }
            KeyEvent.KEYCODE_5 -> { loadChannel(4); true }
            KeyEvent.KEYCODE_6 -> { loadChannel(5); true }
            KeyEvent.KEYCODE_7 -> { loadChannel(6); true }
            KeyEvent.KEYCODE_8 -> { loadChannel(7); true }
            KeyEvent.KEYCODE_9 -> { loadChannel(8); true }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showUpgradeDialog() {
        AlertDialog.Builder(this)
            .setTitle("解锁全屏播放")
            .setMessage("加入轻看TV社群，解锁完整体验：\n\n" +
                "✅ 全屏沉浸播放\n" +
                "✅ 一键数字换台\n" +
                "✅ 频道自定义排序\n" +
                "✅ 专属频道源更新\n\n" +
                "轻看TV · 社群版：让电视回归简单")
            .setPositiveButton("了解详情") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("联系社群")
                    .setMessage("请添加微信：\n\nQingKanTV\n\n" +
                        "备注「轻看」加入社群\n" +
                        "社群会员可获取Pro版APK")
                    .setPositiveButton("好的", null)
                    .show()
            }
            .setNegativeButton("继续试用", null)
            .show()
    }

    class ChannelViewHolder(itemView: View) : ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tvId)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }
}
