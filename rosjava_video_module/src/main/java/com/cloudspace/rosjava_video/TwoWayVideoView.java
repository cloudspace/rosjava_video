package com.cloudspace.rosjava_video;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ViewSwitcher;

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
public class TwoWayVideoView extends ViewSwitcher implements NodeMain, View.OnTouchListener {

    private boolean started = false;



    public static final int DISPLAYING_OUTGOING = 0;
    public static final int DISPLAYING_INCOMING = 1;
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
        setDisplayedChild(DISPLAYING_INCOMING);
    }

    public void showOutgoing() {
        setDisplayedChild(DISPLAYING_OUTGOING);
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

    private void setCameraDisplayOrientation(Camera camera) {
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
        outgoingView.setQuality(config.outgoingQuality);
        outgoingView.setTopicName(config.getOutgoingVideoStreamNode());

        incomingView = new RosImageView(getContext());
        incomingView.setTopicName(config.getIncomingVideoStreamNode());
        incomingView.setMessageType(CompressedImage._TYPE);
        incomingView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        checkHandler.post(sizeCheckRunnable);
        this.addView(outgoingView, 0);
        this.addView(incomingView, 1);
        setDisplayedChild(0);

        setOnTouchListener(this);
        if (connectedNode != null) {
            onStart(connectedNode);
        }
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("anonymous_" + new Random().nextInt(Integer.MAX_VALUE));
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        if (hasInit && !started) {
            started = true;
            outgoingView.setRawImageListener(new RawImagePublisher(connectedNode, config.outgoingQuality, config.getOutgoingVideoStreamNode()));
            incomingView.onStart(connectedNode);
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
        if (hasInit) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (getDisplayedChild() == DISPLAYING_INCOMING) {
                        setDisplayedChild(DISPLAYING_OUTGOING);
                    } else {
                        setDisplayedChild(DISPLAYING_INCOMING);
                    }
                    break;
            }
        }
        return false;
    }
}
