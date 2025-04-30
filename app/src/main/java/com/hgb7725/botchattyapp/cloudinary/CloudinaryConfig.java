package com.hgb7725.botchattyapp.cloudinary;

public class CloudinaryConfig {
//    private static final String CLOUD_NAME = "doli3j7da";
//    private static final String API_KEY = "555281845151576";
//    private static final String API_SECRET = "dWNajte0C13bxuIJwom14Bj2mfo";
//
//    public static Map<String, String> getConfig() {
//        Map<String, String> config = new HashMap<>();
//        config.put("cloud_name", CLOUD_NAME);
//        config.put("api_key", API_KEY);
//        config.put("api_secret", API_SECRET);
//        return config;
//    }

    public static final String CLOUD_NAME = "doli3j7da";
    public static final String PRESET_IMAGE = "chat-image-upload";
    public static final String PRESET_VIDEO = "chat-video-upload";
    public static final String PRESET_AUDIO = "chat-audio-upload";
    public static final String PRESET_FILE = "chat-file-upload";

    public static String getUploadUrl(String resourceType) {
        return "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/" + resourceType + "/upload";
    }
} 