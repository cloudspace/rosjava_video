package com.cloudspace.rosjava_video;

import android.util.Log;

public class VideoConfig {
    public static final int TYPE_TOUCH = 0;
    public static final int TYPE_DETECT_FACE = 1;
    public static final int TYPE_DETECT_VOICE = 2;

    private final String incomingVideoStreamNode, outgoingVideoStreamNode;
    private int outgoingQuality = 20;
    private int currentType = TYPE_TOUCH;


    public VideoConfig(String incomingVideoStreamNode, String outgoingVideoStreamNode) {
        this.incomingVideoStreamNode = incomingVideoStreamNode;
        this.outgoingVideoStreamNode = outgoingVideoStreamNode;
    }

    public int getOutgoingQuality() {
        return outgoingQuality;
    }

    public VideoConfig withOutGoingQuality(int quality) {
        this.outgoingQuality = quality;
        return this;
    }

    public VideoConfig withType(int type) {
        if (type >= 0 && type < 3) {
            this.currentType = type;
        } else {
            Log.wtf("TWO WAY VIDEO", "CONFIG TYPE " + type + " NOT SUPPORTED");
        }
        return this;
    }

    public String getOutgoingVideoStreamNode() {
        return outgoingVideoStreamNode;
    }

    public String getIncomingVideoStreamNode() {
        return incomingVideoStreamNode;
    }

    public int getCurrentType() {
        return currentType;
    }
}