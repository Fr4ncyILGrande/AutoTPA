package de.fr4ncyilgrande.autotpa.client.tpa;

import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import de.fr4ncyilgrande.autotpa.client.config.PatternRule;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class TpaAcceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA");
    private static final long GLOBAL_MIN_SPACING_NANOS = TimeUnit.MILLISECONDS.toNanos(600L);
    private static final long ENTRY_TTL_NANOS = TimeUnit.MINUTES.toNanos(1L);

    private final Map<String, Long> lastAcceptedByPlayer = new HashMap<>();
    private long lastCommandSentAtNanos = Long.MIN_VALUE;

    public boolean tryAccept(String playerName, PatternRule rule, ModConfig config) {
        if (!ModConfig.isValidPlayerName(playerName) || rule == null || config == null) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        if (connection.getPlayerInfoIgnoreCase(playerName) == null) {
            if (config.debugMode) {
                LOGGER.info("[AutoTPA] Skipping accept for {} - player is no longer online", playerName);
            }
            return false;
        }

        long now = System.nanoTime();

        long cooldownNanos = TimeUnit.SECONDS.toNanos(Math.max(0L, config.cooldownSeconds));
        long entryRetentionNanos = Math.max(ENTRY_TTL_NANOS, cooldownNanos);
        this.lastAcceptedByPlayer.entrySet().removeIf(e -> now - e.getValue() > entryRetentionNanos);

        String key = playerName.toLowerCase(Locale.ROOT);

        Long lastForPlayer = this.lastAcceptedByPlayer.get(key);
        if (lastForPlayer != null && now - lastForPlayer < cooldownNanos) {
            if (config.debugMode) {
                long remainingMillis = TimeUnit.NANOSECONDS.toMillis(cooldownNanos - (now - lastForPlayer));
                LOGGER.info("[AutoTPA] Skipping accept for {} - cooldown ({} ms remaining)",
                        playerName, remainingMillis);
            }
            return false;
        }

        if (this.lastCommandSentAtNanos != Long.MIN_VALUE
                && now - this.lastCommandSentAtNanos < GLOBAL_MIN_SPACING_NANOS) {
            if (config.debugMode) {
                LOGGER.info("[AutoTPA] Skipping accept for {} - global spacing not elapsed", playerName);
            }
            return false;
        }

        String command = rule.buildAcceptCommand(playerName);
        if (command.isEmpty()) return false;
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) return false;

        if (config.debugMode) {
            LOGGER.info("[AutoTPA] Sending /{} (matched rule '{}')", command, rule.id);
        }

        connection.sendCommand(command);
        this.lastAcceptedByPlayer.put(key, now);
        this.lastCommandSentAtNanos = now;
        return true;
    }
}
