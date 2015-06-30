package com.cloudspace.rosjava_video;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.Random;

import sensor_msgs.CompressedImage;

/**
 * Created by Ken Kyger on 5/21/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class TwoWayVideoView extends ViewFlipper implements NodeMain, View.OnTouchListener {
    private static int RECORDER_SAMPLERATE = 8000;
    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private boolean started = false;

    public static final int DISPLAYING_NOTHING = 0;
    public static final int DISPLAYING_OUTGOING = 1;
    public static final int DISPLAYING_INCOMING = 2;
    boolean didShowConfigError = false;
    long firstChecked = -1;

    CustomRosCameraPreviewView outgoingView;
    RosImageView incomingView;

    VideoConfig config;

    boolean hasInit = false;
    ConnectedNode connectedNode;

    public VideoConfig getConfig() {
        return config;
    }

    public void setConfig(VideoConfig config) {
        this.config = config;
    }

    public void showIncoming() {
        outgoingView.pause();
//        incomingView.play();
        setDisplayedChild(DISPLAYING_INCOMING);
    }

    public void showOutgoing() {
        outgoingView.play();
//        incomingView.pause();
        setDisplayedChild(DISPLAYING_OUTGOING);
    }

    public void onTriggerForType(final int trigger) {
        if (trigger == getConfig().getCurrentType()) {
            post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "TRIGGGER  " + trigger + " ACTIVATED", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void showNext() {
        super.showNext();
        throw new IllegalAccessError("Use showOutgoing or showIncoming directly");
    }

    @Override
    public void showPrevious() {
        super.showPrevious();
        throw new IllegalAccessError("Use showOutgoing or showIncoming directly");
    }

    Handler checkHandler = new Handler();
    Runnable sizeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (outgoingView.height == -1 || outgoingView.width == -1) {
                checkHandler.postDelayed(this, 100);
            } else {
                Camera camera = Camera.open(0);
                setCameraDisplayOrientation(camera);
                outgoingView.setCamera(camera);
            }
        }
    };

    Runnable configCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (firstChecked == -1) {
                firstChecked = System.currentTimeMillis();
            }
            if (config == null) {
                if (System.currentTimeMillis() - firstChecked > 1000 && !didShowConfigError) {
                    didShowConfigError = true;
                    Log.wtf("TWO WAY VIEW", "CONFIG NOT SET");
                }
                checkHandler.postDelayed(this, 100);
            } else {
                hasInit = true;
                continueInit();
            }
        }
    };

    Camera.FaceDetectionListener faceDetector = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                onTriggerForType(VideoConfig.TYPE_DETECT_FACE);
            }
        }
    };

    private void setCameraDisplayOrientation(Camera camera) {
        if (config.getCurrentType() == VideoConfig.TYPE_DETECT_FACE) {
            camera.startFaceDetection();
            camera.setFaceDetectionListener(faceDetector);
        }
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        int rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        camera.setDisplayOrientation((info.orientation - degrees + 360) % 360);
        incomingView.setRotation((info.orientation - degrees + 360) % 360);
    }

    public TwoWayVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TwoWayVideoView(Context context) {
        super(context);
        init();
    }

    private void init() {
        checkHandler.post(configCheckRunnable);
    }

    private void continueInit() {
        outgoingView = new CustomRosCameraPreviewView(getContext());
        outgoingView.setQuality(config.getOutgoingQuality());
        outgoingView.setTopicName(config.getOutgoingVideoStreamNode());

        incomingView = new RosImageView(getContext());
        incomingView.setTopicName(config.getIncomingVideoStreamNode());
        incomingView.setMessageType(CompressedImage._TYPE);
        incomingView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        checkHandler.post(sizeCheckRunnable);
        addView(generateLoadingView());
        addView(outgoingView, DISPLAYING_OUTGOING);
        addView(incomingView, DISPLAYING_INCOMING);
        setDisplayedChild(DISPLAYING_NOTHING);

        setOnTouchListener(this);
        if (connectedNode != null) {
            onStart(connectedNode);
        }

        if (config.getCurrentType() == VideoConfig.TYPE_DETECT_VOICE) {
            startAudioWatchingThread();
        }
    }

    private View generateLoadingView() {
        RelativeLayout layout = new RelativeLayout(getContext());
        ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);

        return layout;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("anonymous_" + new Random().nextInt(Integer.MAX_VALUE));
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (hasInit && !started) {
            started = true;
            outgoingView.setRawImageListener(new RawImagePublisher(connectedNode, config.getOutgoingQuality(), config.getOutgoingVideoStreamNode()));
            incomingView.onStart(connectedNode);
            post(new Runnable() {
                @Override
                public void run() {
                    setDisplayedChild(DISPLAYING_INCOMING);
                }
            });
        } else {
            this.connectedNode = connectedNode;
        }
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        if (hasInit && config.getCurrentType() == VideoConfig.TYPE_TOUCH) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onTriggerForType(VideoConfig.TYPE_TOUCH);
                    break;
            }
        }
        return false;
    }

    private void startAudioWatchingThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get the minimum buffer size required for the successful creation of an AudioRecord object.
                int bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                        RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING
                );
                // Initialize Audio Recorder.
                AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLERATE,
                        RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING,
                        bufferSizeInBytes
                );
                // Start Recording.
                audioRecorder.startRecording();

                int numberOfReadBytes = 0;
                byte audioBuffer[] = new byte[bufferSizeInBytes];
                float tempFloatBuffer[] = new float[3];
                int tempIndex = 0;
                int totalReadBytes = 0;
                byte totalByteBuffer[] = new byte[60 * 44100 * 2];
                // While data come from microphone.
                while (true) {
                    float totalAbsValue = 0.0f;
                    short sample = 0;

                    numberOfReadBytes = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes);

                    // Analyze Sound.
                    for (int i = 0; i < bufferSizeInBytes; i += 2) {
                        sample = (short) ((audioBuffer[i]) | audioBuffer[i + 1] << 8);
                        totalAbsValue += Math.abs(sample) / (numberOfReadBytes / 2);
                    }

                    // Analyze temp buffer.
                    tempFloatBuffer[tempIndex % 3] = totalAbsValue;
                    float temp = 0.0f;
                    for (int i = 0; i < 3; ++i)
                        temp += tempFloatBuffer[i];

                    if ((temp >= 0 && temp <= 350)) {
                        tempIndex++;
                        continue;
                    }

                    if (temp > 350) {
                        onTriggerForType(VideoConfig.TYPE_DETECT_VOICE);
                    }

                    if ((temp >= 0 && temp <= 350)) {
                        tempIndex++;
                        break;
                    }

                    for (int i = 0; i < numberOfReadBytes; i++)
                        totalByteBuffer[totalReadBytes + i] = audioBuffer[i];
                    totalReadBytes += numberOfReadBytes;

                    tempIndex++;

                }
            }
        }).start();

    }

}
