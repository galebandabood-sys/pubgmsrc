package com.RIYAZSRC;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Explicit, user-initiated root shortcut for two binaries that the user
 * previously installed and manually ran. No executable payload is bundled.
 * This is not a claim that either external module works in the game.
 */
public final class ExternalToolsLauncher {
    public interface Callback {
        void finished(int exitCode, String output);
    }

    private static final String COMMAND =
            "cd /data/local/tmp || exit 80; " +
            "if [ ! -x ./ipad-irch ]; then echo MISSING_IPAD; exit 81; fi; " +
            "if [ ! -x ./frost-v37 ]; then echo MISSING_FROST; exit 82; fi; " +
            "if [ ! -r ./Frost.Key ]; then echo MISSING_FROST_KEY; exit 83; fi; " +
            "echo STARTING_IPAD; " +
            "printf '1\\n' | ./ipad-irch; a=$?; echo IPAD_EXIT=$a; " +
            "if [ \"$a\" -ne 0 ]; then exit 84; fi; " +
            "echo STARTING_FROST; " +
            "printf '16\\n1\\n1\\n' | ./frost-v37; b=$?; echo FROST_EXIT=$b; " +
            "exit \"$b\"";

    private ExternalToolsLauncher() {}

    public static void start(Callback callback) {
        new Thread(() -> {
            int exit = -1;
            StringBuilder messages = new StringBuilder();
            try {
                Process process = new ProcessBuilder("/system/bin/su", "-c", COMMAND)
                        .redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        messages.append(line).append('\n');
                        if (messages.length() > 5000) {
                            messages.delete(0, messages.length() - 4200);
                        }
                    }
                }
                exit = process.waitFor();
            } catch (Exception failure) {
                messages.append("LAUNCH_ERROR: ").append(failure.getClass().getSimpleName())
                        .append(": ").append(failure.getMessage());
                if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            }
            final int result = exit;
            final String log = messages.toString();
            new Handler(Looper.getMainLooper()).post(() -> callback.finished(result, log));
        }, "nova-external-root-launch").start();
    }
}
