package de.fr4ncyilgrande.autotpa.client.tpa;

import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SoundSuppressionState {
    private static final long CORRELATION_WINDOW_NANOS = TimeUnit.SECONDS.toNanos(1L);

    private static volatile long suppressUntilNanos;
    private static volatile ModConfig activeConfig;

    private SoundSuppressionState() {}

    public static void bind(ModConfig config) {
        activeConfig = config;
    }

    public static void markBlockedMessage() {
        suppressUntilNanos = System.nanoTime() + CORRELATION_WINDOW_NANOS;
    }

    public static boolean shouldSuppress(String soundId) {
        ModConfig config = activeConfig;
        return config != null && config.isSuppressedSound(soundId) && isWithinCorrelationWindow();
    }

    public static boolean isDebugMode() {
        ModConfig config = activeConfig;
        return config != null && config.debugMode;
    }

    public static boolean isWithinCorrelationWindow() {
        long until = suppressUntilNanos;
        if (until == 0L) return false;
        if (System.nanoTime() <= until) return true;
        suppressUntilNanos = 0L;
        return false;
    }
}
