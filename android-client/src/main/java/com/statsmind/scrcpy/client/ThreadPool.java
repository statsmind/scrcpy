package com.statsmind.scrcpy.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool {
    private static final ExecutorService executorService;

    static {
        executorService = Executors.newCachedThreadPool();
    }

    public static Future<?> startThread(Runnable runnable) {
        return executorService.submit(runnable);
    }
}
