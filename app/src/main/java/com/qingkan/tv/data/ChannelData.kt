package com.qingkan.tv.data

import com.qingkan.tv.model.Channel

object ChannelData {

    val channels: List<Channel> = listOf(
        // ═══ CCTV ═══
        Channel(1, "CCTV-1 综合", "http://74.91.26.218:82/live/cctv1hd.m3u8"),
        Channel(2, "CCTV-2 财经", "http://74.91.26.218:82/live/cctv2hd.m3u8"),
        Channel(3, "CCTV-3 综艺", "http://74.91.26.218:82/live/cctv3hd.m3u8"),
        Channel(4, "CCTV-4 中文国际", "http://74.91.26.218:82/live/cctv4hd.m3u8"),
        Channel(5, "CCTV-5 体育", "http://74.91.26.218:82/live/cctv5hd.m3u8"),
        Channel(6, "CCTV-5+ 体育赛事", "http://192.151.150.154/live/cctv5p.m3u8"),
        Channel(7, "CCTV-6 电影", "http://198.204.240.250:82/live/cctv6.m3u8"),
        Channel(8, "CCTV-7 国防军事", "http://74.91.26.218:82/live/cctv7hd.m3u8"),
        Channel(9, "CCTV-8 电视剧", "http://74.91.26.218:82/live/cctv8hd.m3u8"),
        Channel(10, "CCTV-9 纪录", "http://74.91.26.218:82/live/cctv9hd.m3u8"),
        Channel(11, "CCTV-10 科教", "http://74.91.26.218:82/live/cctv10hd.m3u8"),
        Channel(12, "CCTV-11 戏曲", "http://74.91.26.218:82/live/cctv11hd.m3u8"),
        Channel(13, "CCTV-12 社会与法", "http://74.91.26.218:82/live/cctv12hd.m3u8"),
        Channel(14, "CCTV-13 新闻", "http://74.91.26.218:82/live/cctv13hd.m3u8"),
        Channel(15, "CCTV-14 少儿", "http://74.91.26.218:82/live/cctv14hd.m3u8"),
        Channel(16, "CCTV-15 音乐", "http://74.91.26.218:82/live/cctv15hd.m3u8"),
        Channel(17, "CCTV-16 奥林匹克", "http://74.91.26.218:82/live/cctv16hd.m3u8"),
        Channel(18, "CCTV-17 农业农村", "http://74.91.26.218:82/live/cctv17hd.m3u8"),

        // ═══ 卫视 (热门) ═══
        Channel(19, "湖南卫视", "http://live1.hnntv.cn/hnws/sd/live.m3u8"),
        Channel(20, "浙江卫视", "http://39.134.115.163:8080/PLTV/88888910/224/3221225703/index.m3u8"),
        Channel(21, "江苏卫视", "http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226200/index.m3u8"),
        Channel(22, "东方卫视", "http://118.81.195.79:9003/hls/23/index.m3u8"),
        Channel(23, "北京卫视", "http://go.bkpcp.top/mg/bjws"),
        Channel(24, "广东卫视", "http://118.81.195.79:9003/hls/26/index.m3u8"),
        Channel(25, "深圳卫视", "http://118.81.195.79:9003/hls/29/index.m3u8"),
        Channel(26, "安徽卫视", "http://118.81.195.79:9003/hls/21/index.m3u8"),
        Channel(27, "山东卫视", "http://39.134.115.163:8080/PLTV/88888910/224/3221225697/index.m3u8"),
        Channel(28, "天津卫视", "http://39.134.115.163:8080/PLTV/88888910/224/3221225698/index.m3u8"),
        Channel(29, "湖北卫视", "http://118.81.195.79:9003/hls/32/index.m3u8"),
        Channel(30, "辽宁卫视", "http://39.134.39.37/PLTV/88888888/224/3221226209/index.m3u8"),
        Channel(31, "河南卫视", "http://118.81.195.79:9003/hls/30/index.m3u8"),
        Channel(32, "四川卫视", "http://39.134.115.163:8080/PLTV/88888910/224/3221225733/index.m3u8"),
        Channel(33, "江西卫视", "http://39.134.115.163:8080/PLTV/88888910/224/3221225705/index.m3u8"),
        Channel(34, "东南卫视", "http://118.81.195.79:9003/hls/24/index.m3u8"),
        Channel(35, "黑龙江卫视", "http://118.81.195.79:9003/hls/31/index.m3u8"),
        Channel(36, "重庆卫视", "http://118.81.195.79:9003/hls/22/index.m3u8"),
        Channel(37, "河北卫视", "http://118.81.195.79:9003/hls/20/index.m3u8"),
        Channel(38, "吉林卫视", "http://118.81.195.79:9003/hls/34/index.m3u8"),
        Channel(39, "山西卫视", "http://118.81.195.79:9003/hls/19/index.m3u8"),
        Channel(40, "贵州卫视", "http://183.207.248.71/gitv/live1/G_GUIZHOU/G_GUIZHOU"),
        Channel(41, "广西卫视", "http://118.81.195.79:9003/hls/27/index.m3u8"),
        Channel(42, "云南卫视", "https://hwapi.yunshicloud.com/8xughf/e0bx15.m3u8"),
        Channel(43, "海南卫视", "http://118.81.195.79:9003/hls/28/index.m3u8"),
        Channel(44, "甘肃卫视", "http://118.81.195.79:9003/hls/25/index.m3u8"),
        Channel(45, "青海卫视", "http://live.geermurmt.com/qhws/sd/live.m3u8"),
        Channel(46, "宁夏卫视", "http://39.134.115.163:8080/PLTV/88888910/224/3221225726/index.m3u8"),
        Channel(47, "陕西卫视", "http://118.81.195.79:9003/hls/33/index.m3u8"),
        Channel(48, "内蒙古卫视", "http://118.81.195.79:9003/hls/35/index.m3u8"),
        Channel(49, "新疆卫视", "http://live3.hkstv.tv/live/0a1326e03df04d3bb85c62846264b32e.m3u8"),
        Channel(50, "西藏卫视", "http://live.irtv.cn/xzws/sd/live.m3u8"),
    )

    // 分组索引
    val cctvCount = 18
    val weishiStartIndex = 18
}
