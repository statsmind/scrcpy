package com.statsmind.scrcpy.client;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

import static android.media.AudioManager.AUDIO_SESSION_ID_GENERATE;
import static com.statsmind.scrcpy.Consts.TAG;


public class MainActivity extends ComponentActivity implements SurfaceHolder.Callback {


    private SurfaceView surfaceView;
    private AudioTrack audioTrack;

    private CodecDecoder codecDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface);
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        surfaceView.getHolder().setFixedSize(720, 1280);
        surfaceView.getHolder().addCallback(this);

        int bufSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        // 实例化AudioTrack(设置缓冲区为最小缓冲区的2倍，至少要等于最小缓冲区)
         AudioAttributes audioAttributes = new AudioAttributes.Builder()
                 .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                 .build();
         AudioFormat audioFormat = new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(48000)
                    .build();
         audioTrack = new AudioTrack(audioAttributes, audioFormat, bufSize*2,
                 AudioTrack.MODE_STREAM, AUDIO_SESSION_ID_GENERATE);
        // 设置音量
        audioTrack.setVolume(2f) ;
        // 设置播放频率
        audioTrack.setPlaybackRate(10) ;
        audioTrack.play();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "MainActivity::surfaceCreated");
        this.codecDecoder = new CodecDecoder(this, surfaceView, holder.getSurface(), audioTrack);
        this.codecDecoder.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "MainActivity::surfaceChanged");
        this.codecDecoder.setOutputSurface(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "MainActivity::surfaceDestroyed");
        this.codecDecoder.close();
    }
}