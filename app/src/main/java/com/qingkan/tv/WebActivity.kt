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
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qingkan.tv.data.ChannelData
import com.qingkan.tv.databinding.ActivityWebBinding

class WebActivity : ComponentActivity() {

    private lateinit var binding: ActivityWebBinding
    private lateinit var webView: WebView
    private var showOverlay = true
    private var currentChannelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channelIndex = intent.getIntExtra("channel_index", 0)
        currentChannelIndex = channelIndex.coerceIn(0, ChannelData.channels.size - 1)

        webView = binding.webview

        // ★ 核心：WebView不可获取焦点，所有按键事件由Activity拦截
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
        // 不让触控焦点乱跳
        webView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        setupWebView()
        setupChannelOverlay()

        loadChannel(currentChannelIndex)

        // 默认全屏（Pro版）
        if (BuildConfig.IS_PRO) {
            hideSystemBars(true)
        }

        binding.btnBack.setOnClickListener { finish() }

        // 免费版升级按钮
        if (!BuildConfig.IS_PRO) {
            binding.bottomBar.visibility = View.VISIBLE
            binding.btnUpgrade.setOnClickListener { showUpgradeDialog() }
        }
    }

    private fun setupWebView() {
        // ★ 强制硬件渲染（对WebView视频播放至关重要）
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            allowContentAccess = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = true
            // Android TV User-Agent（模拟原生TV浏览器）
            userAgentString = "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                // 页面加载完 → 清理 + 自动播放视频
                Handler(Looper.getMainLooper()).postDelayed({
                    cleanAndPlay()
                }, 1500)
            }
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {}
    }

    private fun cleanAndPlay() {
        val js = """
            (function(){
                // 1. 清理页面杂物
                var s=document.createElement('style');
                s.innerHTML='.topnav,.header,.footer,.nav,.navbar,.cctv-nav,.cctv-top,.cctv-footer,.g-nav,.g-topnav,.g-footer,.side-nav,.sidenav,.sidebar,.login-box,.login-pop,.popup-layer,.banner,.ad-banner,.ad-box,.zqj-menu,.menu-box,.hot-words,.hotwords,.link-box,.links-box,.recommend-box,.relate-box,[class*="nav"],[class*="menu"],[class*="sidebar"],[class*="login"],[class*="popup"],[class*="ad-"],[class*="banner"],[class*="footer"],[class*="header"],[class*="recommend"],[class*="relate"],#popup,#bpopup,#nav,#header,#footer,.login,.popup,.advertisement,.copyright,.copy-right{display:none!important}body{margin:0!important;padding:0!important;background:#000!important;overflow:hidden!important}.video-box,.player-box,.live-box,[class*="player"],[class*="video"],[class*="live"],[class*="stream"],#player,#video,#live,.cctv-player,.cctv-live,.content-box,.main-box,.container,.wrap{position:fixed!important;top:0!important;left:0!important;width:100%!important;height:100%!important;margin:0!important;padding:0!important;z-index:1!important;background:#000!important}video,iframe,embed,object{width:100%!important;height:100%!important}';
                document.head.appendChild(s);

                // 2. 关弹窗
                document.querySelectorAll('.close,.popup-close,.pop-close,.video-close,.tcaptcha,.mask').forEach(function(b){b.click()});

                // 3. 找到视频元素并强制播放
                var played=false;
                function tryPlay(el){
                    if(!el || played) return;
                    el.muted=true;
                    el.playsInline=true;
                    el.autoplay=true;
                    el.preload='auto';
                    el.play().then(function(){played=true;}).catch(function(e){console.log('play err:'+e.message);});
                }
                document.querySelectorAll('video').forEach(tryPlay);

                // 4. 点击所有可能是播放按钮的元素
                document.querySelectorAll('.play-btn,.player-btn,.play-icon,.play,.video-play,.cctv-play,.start-play,.big-play,[class*="play-btn"],[class*="play-icon"],[class*="start-play"]').forEach(function(b){b.click()});

                // 5. 监听DOM变化 — 新加入的视频也触发播放
                var obs=new MutationObserver(function(){
                    document.querySelectorAll('video:not([data-qingtv])').forEach(function(el){
                        el.setAttribute('data-qingtv','1');
                        tryPlay(el);
                        el.parentElement&&el.parentElement.click();
                    });
                });
                obs.observe(document.body||document,{childList:true,subtree:true});

                // 6. 滚动所有容器（某些页面需要滚动后才加载视频）
                document.querySelectorAll('[class*="scroll"],[class*="container"]').forEach(function(el){el.scrollTop=0});

                // 7. 再次尝试自动播放（setInterval until played）
                if(!played){
                    var iv=setInterval(function(){
                        document.querySelectorAll('video').forEach(function(el){
                            if(!el.src&&!el.querySelector('source')) return;
                            el.muted=true;el.autoplay=true;
                            el.play();
                        });
                    },1000);
                    setTimeout(function(){clearInterval(iv);},10000);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun loadChannel(index: Int) {
        val channels = ChannelData.channels
        if (index < 0 || index >= channels.size) return
        currentChannelIndex = index
        val channel = channels[index]

        binding.tvChannelName.text = channel.name
        binding.tvChannelInfo.text = "${channel.id} · ${channel.name}"

        // 隐藏菜单
        binding.channelOverlay.visibility = View.GONE
        showOverlay = false

        webView.loadUrl(channel.url)
    }

    private fun setupChannelOverlay() {
        val channels = ChannelData.channels
        binding.channelGrid.layoutManager = GridLayoutManager(this, 4)
        binding.channelGrid.adapter = object : RecyclerView.Adapter<ChannelVH>() {
            override fun onCreateViewHolder(p: ViewGroup, t: Int): ChannelVH {
                val v = layoutInflater.inflate(R.layout.item_channel, p, false)
                return ChannelVH(v)
            }
            override fun onBindViewHolder(h: ChannelVH, i: Int) {
                val ch = channels[i]
                h.tvId.text = ch.id.toString()
                h.tvName.text = ch.name
                h.itemView.setBackgroundResource(
                    if (i == currentChannelIndex) R.drawable.btn_bg_selected
                    else R.drawable.btn_bg
                )
                h.itemView.setOnClickListener { loadChannel(i) }
            }
            override fun getItemCount() = channels.size
        }
    }

    private fun toggleOverlay() {
        showOverlay = !showOverlay
        binding.channelOverlay.visibility = if (showOverlay) View.VISIBLE else View.GONE
    }

    private fun channelUp() {
        val n = (currentChannelIndex - 1 + ChannelData.channels.size) % ChannelData.channels.size
        loadChannel(n)
    }

    private fun channelDown() {
        val n = (currentChannelIndex + 1) % ChannelData.channels.size
        loadChannel(n)
    }

    private fun hideSystemBars(hide: Boolean) {
        if (hide) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // OK/Enter/菜单 → 呼出/隐藏频道菜单
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MENU -> {
                toggleOverlay()
                true
            }

            // 上下键 → 换台（菜单打开时让列表处理）
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (showOverlay) {
                    binding.channelGrid.requestFocus()
                    super.onKeyDown(keyCode, event)
                } else { channelUp(); true }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (showOverlay) {
                    binding.channelGrid.requestFocus()
                    super.onKeyDown(keyCode, event)
                } else { channelDown(); true }
            }

            // 左右 → 不影响播放
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> true

            // 返回 → 有菜单先关菜单
            KeyEvent.KEYCODE_BACK -> {
                if (showOverlay) { toggleOverlay(); true }
                else { finish(); true }
            }

            // 数字快速换台
            KeyEvent.KEYCODE_0 -> { loadChannel(17); true }
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
                "✅ 全屏沉浸播放\n✅ 一键数字换台\n✅ 频道自定义排序\n✅ 专属频道源更新\n\n" +
                "轻看TV · 社群版：让电视回归简单")
            .setPositiveButton("了解详情") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("联系社群")
                    .setMessage("请添加微信：QingKanTV\n\n备注「轻看」加入社群\n社群会员可获取Pro版APK")
                    .setPositiveButton("好的", null).show()
            }
            .setNegativeButton("继续试用", null).show()
    }

    class ChannelVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tvId)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }

    companion object {}
}
