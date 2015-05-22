package com.cloudspace.rosjava_video;

public class VideoConfig {
    private final String incomingVideoStreamNode, outgoingVideoStreamNode;
    int outgoingQuality = 20;

    public VideoConfig(String incomingVideoStreamNode, String outgoingVideoStreamNode) {
        this.incomingVideoStreamNode = incomingVideoStreamNode;
        this.outgoingVideoStreamNode = outgoingVideoStreamNode;
    }

    public VideoConfig withOutGoingQuality(int quality) {
        this.outgoingQuality = quality;
        return this;
    }

    public String getOutgoingVideoStreamNode() {
        return outgoingVideoStreamNode;
    }

    public String getIncomingVideoStreamNode() {
        return incomingVideoStreamNode;
    }
}