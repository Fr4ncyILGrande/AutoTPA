package de.fr4ncyilgrande.autotpa.client.tpa;

import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import de.fr4ncyilgrande.autotpa.client.config.PatternRule;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class TpaAcceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA");
    private static final long GLOBAL_MIN_SPACING_MILLIS = 600L;
    private static final long ENTRY_TTL_MILLIS = 60_000L;

    private final Map<String, Long> lastAcceptedByPlayer = new HashMap<>();
    private long lastCommandSentAt = 0L;

    public void tryAccept(String playerName, PatternRule rule, ModConfig config) {
        if (playerName == null || playerName.isEmpty()) return;
        if (rule == null || config == null) return;

        long now = System.currentTimeMillis();

        this.lastAcceptedByPlayer.entrySet().removeIf(e -> now - e.getValue() > ENTRY_TTL_MILLIS);

        long cooldownMillis = (long) Math.max(0, config.cooldownSeconds) * 1000L;
        String key = playerName.toLowerCase(Locale.ROOT);

        Long lastForPlayer = this.lastAcceptedByPlayer.get(key);
        if (lastForPlayer != null && now - lastForPlayer < cooldownMillis) {
            if (config.debugMode) {
                LOGGER.info("[AutoTPA] Skipping accept for {} - cooldown ({} ms remaining)",
                        playerName, cooldownMillis - (now - lastForPlayer));
            }
            return;
        }

        if (now - this.lastCommandSentAt < GLOBAL_MIN_SPACING_MILLIS) {
            if (config.debugMode) {
                LOGGER.info("[AutoTPA] Skipping accept for {} - global spacing not elapsed", playerName);
            }
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;

        String command = rule.buildAcceptCommand(playerName);
        if (command.isEmpty()) return;
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (config.debugMode) {
            LOGGER.info("[AutoTPA] Sending /{} (matched rule '{}')", command, rule.id);
        }

        connection.sendCommand(command);
        this.lastAcceptedByPlayer.put(key, now);
        this.lastCommandSentAt = now;
    }
}