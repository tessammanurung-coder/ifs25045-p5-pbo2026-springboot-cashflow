package org.delcom.ifs25045springbootcashflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memuat berkas .env ke System properties agar bisa dirujuk Spring (${DB_HOST}, dst).
 * Variabel environment OS dan -D yang sudah ada tidak ditimpa.
 */
public final class DotEnv {
    private DotEnv() {}

    public static void load() {
        load(Path.of(".env"));
    }

    public static void load(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            parse(Files.readAllLines(file, StandardCharsets.UTF_8)).forEach((key, value) -> {
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca " + file, e);
        }
    }

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }
}
