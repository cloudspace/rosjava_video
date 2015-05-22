package com.cloudspace.rosjava_video;

/**
 * Created by Ken Kyger on 5/4/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class Constants {

    public static final String NODE_CAMERA_PREVIEW = "ros_camera_preview_view/";
    private static final String NODE_PREFIX_CAMERA = "camera/";
    public static final String NODE_IMAGE_COMPRESSED = NODE_PREFIX_CAMERA + "image/compressed/";
    public static final String NODE_CAMERA_INFO = NODE_PREFIX_CAMERA + "image/info/";

    public static final String CAMERA_FRAME_ID = NODE_PREFIX_CAMERA;
    public static final String IMAGE_FORMAT = "jpeg";

}
