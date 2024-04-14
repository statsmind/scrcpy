package com.statsmind.scrcpy;

import android.graphics.Rect;
import com.genymobile.scrcpy.BuildConfig;

import java.util.List;
import java.util.Locale;

public class Options {

    private Ln.Level logLevel = Ln.Level.DEBUG;
    private int scid = -1; // 31-bit non-negative value, or -1
    private int serverPort = 21708;
    private boolean video = true;
    private boolean audio = true;
    private int maxSize;
    private VideoCodec videoCodec = VideoCodec.H264;
    private AudioCodec audioCodec = AudioCodec.AAC;
    private VideoSource videoSource = VideoSource.DISPLAY;
    private AudioSource audioSource = AudioSource.OUTPUT;
    private int videoBitRate = 8000000;
    private int audioBitRate = 128000;
    private int maxFps = 60;
    private int lockVideoOrientation = -1;
    private boolean tunnelForward = false;
    private Rect crop;
    private boolean control = true;
    private int displayId;
    private String cameraId;
    private Size cameraSize;
    private CameraFacing cameraFacing;
    private CameraAspectRatio cameraAspectRatio;
    private int cameraFps;
    private boolean cameraHighSpeed;
    private boolean showTouches = false;
    private boolean stayAwake = true;
    private List<CodecOption> videoCodecOptions;
    private List<CodecOption> audioCodecOptions;

    private String videoEncoder;
    private String audioEncoder;
    private boolean powerOffScreenOnClose = false;
    private boolean clipboardAutosync = false;
    private boolean downsizeOnError = false;
    private boolean cleanup = true;
    private boolean powerOn = true;

    private boolean listEncoders = false;
    private boolean listDisplays = false;
    private boolean listCameras = false;
    private boolean listCameraSizes = false;

    // Options not used by the scrcpy client, but useful to use scrcpy-server directly
    private boolean sendDeviceMeta = false; // send device name and size
    private boolean sendFrameMeta = false; // send PTS so that the client may record properly
    private boolean sendDummyByte = false; // write a byte on start to detect connection issues
    private boolean sendCodecMeta = false; // write the codec metadata before the stream

    public Ln.Level getLogLevel() {
        return logLevel;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getScid() {
        return scid;
    }

    public boolean getVideo() {
        return video;
    }

    public boolean getAudio() {
        return audio;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public VideoCodec getVideoCodec() {
        return videoCodec;
    }

    public AudioCodec getAudioCodec() {
        return audioCodec;
    }

    public VideoSource getVideoSource() {
        return videoSource;
    }

    public AudioSource getAudioSource() {
        return audioSource;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public int getMaxFps() {
        return maxFps;
    }

    public int getLockVideoOrientation() {
        return lockVideoOrientation;
    }

    public boolean isTunnelForward() {
        return tunnelForward;
    }

    public Rect getCrop() {
        return crop;
    }

    public boolean getControl() {
        return control;
    }

    public int getDisplayId() {
        return displayId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public Size getCameraSize() {
        return cameraSize;
    }

    public CameraFacing getCameraFacing() {
        return cameraFacing;
    }

    public CameraAspectRatio getCameraAspectRatio() {
        return cameraAspectRatio;
    }

    public int getCameraFps() {
        return cameraFps;
    }

    public boolean getCameraHighSpeed() {
        return cameraHighSpeed;
    }

    public boolean getShowTouches() {
        return showTouches;
    }

    public boolean getStayAwake() {
        return stayAwake;
    }

    public List<CodecOption> getVideoCodecOptions() {
        return videoCodecOptions;
    }

    public List<CodecOption> getAudioCodecOptions() {
        return audioCodecOptions;
    }

    public String getVideoEncoder() {
        return videoEncoder;
    }

    public String getAudioEncoder() {
        return audioEncoder;
    }

    public boolean getPowerOffScreenOnClose() {
        return this.powerOffScreenOnClose;
    }

    public boolean getClipboardAutosync() {
        return clipboardAutosync;
    }

    public boolean getDownsizeOnError() {
        return downsizeOnError;
    }

    public boolean getCleanup() {
        return cleanup;
    }

    public boolean getPowerOn() {
        return powerOn;
    }

    public boolean getList() {
        return listEncoders || listDisplays || listCameras || listCameraSizes;
    }

    public boolean getListEncoders() {
        return listEncoders;
    }

    public boolean getListDisplays() {
        return listDisplays;
    }

    public boolean getListCameras() {
        return listCameras;
    }

    public boolean getListCameraSizes() {
        return listCameraSizes;
    }

    public boolean getSendDeviceMeta() {
        return sendDeviceMeta;
    }

    public boolean getSendFrameMeta() {
        return sendFrameMeta;
    }

    public boolean getSendDummyByte() {
        return sendDummyByte;
    }

    public boolean getSendCodecMeta() {
        return sendCodecMeta;
    }

    @SuppressWarnings("MethodLength")
    public static Options parse(String... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing client version");
        }

        String clientVersion = args[0];
        if (!clientVersion.equals(BuildConfig.VERSION_NAME)) {
            throw new IllegalArgumentException(
                    "The server version (" + BuildConfig.VERSION_NAME + ") does not match the client " + "(" + clientVersion + ")");
        }

        Options options = new Options();

        for (int i = 1; i < args.length; ++i) {
            String arg = args[i];
            int equalIndex = arg.indexOf('=');
            if (equalIndex == -1) {
                throw new IllegalArgumentException("Invalid key=value pair: \"" + arg + "\"");
            }
            String key = arg.substring(0, equalIndex);
            String value = arg.substring(equalIndex + 1);
            switch (key) {
                case "scid":
                    int scid = Integer.parseInt(value, 0x10);
                    if (scid < -1) {
                        throw new IllegalArgumentException("scid may not be negative (except -1 for 'none'): " + scid);
                    }
                    options.scid = scid;
                    break;
                case "log_level":
                    options.logLevel = Ln.Level.valueOf(value.toUpperCase(Locale.ENGLISH));
                    break;
                case "server_port":
                    options.serverPort = Integer.parseInt(value);
                    break;
                case "video":
                    options.video = Boolean.parseBoolean(value);
                    break;
                case "audio":
                    options.audio = Boolean.parseBoolean(value);
                    break;
                case "video_codec":
                    VideoCodec videoCodec = VideoCodec.findByName(value);
                    if (videoCodec == null) {
                        throw new IllegalArgumentException("Video codec " + value + " not supported");
                    }
                    options.videoCodec = videoCodec;
                    break;
                case "audio_codec":
                    AudioCodec audioCodec = AudioCodec.findByName(value);
                    if (audioCodec == null) {
                        throw new IllegalArgumentException("Audio codec " + value + " not supported");
                    }
                    options.audioCodec = audioCodec;
                    break;
                case "video_source":
                    VideoSource videoSource = VideoSource.findByName(value);
                    if (videoSource == null) {
                        throw new IllegalArgumentException("Video source " + value + " not supported");
                    }
                    options.videoSource = videoSource;
                    break;
                case "audio_source":
                    AudioSource audioSource = AudioSource.findByName(value);
                    if (audioSource == null) {
                        throw new IllegalArgumentException("Audio source " + value + " not supported");
                    }
                    options.audioSource = audioSource;
                    break;
                case "max_size":
                    options.maxSize = Integer.parseInt(value) & ~7; // multiple of 8
                    break;
                case "video_bit_rate":
                    options.videoBitRate = Integer.parseInt(value);
                    break;
                case "audio_bit_rate":
                    options.audioBitRate = Integer.parseInt(value);
                    break;
                case "max_fps":
                    options.maxFps = Integer.parseInt(value);
                    break;
                case "lock_video_orientation":
                    options.lockVideoOrientation = Integer.parseInt(value);
                    break;
                case "tunnel_forward":
                    options.tunnelForward = Boolean.parseBoolean(value);
                    break;
                case "crop":
                    if (!value.isEmpty()) {
                        options.crop = parseCrop(value);
                    }
                    break;
                case "control":
                    options.control = Boolean.parseBoolean(value);
                    break;
                case "display_id":
                    options.displayId = Integer.parseInt(value);
                    break;
                case "show_touches":
                    options.showTouches = Boolean.parseBoolean(value);
                    break;
                case "stay_awake":
                    options.stayAwake = Boolean.parseBoolean(value);
                    break;
                case "video_codec_options":
                    options.videoCodecOptions = CodecOption.parse(value);
                    break;
                case "audio_codec_options":
                    options.audioCodecOptions = CodecOption.parse(value);
                    break;
                case "video_encoder":
                    if (!value.isEmpty()) {
                        options.videoEncoder = value;
                    }
                    break;
                case "audio_encoder":
                    if (!value.isEmpty()) {
                        options.audioEncoder = value;
                    }
                case "power_off_on_close":
                    options.powerOffScreenOnClose = Boolean.parseBoolean(value);
                    break;
                case "clipboard_autosync":
                    options.clipboardAutosync = Boolean.parseBoolean(value);
                    break;
                case "downsize_on_error":
                    options.downsizeOnError = Boolean.parseBoolean(value);
                    break;
                case "cleanup":
                    options.cleanup = Boolean.parseBoolean(value);
                    break;
                case "power_on":
                    options.powerOn = Boolean.parseBoolean(value);
                    break;
                case "list_encoders":
                    options.listEncoders = Boolean.parseBoolean(value);
                    break;
                case "list_displays":
                    options.listDisplays = Boolean.parseBoolean(value);
                    break;
                case "list_cameras":
                    options.listCameras = Boolean.parseBoolean(value);
                    break;
                case "list_camera_sizes":
                    options.listCameraSizes = Boolean.parseBoolean(value);
                    break;
                case "camera_id":
                    if (!value.isEmpty()) {
                        options.cameraId = value;
                    }
                    break;
                case "camera_size":
                    if (!value.isEmpty()) {
                        options.cameraSize = parseSize(value);
                    }
                    break;
                case "camera_facing":
                    if (!value.isEmpty()) {
                        CameraFacing facing = CameraFacing.findByName(value);
                        if (facing == null) {
                            throw new IllegalArgumentException("Camera facing " + value + " not supported");
                        }
                        options.cameraFacing = facing;
                    }
                    break;
                case "camera_ar":
                    if (!value.isEmpty()) {
                        options.cameraAspectRatio = parseCameraAspectRatio(value);
                    }
                    break;
                case "camera_fps":
                    options.cameraFps = Integer.parseInt(value);
                    break;
                case "camera_high_speed":
                    options.cameraHighSpeed = Boolean.parseBoolean(value);
                    break;
                case "send_device_meta":
                    options.sendDeviceMeta = Boolean.parseBoolean(value);
                    break;
                case "send_frame_meta":
                    options.sendFrameMeta = Boolean.parseBoolean(value);
                    break;
                case "send_dummy_byte":
                    options.sendDummyByte = Boolean.parseBoolean(value);
                    break;
                case "send_codec_meta":
                    options.sendCodecMeta = Boolean.parseBoolean(value);
                    break;
                case "raw_stream":
                    boolean rawStream = Boolean.parseBoolean(value);
                    if (rawStream) {
                        options.sendDeviceMeta = false;
                        options.sendFrameMeta = false;
                        options.sendDummyByte = false;
                        options.sendCodecMeta = false;
                    }
                    break;
                default:
                    Ln.w("Unknown server option: " + key);
                    break;
            }
        }

        return options;
    }

    private static Rect parseCrop(String crop) {
        // input format: "width:height:x:y"
        String[] tokens = crop.split(":");
        if (tokens.length != 4) {
            throw new IllegalArgumentException("Crop must contains 4 values separated by colons: \"" + crop + "\"");
        }
        int width = Integer.parseInt(tokens[0]);
        int height = Integer.parseInt(tokens[1]);
        int x = Integer.parseInt(tokens[2]);
        int y = Integer.parseInt(tokens[3]);
        return new Rect(x, y, x + width, y + height);
    }

    private static Size parseSize(String size) {
        // input format: "<width>x<height>"
        String[] tokens = size.split("x");
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid size format (expected <width>x<height>): \"" + size + "\"");
        }
        int width = Integer.parseInt(tokens[0]);
        int height = Integer.parseInt(tokens[1]);
        return new Size(width, height);
    }

    private static CameraAspectRatio parseCameraAspectRatio(String ar) {
        if ("sensor".equals(ar)) {
            return CameraAspectRatio.sensorAspectRatio();
        }

        String[] tokens = ar.split(":");
        if (tokens.length == 2) {
            int w = Integer.parseInt(tokens[0]);
            int h = Integer.parseInt(tokens[1]);
            return CameraAspectRatio.fromFraction(w, h);
        }

        float floatAr = Float.parseFloat(tokens[0]);
        return CameraAspectRatio.fromFloat(floatAr);
    }

    @Override
    public String toString() {
        return "Options{" +
                "logLevel=" + logLevel +
                ", scid=" + scid +
                ", video=" + video +
                ", audio=" + audio +
                ", maxSize=" + maxSize +
                ", videoCodec=" + videoCodec +
                ", audioCodec=" + audioCodec +
                ", videoSource=" + videoSource +
                ", audioSource=" + audioSource +
                ", videoBitRate=" + videoBitRate +
                ", audioBitRate=" + audioBitRate +
                ", maxFps=" + maxFps +
                ", lockVideoOrientation=" + lockVideoOrientation +
                ", tunnelForward=" + tunnelForward +
                ", crop=" + crop +
                ", control=" + control +
                ", displayId=" + displayId +
                ", cameraId='" + cameraId + '\'' +
                ", cameraSize=" + cameraSize +
                ", cameraFacing=" + cameraFacing +
                ", cameraAspectRatio=" + cameraAspectRatio +
                ", cameraFps=" + cameraFps +
                ", cameraHighSpeed=" + cameraHighSpeed +
                ", showTouches=" + showTouches +
                ", stayAwake=" + stayAwake +
                ", videoCodecOptions=" + videoCodecOptions +
                ", audioCodecOptions=" + audioCodecOptions +
                ", videoEncoder='" + videoEncoder + '\'' +
                ", audioEncoder='" + audioEncoder + '\'' +
                ", powerOffScreenOnClose=" + powerOffScreenOnClose +
                ", clipboardAutosync=" + clipboardAutosync +
                ", downsizeOnError=" + downsizeOnError +
                ", cleanup=" + cleanup +
                ", powerOn=" + powerOn +
                ", listEncoders=" + listEncoders +
                ", listDisplays=" + listDisplays +
                ", listCameras=" + listCameras +
                ", listCameraSizes=" + listCameraSizes +
                ", sendDeviceMeta=" + sendDeviceMeta +
                ", sendFrameMeta=" + sendFrameMeta +
                ", sendDummyByte=" + sendDummyByte +
                ", sendCodecMeta=" + sendCodecMeta +
                '}';
    }
}
