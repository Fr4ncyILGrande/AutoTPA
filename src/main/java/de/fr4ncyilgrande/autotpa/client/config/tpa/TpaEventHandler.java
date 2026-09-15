package de.fr4ncyilgrande.autotpa.client.tpa;

import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import de.fr4ncyilgrande.autotpa.client.config.PatternRule;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class TpaEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA");
    private static final int MAX_MESSAGE_LENGTH = 512;

    private static TpaEventHandler INSTANCE;

    private ModConfig config;
    private final TpaAcceptor acceptor = new TpaAcceptor();

    private TpaEventHandler(ModConfig config) {
        this.config = config;
    }

    public static synchronized void init(ModConfig config) {
        if (INSTANCE == null) {
            INSTANCE = new TpaEventHandler(config);

            ClientReceiveMessageEvents.ALLOW_GAME.register(
                    (message, overlay) -> INSTANCE.handle(message));

            ClientReceiveMessageEvents.ALLOW_CHAT.register(
                    (message, signedMessage, sender, params, receptionTimestamp) -> {
                        if (sender != null) return true;
                        return INSTANCE.handle(message);
                    });
        } else {
            INSTANCE.config = config;
        }
        SoundSuppressionState.bind(config);
    }

    private boolean handle(Component messageText) {
        String message = TextUtil.plain(messageText);
        if (message.isEmpty() || message.length() > MAX_MESSAGE_LENGTH) {
            return true;
        }

        if (this.config.debugMode) {
            LOGGER.info("[AutoTPA][raw] {}", message);
        }

        Match match = this.findMatch(message);
        if (match == null) return true;

        String playerName = match.playerName;

        if (this.config.isBlocked(playerName)) {
            LOGGER.info("[AutoTPA] Blocked TPA request from {} - hiding message", playerName);
            SoundSuppressionState.markBlockedMessage();
            return false;
        }

        if (this.config.isAutoAccept(playerName)) {
            LOGGER.info("[AutoTPA] Auto-accepting TPA request from {}", playerName);
            this.acceptor.tryAccept(playerName, match.rule, this.config);
        } else if (this.config.debugMode) {
            LOGGER.info("[AutoTPA] TPA request from {} ignored (not on any list)", playerName);
        }

        return true;
    }

    private Match findMatch(String message) {
        for (PatternRule rule : this.config.patterns) {
            if (!rule.enabled) continue;

            Pattern pattern = rule.compiled();
            if (pattern == null) continue;

            Matcher matcher = pattern.matcher(message);
            if (matcher.find() && matcher.groupCount() >= 1) {
                String name = matcher.group(1);
                if (name != null && !name.isEmpty()) {
                    return new Match(rule, name);
                }
            }
        }
        return null;
    }

    private static final class Match {
        final PatternRule rule;
        final String playerName;

        Match(PatternRule rule, String playerName) {
            this.rule = rule;
            this.playerName = playerName;
        }
    }
}