package com.genymobile.scrcpy;

import java.io.FileDescriptor;
import java.io.FileOutputStream;

public class Tools {
    /**
     * 将桌面存储为视频文件
     */
    public static void dumpVideo(Options options) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Ln.e("Exception on thread " + t, e);
        });

        Ln.disableSystemStreams();
        Ln.initLogLevel(options.getLogLevel());

        Device device = new Device(options);
        Workarounds.apply(false, false);

        SurfaceCapture surfaceCapture = new ScreenCapture(device);

        FileDescriptor videoDumpFd = new FileOutputStream(options.getVideoOutputFile()).getFD();
        Streamer videoStreamer = new Streamer(videoDumpFd, options.getVideoCodec(), options.getSendCodecMeta(),
                options.getSendFrameMeta(), options.isDumpBinary());

        SurfaceEncoder surfaceEncoder = new SurfaceEncoder(
                surfaceCapture, videoStreamer, options.getVideoBitRate(), options.getMaxFps(),
                options.getVideoCodecOptions(), options.getVideoEncoder(),
                options.getDownsizeOnError());

        Ln.i("开始读取视频数据");
        surfaceEncoder.start(fatalError -> {
            // do nothing
        });

        Thread.sleep(options.getVideoOutputDuration() * 1000L);
        surfaceEncoder.stop();
        Ln.i("视频数据读取结束");
    }

    /**
     * 存储音频文件
     */
    public static void dumpAudio(Options options) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Ln.e("Exception on thread " + t, e);
        });

        Ln.disableSystemStreams();
        Ln.initLogLevel(options.getLogLevel());
        Workarounds.apply(true, false);

        AudioCodec audioCodec = options.getAudioCodec();
        AudioCapture audioCapture = new AudioCapture(options.getAudioSource());
        FileDescriptor audioDumpFd = new FileOutputStream(options.getAudioOutputFile()).getFD();
        Streamer audioStreamer = new Streamer(audioDumpFd, audioCodec, options.getSendCodecMeta(), options.getSendFrameMeta(), options.isDumpBinary());

        AsyncProcessor audioRecorder;
        if (audioCodec == AudioCodec.RAW) {
            audioRecorder = new AudioRawRecorder(audioCapture, audioStreamer);
        } else {
            audioRecorder = new AudioEncoder(audioCapture, audioStreamer, options.getAudioBitRate(), options.getAudioCodecOptions(),
                    options.getAudioEncoder());
        }

        Ln.i("开始读取音频数据, codec=" + audioCodec);
        audioRecorder.start(fatalError -> {
            // do nothing
        });

        Thread.sleep(options.getAudioOutputDuration() * 1000L);
        audioRecorder.stop();
        Ln.i("音频数据读取结束");
    }
}