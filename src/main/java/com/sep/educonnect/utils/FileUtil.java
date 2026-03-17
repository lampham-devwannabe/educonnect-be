package com.sep.educonnect.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class FileUtil {

    private FileUtil() {
    }

    public static File decodeBase64ToFile(String base64Content, String filePath) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        Path path = Paths.get(filePath);
        Files.write(path, decodedBytes);
        return path.toFile();
    }
}