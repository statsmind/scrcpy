package com.statsmind.scrcpy.websocket;

import com.genymobile.scrcpy.ConfigurationException;
import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.Ln;
import com.genymobile.scrcpy.LogUtils;
import com.genymobile.scrcpy.Options;
import com.genymobile.scrcpy.ScreenCapture;
import com.genymobile.scrcpy.Streamer;
import com.genymobile.scrcpy.SurfaceCapture;
import com.genymobile.scrcpy.SurfaceEncoder;
import com.genymobile.scrcpy.Workarounds;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public class DumpVideo {
    public static void main(String[] args) {
        int status = 0;
        try {
            internalMain(args);
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
            status = 1;
        } finally {
            System.exit(status);
        }
    }


    private static void internalMain(String[] args) throws ConfigurationException, IOException, InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Ln.e("Exception on thread " + t, e);
        });

        Options options = Options.parse(args);
        Ln.disableSystemStreams();
        Ln.initLogLevel(options.getLogLevel());

        Ln.i(LogUtils.buildVideoEncoderListMessage());
        Ln.i(LogUtils.buildAudioEncoderListMessage());
        Ln.i(LogUtils.buildDisplayListMessage());

        Workarounds.apply(false, true);
        Ln.i(LogUtils.buildCameraListMessage(options.getListCameraSizes()));

        Device device = new Device(options);
        Workarounds.apply(false, true);

        SurfaceCapture surfaceCapture = new ScreenCapture(device);

        FileDescriptor videoDumpFd = new FileOutputStream("/data/local/tmp/record.h264").getFD();
        Streamer videoStreamer = new Streamer(videoDumpFd, options.getVideoCodec(), options.getSendCodecMeta(),
                options.getSendFrameMeta());

        SurfaceEncoder surfaceEncoder = new SurfaceEncoder(
                surfaceCapture, videoStreamer, options.getVideoBitRate(), options.getMaxFps(),
                options.getVideoCodecOptions(), options.getVideoEncoder(),
                options.getDownsizeOnError());

        surfaceEncoder.start(fatalError -> {
            // do nothing
        });

        Thread.sleep(30000);
        surfaceEncoder.stop();
    }
}
