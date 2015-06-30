package com.cloudspace.rosjava_video;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import org.ros.exception.RosRuntimeException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class CustomCameraPreviewView extends ViewGroup {
    private SurfaceHolder surfaceHolder;
    private Size previewSize;
    private Camera camera;
    private byte[] previewBuffer;
    private CustomRawImageListener rawImageListener;
    private CustomCameraPreviewView.BufferingPreviewCallback bufferingPreviewCallback;

    boolean isPaused = false;

    public void pause() {
        isPaused = true;
        if (camera != null) {
            camera.stopPreview();
        }
    }

    public void play() {
        isPaused = false;
        if (camera != null) {
            camera.startPreview();
        }
    }

    private void init(Context context) {
        SurfaceView surfaceView = new SurfaceView(context);
        this.addView(surfaceView);
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.addCallback(new CustomCameraPreviewView.SurfaceHolderCallback());
        this.surfaceHolder.setType(3);
        this.bufferingPreviewCallback = new CustomCameraPreviewView.BufferingPreviewCallback();
    }

    public CustomCameraPreviewView(Context context) {
        super(context);
        this.init(context);
    }

    public CustomCameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public CustomCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.init(context);
    }

    public void releaseCamera() {
        if (this.camera != null) {
            this.camera.setPreviewCallbackWithBuffer((PreviewCallback) null);
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
        }
    }

    public void setRawImageListener(RawImagePublisher rawImageListener) {
        this.rawImageListener = rawImageListener;
    }

    public Size getPreviewSize() {
        return this.previewSize;
    }

    public void setCamera(Camera camera) {
        Preconditions.checkNotNull(camera);
        this.camera = camera;
        this.setupCameraParameters();
        this.setupBufferingPreviewCallback();
        camera.startPreview();

        try {
            camera.setPreviewDisplay(this.surfaceHolder);
        } catch (IOException var3) {
            throw new RosRuntimeException(var3);
        }
    }

    private void setupCameraParameters() {
        Parameters parameters = this.camera.getParameters();
        List supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        this.previewSize = this.getOptimalPreviewSize(supportedPreviewSizes, this.getWidth(), this.getHeight());
        parameters.setPreviewSize(this.previewSize.width, this.previewSize.height);
        parameters.setPreviewFormat(17);
        this.camera.setParameters(parameters);
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
        Preconditions.checkNotNull(sizes);
        double targetRatio = (double) width / (double) height;
        double minimumDifference = 1.7976931348623157E308D;
        Size optimalSize = null;
        Iterator i$ = sizes.iterator();

        Size size;
        while (i$.hasNext()) {
            size = (Size) i$.next();
            double ratio = (double) size.width / (double) size.height;
            if (Math.abs(ratio - targetRatio) <= 0.1D && (double) Math.abs(size.height - height) < minimumDifference) {
                optimalSize = size;
                minimumDifference = (double) Math.abs(size.height - height);
            }
        }

        if (optimalSize == null) {
            minimumDifference = 1.7976931348623157E308D;
            i$ = sizes.iterator();

            while (i$.hasNext()) {
                size = (Size) i$.next();
                double diff = (double) Math.abs(size.height - height);
                if (diff < minimumDifference) {
                    optimalSize = size;
                    minimumDifference = (double) Math.abs(size.height - height);
                }
            }
        }

        Preconditions.checkNotNull(optimalSize);
        return optimalSize;
    }

    private void setupBufferingPreviewCallback() {
        int format = this.camera.getParameters().getPreviewFormat();
        int bits_per_pixel = ImageFormat.getBitsPerPixel(format);
        this.previewBuffer = new byte[this.previewSize.height * this.previewSize.width * bits_per_pixel / 8];
        this.camera.addCallbackBuffer(this.previewBuffer);
        this.camera.setPreviewCallbackWithBuffer(this.bufferingPreviewCallback);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && this.getChildCount() > 0) {
            View child = this.getChildAt(0);
            int width = r - l;
            int height = b - t;
            int previewWidth = width;
            int previewHeight = height;
            if (this.previewSize != null) {
                previewWidth = this.previewSize.width;
                previewHeight = this.previewSize.height;
            }

            int scaledChildHeight;
            if (width * previewHeight > height * previewWidth) {
                scaledChildHeight = previewWidth * height / previewHeight;
                child.layout((width - scaledChildHeight) / 2, 0, (width + scaledChildHeight) / 2, height);
            } else {
                scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
            }
        }

    }

    private final class SurfaceHolderCallback implements Callback {
        private SurfaceHolderCallback() {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (camera != null) {
                    camera.setPreviewDisplay(holder);
                }
            } catch (IOException var3) {
                throw new RosRuntimeException(var3);
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    }

    private final class BufferingPreviewCallback implements PreviewCallback {
        private BufferingPreviewCallback() {
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
            Preconditions.checkArgument(camera == camera);
            Preconditions.checkArgument(data == previewBuffer);
            if (rawImageListener != null) {
                rawImageListener.onNewRawImage(data, previewSize);
            }

            camera.addCallbackBuffer(previewBuffer);
        }
    }
}
