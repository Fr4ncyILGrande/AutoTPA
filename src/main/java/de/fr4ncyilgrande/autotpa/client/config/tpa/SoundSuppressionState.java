package de.fr4ncyilgrande.autotpa.client.tpa;

import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SoundSuppressionState {
    private static final long CORRELATION_WINDOW_MILLIS = 1000L;

    private static volatile long suppressUntil = -1L;
    private static volatile ModConfig activeConfig;

    private SoundSuppressionState() {}

    public static void bind(ModConfig config) {
        activeConfig = config;
    }

    public static void markBlockedMessage() {
        suppressUntil = System.currentTimeMillis() + CORRELATION_WINDOW_MILLIS;
    }

    public static boolean shouldSuppress(String soundId) {
        ModConfig config = activeConfig;
        if (config == null || config.suppressedSoundIds.isEmpty()) return false;
        if (suppressUntil < 0) return false;
        if (System.currentTimeMillis() > suppressUntil) return false;
        return config.suppressedSoundIds.contains(soundId);
    }

    public static boolean isDebugMode() {
        ModConfig config = activeConfig;
        return config != null && config.debugMode;
    }

    public static boolean isWithinCorrelationWindow() {
        return suppressUntil >= 0 && System.currentTimeMillis() <= suppressUntil;
    }
}