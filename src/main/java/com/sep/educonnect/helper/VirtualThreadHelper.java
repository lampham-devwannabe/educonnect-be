package com.sep.educonnect.helper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VirtualThreadHelper {
    public static void execute(Runnable runnable) {
        Thread.startVirtualThread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Error executing virtual thread: {}", e.getMessage());
            }
        });
    }
}