package de.fr4ncyilgrande.autotpa.client.config;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PatternRule {
    private static final String PLAYER_NAME_PATTERN = "([A-Za-z0-9_]{3,16})";

    public String id = "custom";
    public String regex = "";
    public String acceptCommand = "/tpaccept";
    public boolean enabled = true;

    private transient Pattern compiled;
    private transient boolean compileFailed;
    private transient String compiledFrom;

    public PatternRule() {}

    public PatternRule(String id, String regex, String acceptCommand) {
        this.id = id;
        this.regex = regex;
        this.acceptCommand = acceptCommand;
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
            String expanded = this.regex.replace("%player%", PLAYER_NAME_PATTERN);
            this.compiled = Pattern.compile(expanded, Pattern.CASE_INSENSITIVE);
            this.compileFailed = false;
            this.compiledFrom = this.regex;
            return this.compiled;
        } catch (PatternSyntaxException e) {
            this.compiled = null;
            this.compileFailed = true;
            this.compiledFrom = this.regex;
            return null;
        }
    }

    public String buildAcceptCommand(String playerName) {
        if (playerName == null) return this.acceptCommand;
        return this.acceptCommand.replace("%player%", playerName);
    }
}