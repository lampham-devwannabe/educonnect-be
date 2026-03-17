package com.sep.educonnect.enums;

import lombok.Getter;

@Getter
public enum UploadResourceType {
    IMAGE("image_preset", "images"),
    PUBLIC_VIDEOS("public_videos_preset", "videos"),
    PRIVATE_VIDEOS("private_videos_preset", "p-videos"),
    FILE("file_preset", "files");
    private String preset;
    private String folder;

    UploadResourceType(String preset, String folder){
        this.preset = preset;
        this.folder = folder;
    }

};
