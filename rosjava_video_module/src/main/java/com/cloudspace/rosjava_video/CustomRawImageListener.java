package com.cloudspace.rosjava_video;

import android.hardware.Camera;

public interface CustomRawImageListener {
    void onNewRawImage(byte[] var1, Camera.Size var2);
}