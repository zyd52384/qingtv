package com.qingkan.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.qingkan.tv.data.ChannelData
import com.qingkan.tv.databinding.ActivityPlayerBinding
import com.qingkan.tv.model.Channel

class PlayerActivity : ComponentActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var currentIndex = 0
    private var isOverlayVisible = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private var channelBarVisible = false
    private val channelBarRunnable = Runnable { hideChannelBar() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentIndex = intent.getIntExtra("channel_index", 0)
            .coerceIn(0, ChannelData.channels.size - 1)

        setupPlayer()
        setupOverlay()

        // Pro: 全屏沉浸
        if (BuildConfig.IS_PRO) {
            hideSystemBars(true)
        } else {
            binding.bottomBar.visibility = View.VISIBLE
            binding.btnUpgrade.setOnClickListener { showUpgradeDialog() }
        }

        // 启动时显示频道条
        showChannelBar(ChannelData.channels[currentIndex])
    }

    private fun setupPlayer() {
        val channel = ChannelData.channels[currentIndex]

        player = ExoPlayer.Builder(this).build().apply {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(channel.url))
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // 播放失败 → 静默
                }
                override fun onPlaybackStateChanged(state: Int) {
                    binding.progressBar.visibility =
                        if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }
            })
        }

        binding.playerView.player = player
        binding.playerView.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_ALWAYS)
        binding.playerView.useController = false
    }

    private fun switchChannel(index: Int) {
        if (index < 0 || index >= ChannelData.channels.size) return
        currentIndex = index
        val channel = ChannelData.channels[index]
        player?.stop()
        player?.clearMediaItems()
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(channel.url))
        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.playWhenReady = true
        showChannelBar(channel)
    }

    private fun channelUp() {
        val n = if (currentIndex - 1 < 0) ChannelData.channels.size - 1 else currentIndex - 1
        switchChannel(n)
    }

    private fun channelDown() {
        val n = (currentIndex + 1) % ChannelData.channels.size
        switchChannel(n)
    }

    // ═══ 悬浮频道条 — 视频上方半透明状态条 ═══
    private fun showChannelBar(channel: Channel) {
        val groupTag = if (channel.group == "CCTV") "央视" else "卫视"
        binding.tvChannelBarName.text = channel.name
        binding.tvChannelBarInfo.text = "$groupTag · CH${channel.id}"
        binding.channelBar.visibility = View.VISIBLE
        channelBarVisible = true
        hideHandler.removeCallbacks(channelBarRunnable)
        hideHandler.postDelayed(channelBarRunnable, 4000)
    }

    private fun hideChannelBar() {
        binding.channelBar.visibility = View.GONE
        channelBarVisible = false
    }

    // ═══ 频道网格覆盖层 ═══
    private fun setupOverlay() {
        binding.channelGrid.layoutManager = GridLayoutManager(this, 5)
        binding.channelGrid.adapter = ChannelGridAdapter(ChannelData.channels, currentIndex) { index ->
            switchChannel(index)
            hideOverlay()
        }
    }

    private fun showOverlay() {
        (binding.channelGrid.adapter as? ChannelGridAdapter)?.currentIndex = currentIndex
        binding.channelGrid.adapter?.notifyDataSetChanged()
        binding.channelOverlay.visibility = View.VISIBLE
        isOverlayVisible = true
    }

    private fun hideOverlay() {
        binding.channelOverlay.visibility = View.GONE
        isOverlayVisible = false
    }

    private fun toggleOverlay() {
        if (isOverlayVisible) hideOverlay() else showOverlay()
    }

    // ═══ 遥控器按键 ═══
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MENU -> {
                toggleOverlay()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isOverlayVisible) {
                    binding.channelGrid.requestFocus()
                    super.onKeyDown(keyCode, event)
                } else { channelUp(); true }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isOverlayVisible) {
                    binding.channelGrid.requestFocus()
                    super.onKeyDown(keyCode, event)
                } else { channelDown(); true }
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isOverlayVisible) {
                    binding.channelGrid.requestFocus()
                    super.onKeyDown(keyCode, event)
                } else true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isOverlayVisible) { hideOverlay(); true }
                else { finish(); true }
            }
            // 数字快捷键: 1-9 → CCTV-1~9, 0 → CCTV-新闻
            KeyEvent.KEYCODE_0 -> { switchChannel(17); true }
            KeyEvent.KEYCODE_1 -> { switchChannel(0); true }
            KeyEvent.KEYCODE_2 -> { switchChannel(1); true }
            KeyEvent.KEYCODE_3 -> { switchChannel(2); true }
            KeyEvent.KEYCODE_4 -> { switchChannel(3); true }
            KeyEvent.KEYCODE_5 -> { switchChannel(4); true }
            KeyEvent.KEYCODE_6 -> { switchChannel(5); true }
            KeyEvent.KEYCODE_7 -> { switchChannel(6); true }
            KeyEvent.KEYCODE_8 -> { switchChannel(7); true }
            KeyEvent.KEYCODE_9 -> { switchChannel(8); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // ═══ 全屏 ═══
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

    private fun showUpgradeDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("解锁全屏播放")
            .setMessage("加入轻看TV社群，解锁完整体验：\n\n" +
                "✅ 全屏沉浸播放\n✅ 一键数字换台\n✅ 50+频道\n✅ 专属频道源更新\n\n" +
                "轻看TV · 社群版：让电视回归简单")
            .setPositiveButton("了解详情") { _, _ ->
                android.app.AlertDialog.Builder(this)
                    .setTitle("联系社群")
                    .setMessage("请添加微信：QingKanTV\n\n备注「轻看」加入社群\n社群会员可获取Pro版APK")
                    .setPositiveButton("好的", null).show()
            }
            .setNegativeButton("继续试用", null).show()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        hideHandler.removeCallbacks(channelBarRunnable)
        player?.release()
        player = null
        super.onDestroy()
    }
}

// ═══ 频道网格适配器 ═══
class ChannelGridAdapter(
    private val channels: List<Channel>,
    var currentIndex: Int,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<ChannelGridAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val ch = channels[i]
        h.tvName.text = ch.name
        val groupPrefix = if (ch.group == "CCTV") "C" else "S"
        h.tvId.text = "$groupPrefix${ch.id}"
        h.itemView.setBackgroundResource(
            if (i == currentIndex) R.drawable.btn_bg_selected
            else R.drawable.btn_bg
        )
        h.itemView.setOnClickListener { onClick(i) }
    }

    override fun getItemCount() = channels.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tvId)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }
}
