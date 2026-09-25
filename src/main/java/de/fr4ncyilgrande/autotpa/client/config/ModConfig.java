package de.fr4ncyilgrande.autotpa.client.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModConfig {
    private static final int MAX_PLAYER_NAME_LENGTH = 16;

    public Set<String> autoAccept = new LinkedHashSet<>();
    public Set<String> blocked = new LinkedHashSet<>();
    public boolean acceptAllTpaRequests = false;
    public boolean debugMode = false;
    public int cooldownSeconds = 5;
    public List<PatternRule> patterns = DefaultPatterns.createDefaults();
    public Set<String> suppressedSoundIds = new LinkedHashSet<>();

    public boolean isAutoAccept(String name) {
        return findIgnoreCase(this.autoAccept, name) != null;
    }

    public boolean isBlocked(String name) {
        return findIgnoreCase(this.blocked, name) != null;
    }

    public boolean shouldAutoAccept(String name) {
        return !isBlocked(name) && (this.acceptAllTpaRequests || isAutoAccept(name));
    }

    public boolean addAutoAccept(String name) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName == null) return false;
        boolean removed = removeIgnoreCase(this.blocked, normalizedName);
        return addIgnoreCase(this.autoAccept, normalizedName) || removed;
    }

    public boolean addBlocked(String name) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName == null) return false;
        boolean removed = removeIgnoreCase(this.autoAccept, normalizedName);
        return addIgnoreCase(this.blocked, normalizedName) || removed;
    }

    public boolean removeAutoAccept(String name) {
        return removeIgnoreCase(this.autoAccept, name);
    }

    public boolean unblock(String name) {
        return removeIgnoreCase(this.blocked, name);
    }

    public boolean isSuppressedSound(String soundId) {
        return this.suppressedSoundIds != null
                && soundId != null
                && this.suppressedSoundIds.contains(soundId.toLowerCase(Locale.ROOT));
    }

    public static boolean isValidPlayerName(String name) {
        return normalizePlayerName(name) != null;
    }

    boolean normalize() {
        boolean modified = false;

        Set<String> normalizedAutoAccept = normalizePlayerNames(this.autoAccept);
        Set<String> normalizedBlocked = normalizePlayerNames(this.blocked);
        for (String name : new ArrayList<>(normalizedBlocked)) {
            String automaticallyAcceptedName = findIgnoreCase(normalizedAutoAccept, name);
            if (automaticallyAcceptedName != null) {
                normalizedAutoAccept.remove(automaticallyAcceptedName);
            }
        }
        if (!normalizedAutoAccept.equals(this.autoAccept)) {
            this.autoAccept = normalizedAutoAccept;
            modified = true;
        }
        if (!normalizedBlocked.equals(this.blocked)) {
            this.blocked = normalizedBlocked;
            modified = true;
        }

        Set<String> normalizedSoundIds = normalizeSoundIds(this.suppressedSoundIds);
        if (!normalizedSoundIds.equals(this.suppressedSoundIds)) {
            this.suppressedSoundIds = normalizedSoundIds;
            modified = true;
        }

        if (this.cooldownSeconds < 0) {
            this.cooldownSeconds = 0;
            modified = true;
        }

        if (this.patterns == null) {
            this.patterns = new ArrayList<>();
            modified = true;
        } else {
            List<PatternRule> normalizedPatterns = new ArrayList<>();
            for (PatternRule pattern : this.patterns) {
                if (pattern == null) {
                    modified = true;
                    continue;
                }
                if (pattern.normalize()) {
                    modified = true;
                }
                normalizedPatterns.add(pattern);
            }
            if (normalizedPatterns.size() != this.patterns.size()) {
                this.patterns = normalizedPatterns;
            }
        }

        return modified;
    }

    private static boolean addIgnoreCase(Set<String> set, String name) {
        if (set == null || name == null) return false;
        if (findIgnoreCase(set, name) != null) return false;
        set.add(name);
        return true;
    }

    private static boolean removeIgnoreCase(Set<String> set, String name) {
        if (set == null || name == null) return false;
        String existing = findIgnoreCase(set, name);
        if (existing == null) return false;
        set.remove(existing);
        return true;
    }

    private static String findIgnoreCase(Set<String> set, String name) {
        if (set == null || name == null) return null;
        for (String entry : set) {
            if (entry != null && entry.equalsIgnoreCase(name)) return entry;
        }
        return null;
    }

    private static Set<String> normalizePlayerNames(Set<String> names) {
        Set<String> normalized = new LinkedHashSet<>();
        if (names == null) {
            return normalized;
        }
        for (String name : names) {
            String validName = normalizePlayerName(name);
            if (validName != null && findIgnoreCase(normalized, validName) == null) {
                normalized.add(validName);
            }
        }
        return normalized;
    }

    private static Set<String> normalizeSoundIds(Set<String> soundIds) {
        Set<String> normalized = new LinkedHashSet<>();
        if (soundIds == null) {
            return normalized;
        }
        for (String soundId : soundIds) {
            if (soundId == null) continue;
            String trimmed = soundId.trim().toLowerCase(Locale.ROOT);
            if (trimmed.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private static String normalizePlayerName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.length() < 3 || trimmed.length() > MAX_PLAYER_NAME_LENGTH) return null;
        for (int i = 0; i < trimmed.length(); i++) {
            char character = trimmed.charAt(i);
            if (!(character >= 'A' && character <= 'Z')
                    && !(character >= 'a' && character <= 'z')
                    && !(character >= '0' && character <= '9')
                    && character != '_') {
                return null;
            }
        }
        return trimmed;
    }
}
