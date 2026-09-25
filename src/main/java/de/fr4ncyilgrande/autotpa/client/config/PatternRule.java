package de.fr4ncyilgrande.autotpa.client.config;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PatternRule {
    private static final String NAMED_PLAYER_NAME_PATTERN = "(?<autotpaPlayer>[A-Za-z0-9_]{3,16})";
    private static final String UNCAPPED_PLAYER_NAME_PATTERN = "(?:[A-Za-z0-9_]{3,16})";
    private static final String PLAYER_PLACEHOLDER = "%player%";
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA/Patterns");

    public String id = "custom";
    public String regex = "";
    public String acceptCommand = "/tpaccept";
    public TpaRequestType requestType = TpaRequestType.UNKNOWN;
    public boolean enabled = true;

    private transient Pattern compiled;
    private transient boolean compileFailed;
    private transient String compiledFrom;
    private transient boolean compiledWithPlayerPlaceholder;

    public PatternRule() {}

    public PatternRule(String id, String regex, String acceptCommand) {
        this.id = id;
        this.regex = regex;
        this.acceptCommand = acceptCommand;
        this.requestType = TpaRequestType.TPA;
    }

    public Pattern compiled() {
        if (this.regex == null) return null;
        if (this.compiled != null && this.regex.equals(this.compiledFrom)) {
            return this.compiled;
        }
        if (this.compileFailed && this.regex.equals(this.compiledFrom)) {
            return null;
        }
        try {
            String expanded = expandPlayerPlaceholder(this.regex);
            this.compiled = Pattern.compile(expanded, Pattern.CASE_INSENSITIVE);
            this.compileFailed = false;
            this.compiledFrom = this.regex;
            this.compiledWithPlayerPlaceholder = this.regex.contains(PLAYER_PLACEHOLDER);
            return this.compiled;
        } catch (PatternSyntaxException e) {
            this.compiled = null;
            this.compileFailed = true;
            this.compiledFrom = this.regex;
            this.compiledWithPlayerPlaceholder = false;
            LOGGER.warn("Ignoring invalid AutoTPA pattern '{}': {}", this.id, e.getDescription());
            return null;
        }
    }

    public String extractPlayerName(java.util.regex.Matcher matcher) {
        if (matcher == null) return null;
        if (this.compiledWithPlayerPlaceholder) {
            return matcher.group("autotpaPlayer");
        }
        return matcher.groupCount() >= 1 ? matcher.group(1) : null;
    }

    public String buildAcceptCommand(String playerName) {
        if (this.acceptCommand == null) return "";
        if (this.acceptCommand.indexOf('\n') >= 0 || this.acceptCommand.indexOf('\r') >= 0) {
            return "";
        }
        String command = this.acceptCommand.trim();
        if (command.isEmpty()) return "";
        return playerName == null ? command : command.replace(PLAYER_PLACEHOLDER, playerName);
    }

    public boolean isRegularTpaRequest() {
        return this.requestType == TpaRequestType.TPA;
    }

    boolean normalize() {
        boolean modified = false;

        String normalizedId = this.id == null ? "custom" : this.id.trim();
        if (normalizedId.isEmpty()) normalizedId = "custom";
        if (!normalizedId.equals(this.id)) {
            this.id = normalizedId;
            modified = true;
        }

        if (this.requestType == null) {
            this.requestType = TpaRequestType.UNKNOWN;
            modified = true;
        }
        TpaRequestType legacyType = legacyRequestType(this.id);
        if (legacyType != null && this.requestType != legacyType) {
            this.requestType = legacyType;
            modified = true;
        }

        if (this.regex == null) {
            this.regex = "";
            modified = true;
        }

        if (this.acceptCommand == null) {
            this.acceptCommand = "/tpaccept";
            modified = true;
        } else {
            String normalizedCommand = this.acceptCommand.trim();
            if (!normalizedCommand.equals(this.acceptCommand)) {
                this.acceptCommand = normalizedCommand;
                modified = true;
            }
        }

        return modified;
    }

    private static TpaRequestType legacyRequestType(String ruleId) {
        if (ruleId == null) return null;
        if (ruleId.startsWith("essentials_tpahere_")) return TpaRequestType.TPA_HERE;
        if (ruleId.equals("essentials_tpa_3")) return TpaRequestType.UNKNOWN;
        if (ruleId.startsWith("essentials_tpa_")) return TpaRequestType.TPA;
        return null;
    }

    private static String expandPlayerPlaceholder(String expression) {
        int firstPlaceholder = expression.indexOf(PLAYER_PLACEHOLDER);
        if (firstPlaceholder < 0) {
            return expression;
        }

        String before = expression.substring(0, firstPlaceholder);
        String after = expression.substring(firstPlaceholder + PLAYER_PLACEHOLDER.length())
                .replace(PLAYER_PLACEHOLDER, UNCAPPED_PLAYER_NAME_PATTERN);
        return before + NAMED_PLAYER_NAME_PATTERN + after;
    }
}
