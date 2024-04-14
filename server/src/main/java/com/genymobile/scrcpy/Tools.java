package com.genymobile.scrcpy;

import java.io.FileDescriptor;
import java.io.FileOutputStream;

public class Tools {
    /**
     * 将桌面存储为视频文件
     */
    public static void dumpVideo(Options options) throws Exception {
        Device device = new Device(options);
        Workarounds.apply(false, true);

        SurfaceCapture surfaceCapture = new ScreenCapture(device);

        FileDescriptor videoDumpFd = new FileOutputStream(options.getVideoOutputFile()).getFD();
        Streamer videoStreamer = new Streamer(videoDumpFd, options.getVideoCodec(), options.getSendCodecMeta(),
                options.getSendFrameMeta());

        SurfaceEncoder surfaceEncoder = new SurfaceEncoder(
                surfaceCapture, videoStreamer, options.getVideoBitRate(), options.getMaxFps(),
                options.getVideoCodecOptions(), options.getVideoEncoder(),
                options.getDownsizeOnError());

        Ln.i("开始读取视频信息");
        surfaceEncoder.start(fatalError -> {
            // do nothing
        });

        Thread.sleep(options.getVideoDuration() * 1000);
        surfaceEncoder.stop();
        Ln.i("视频读取结束");
    }

    /**
     * 存储音频文件
     */
    public static void dumpAudio(Options options) {
    }
}