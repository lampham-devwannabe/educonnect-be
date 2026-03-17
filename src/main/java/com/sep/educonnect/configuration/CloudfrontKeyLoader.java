package com.sep.educonnect.configuration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CloudfrontKeyLoader {
    public static PrivateKey load() throws Exception {
        InputStream inputStream = CloudfrontKeyLoader.class.getClassLoader()
                .getResourceAsStream("cf-private-key.pem");
        String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        pem = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }
}
