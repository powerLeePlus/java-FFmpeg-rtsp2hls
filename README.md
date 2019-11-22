# java-FFmpeg-rtsp2hls
利用FFmpeg java lib封装IP摄像头rstp转hls的工具类

1、将  <i>rtsp://{用户名}:{密码}@{IP}:{PORT}/{stream}</i>  格式的IPC视频流通过FFmpeg转化成hls格式。

2、用的就是ffmpeg命令，如：  <i>ffmpeg -i rtsp://admin:@192.168.5.203:554/stream1 -f hls -c copy -hls_time 2.0 -hls_list_size 1 -hls_wrap 15 H:/work/nginx-1.16.0/html/webcam/1/1.m3u8</i>

3、只是将其进行了封装，并支持动态添加删除
