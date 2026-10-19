package com.example;

import java.util.List;
import java.util.concurrent.Executors;

public class App {

    // A sealed interface to categorize our log outputs cleanly
    sealed interface LogResult permits InfoResult, ErrorResult {}
    record InfoResult(String message) implements LogResult {}
    record ErrorResult(String message, int code) implements LogResult {}

    public static void main(String[] args) {
        System.out.println("🚀 Starting Log Analyzer using OpenJDK " + System.getProperty("java.version"));

        List<String> rawLogs = List.of(
            "INFO: User logged in successfully",
            "ERROR: 404 - Page not found",
            "INFO: Database connection established",
            "ERROR: 500 - Internal Server Error"
        );

        // Using Java 21 Virtual Threads to process each log line concurrently
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String log : rawLogs) {
                executor.submit(() -> {
                    LogResult result = parseLog(log);
                    processResult(result);
                });
            }
        } // The executor auto-closes here, waiting for all virtual threads to finish

        System.out.println("✅ Analysis finished successfully!");
    }

    private static LogResult parseLog(String log) {
        if (log.startsWith("INFO:")) {
            return new InfoResult(log.substring(6));
        } else if (log.startsWith("ERROR:")) {
            String[] parts = log.substring(7).split(" - ", 2);
            if (parts.length == 2) {
                try {
                    int code = Integer.parseInt(parts[0].trim());
                    return new ErrorResult(parts[1].trim(), code);
                } catch (NumberFormatException e) {
                    return new ErrorResult(log.substring(7), 0);
                }
            }
        }
        return new InfoResult("Unknown format: " + log);
    }

    // Java 21 Pattern Matching for switch blocks
    private static void processResult(LogResult result) {
        switch (result) {
            case InfoResult info -> 
                System.out.printf("[THREAD: %s] INFO LOG -> %s%n", 
                    Thread.currentThread().toString(), info.message());
            case ErrorResult error -> 
                System.out.printf("[THREAD: %s] 🔥 ERROR LOG -> %s (Code: %d)%n", 
                    Thread.currentThread().toString(), error.message(), error.code());
        }
    }
}
