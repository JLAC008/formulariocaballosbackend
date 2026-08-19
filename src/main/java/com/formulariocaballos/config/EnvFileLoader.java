package com.formulariocaballos.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EnvFileLoader {
    private EnvFileLoader() {
    }

    public static void load() {
        load(Path.of(".env"));
    }

    static void load(Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                loadLine(line);
            }
        } catch (IOException ignored) {
            // The app can still start with real environment variables or application.yml defaults.
        }
    }

    private static void loadLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        if (key.isEmpty() || System.getenv(key) != null || System.getProperty(key) != null) {
            return;
        }

        System.setProperty(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
