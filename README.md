# java-FFmpeg-rtsp2hls
利用FFmpeg java lib封装IP摄像头rstp转hls的工具类

1、将  <i>rtsp://{用户名}:{密码}@{IP}:{PORT}/{stream}</i>  格式的IPC视频流通过FFmpeg转化成hls格式。

2、用的就是ffmpeg命令，如：  <i>ffmpeg -i rtsp://admin:@192.168.5.203:554/stream1 -f hls -c copy -hls_time 2.0 -hls_list_size 1 -hls_wrap 15 H:/work/nginx-1.16.0/html/webcam/1/1.m3u8</i>

3、只是将其进行了封装，并支持动态添加删除

# 个人博客
csdn: [https://blog.csdn.net/wenqiangluyao][https://blog.csdn.net/wenqiangluyao]

# 微信公众号
欢迎关注，一起成长

![微信公众号二维码](https://user-images.githubusercontent.com/25865085/206827213-25d1093e-df3d-4ce9-81f6-604288c2746a.png)

[https://blog.csdn.net/wenqiangluyao]: https://blog.csdn.net/wenqiangluyao
