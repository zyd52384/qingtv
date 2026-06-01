package com.qingkan.tv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.qingkan.tv.data.ChannelData
import com.qingkan.tv.databinding.ActivityMainBinding
import com.qingkan.tv.model.Channel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.channelGrid.layoutManager = GridLayoutManager(this, 4)
        binding.channelGrid.adapter = ChannelAdapter(ChannelData.channels) { channel ->
            openChannel(channel)
        }
    }

    private fun openChannel(channel: Channel) {
        val intent = Intent(this, WebActivity::class.java).apply {
            putExtra("channel_name", channel.name)
            putExtra("channel_url", channel.url)
        }
        startActivity(intent)
    }

    class ChannelAdapter(
        private val channels: List<Channel>,
        private val onClick: (Channel) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_channel, parent, false)
            return ChannelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
            val channel = channels[position]
            holder.tvId.text = channel.id.toString()
            holder.tvName.text = channel.name
            holder.itemView.setOnClickListener { onClick(channel) }
            holder.itemView.setOnFocusChangeListener { v, hasFocus ->
                v?.alpha = if (hasFocus) 1.0f else 0.7f
            }
        }

        override fun getItemCount() = channels.size

        class ChannelViewHolder(itemView: View) : ViewHolder(itemView) {
            val tvId: TextView = itemView.findViewById(R.id.tvId)
            val tvName: TextView = itemView.findViewById(R.id.tvName)
        }
    }
}
