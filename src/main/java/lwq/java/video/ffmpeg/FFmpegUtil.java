package lwq.java.video.ffmpeg;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.RunProcessFunction;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * FFmpeg 视频转码工具类
 * @author lwq
 * @create 2019-07-31 下午 4:55
 */
public class FFmpegUtil {
    private static Map<String, Process> progressMap = new ConcurrentHashMap<>();
    private static ExecutorService executorService = null;
    static {
        executorService = new ThreadPoolExecutor(8, 8,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public static void main(String[] args) throws IOException {
        String ffmpegPath = "H:\\work\\ffmpeg-20190730-b3b7523-win64-static\\bin\\ffmpeg.exe";
        String ffprobePath = "H:\\work\\ffmpeg-20190730-b3b7523-win64-static\\bin\\ffprobe.exe";
        String input = "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov";
        String output = "H:/work/nginx-1.16.0/html/hls/test.m3u8";
        toHls(ffmpegPath,ffprobePath,input,output, "1");

    }

    public static void toHls(String input, String output, String deviceId) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        String ffmpegPath = "ffmpeg";
        String ffprobePath = "ffprobe";
        if(File.separator.equals("\\")){
            ffmpegPath = "H:\\work\\ffmpeg-20190730-b3b7523-win64-static\\bin\\ffmpeg.exe";
            ffprobePath = "H:\\work\\ffmpeg-20190730-b3b7523-win64-static\\bin\\ffprobe.exe";
        }
        asyncExecuteToHls(ffmpegPath, ffprobePath, input, output, deviceId);
    }

    public static int terminateFfmpeg(final String deviceId){
        Process process = progressMap.get(deviceId);
        if(process != null){
            progressMap.remove(deviceId);
            return terminateFfmpeg(process);
        }
        return 0;
    }

    /**
     * @return the exit code of the terminated process as an {@code unsigned
     *         byte} (0..255 range), or -1 if the current thread has been
     *         interrupted.
     */
    static int terminateFfmpeg(final Process process) {
        System.out.println("进了terminateFfmpeg方法");
        if (!process.isAlive()) {
			/*
			 * ffmpeg -version, do nothing
			 */
            return process.exitValue();
        }

		/*
		 * ffmpeg -f x11grab
		 */
        System.out.println("About to destroy the child process...");
        try (final OutputStreamWriter out = new OutputStreamWriter(process.getOutputStream(), Charset.forName("UTF-8"))) {
            out.write('q');
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }
        try {
            if (!process.waitFor(5L, TimeUnit.SECONDS)) {
                process.destroy();
                process.waitFor();
            }
            return process.exitValue();
        } catch (final InterruptedException ie) {
            System.out.println("Interrupted");
            ie.printStackTrace();
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    static boolean toHls(String ffmpegPath, String ffprobePath, String input, String output, String deviceId) throws IOException {
        Process process = progressMap.get(deviceId);
        if(process != null){
            boolean alive = process.isAlive();
            if(alive){
                System.out.println("has exists process");
                return true;
            }else {
                process.destroy();
                progressMap.remove(deviceId);
            }
        }
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath, new RunProcessFunction() {
            @Override
            public Process run(final List<String> args) throws IOException {
                final Process process = super.run(args);

                if (!(args.size() == 2 && args.get(1).equals("-version"))) {
					/*
					 * Remember all child processes except "ffmpeg -version"
					 */
                    progressMap.put(deviceId, process);
                }
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("执行：Runtime.getRuntime().addShutdownHook");
                    terminateFfmpeg(process);
                }, "FFmpeg process destroyer"));


                /*try {
                    Thread.sleep(50000);
                    System.out.println("sleep 50s");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(-1);*/

                return process;
            }
        });
        FFprobe ffprobe = new FFprobe(ffprobePath);
        //如果没有目录就创建
        File file = new File(output.substring(0, output.lastIndexOf("/")));
        if(! file.exists()){
            file.mkdirs();
        }
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(input)     // Filename, or a FFmpegProbeResult
                //.overrideOutputFiles(true) // Override the output if it exists
                .addOutput(output)   // Filename for the destination
                .setFormat("hls")        // Format is inferred from filename, or can be set
                // 自定义参数
                .addExtraArgs("-c", "copy")
                .addExtraArgs("-hls_time", "2.0")
                .addExtraArgs("-hls_list_size", "1")
                .addExtraArgs("-hls_wrap", "15")

                //.setTargetSize(250_000)  // Aim for a 250KB file
                //.disableSubtitle()       // No subtiles

                //.setAudioChannels(1)         // Mono audio
                //.setAudioCodec("aac")        // using the aac codec
                //.setAudioSampleRate(48_000)  // at 48KHz
                //.setAudioBitRate(32768)      // at 32 kbit/s

                //.setVideoCodec("libx264")     // Video using x264
                // .setVideoFrameRate(24, 1)     // at 24 frames per second
                //.setVideoResolution(640, 480) // at 640x480 resolution

                //.setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        // Run a one-pass encode
        executor.createJob(builder
          /*  , new ProgressListener() {
                @Override
                public void progress(Progress progress) {
                    //boolean end = progress.isEnd();
                    //System.out.println("progress status:" + end);
                }
            }*/
        ).run();

        return true;


        // Or run a two-pass encode (which is better quality at the cost of being slower)
        //executor.createTwoPassJob(builder).run();
    }

    public static void rtspToHls(String username, String password, String ip, String port, String stream, String
            outputPathBase, String deviceId) throws IOException {
        rtspToHls("ffmpeg", "ffprobe", username, password, ip, port, stream, outputPathBase, deviceId);
    }

    public static void rtspToHls(String ffmpegPath, String ffprobePath, String username, String password, String ip, String port, String stream, String
            outputPathBase, String deviceId) throws IOException {
        String input = "rtsp://";
        if(!StringUtils.isNotBlank(username) && !StringUtils.isNotBlank(password)){
            input += username + ":" + password;
        }
        if(StringUtils.isBlank(ip)){
            throw new IllegalArgumentException("ip不能为空");
        }
        input += "@" + ip + ":";
        if(StringUtils.isNotBlank(port)){
            input += password;
        }else {
            input += "554" + "/";
        }
        if(StringUtils.isNotBlank(stream)){
            input += stream;
        }else {
            input += "stream1";
        }
        String output = "";
        if(StringUtils.isNotBlank(outputPathBase)){
            output += outputPathBase;
        }else {
            output += "/usr/local/nginx/html/hls/";
        }
        if(StringUtils.isBlank(deviceId)){
            throw new IllegalArgumentException("摄像头deviceId不能为空");
        }
        output += deviceId + File.separator + "webcam" + deviceId.substring(0, 5) + ".m3u8";

        toHls(ffmpegPath, ffprobePath, input, output, deviceId);

    }

    public static void asyncExecuteToHls(String ffmpegPath, String ffprobePath, String input, String output, String deviceId) throws InterruptedException, ExecutionException {
        CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return toHls(ffmpegPath, ffprobePath, input, output, deviceId);
            } catch (IOException e) {
                progressMap.remove(deviceId);
                e.printStackTrace();
                return false;
            }

        }, executorService);
        System.out.println("after completableFuture run");
        Thread.sleep(1000L);
        boolean isSuccess = true;
        Process process = progressMap.get(deviceId);
        try {
            isSuccess = completableFuture.get(2, TimeUnit.SECONDS);
        } catch (RuntimeException | ExecutionException e){
            e.printStackTrace();
            process = progressMap.get(deviceId);
            StringBuilder stringBuilder = new StringBuilder();
            StringBuilder stringBuilder1 = new StringBuilder();

            try(
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(process.getInputStream()));
            ) {
                String lineErr;
                while ((lineErr = bufferedReader.readLine()) != null) {
                    stringBuilder.append(lineErr).append("\n");
                }
                System.out.println("process.getErrorStream() - " + stringBuilder.toString());

                String lineErr1;
                while ((lineErr1 = bufferedReader1.readLine()) != null) {
                    stringBuilder1.append(lineErr1).append("\n");
                }
                System.out.println("process.getInputStream() - " + stringBuilder1.toString());
            }catch (IOException ioe){
                ioe.printStackTrace();
            }

            throw new RuntimeException("转流失败");
        } catch (TimeoutException te){
        }

        if(isSuccess && (progressMap.containsKey(deviceId) && process.isAlive())){
            System.out.println("转流进程启动成功");
            System.out.println("当前转流线程:" + deviceId + "-" + progressMap.get(deviceId));
        }else {
            throw new RuntimeException("转流失败");
        }

        System.out.println("转流线路总数：" + progressMap.size());

        //Integer integer = completableFuture.get();
        //System.out.println("completableFuture:" + integer);
    }
}

