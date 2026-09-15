package de.fr4ncyilgrande.autotpa.client.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModConfig {
    public Set<String> autoAccept = new LinkedHashSet<>();
    public Set<String> blocked = new LinkedHashSet<>();
    public boolean debugMode = false;
    public int cooldownSeconds = 5;
    public List<PatternRule> patterns = new ArrayList<>();
    public Set<String> suppressedSoundIds = new LinkedHashSet<>();

    public boolean isAutoAccept(String name) {
        return findIgnoreCase(this.autoAccept, name) != null;
    }

    public boolean isBlocked(String name) {
        return findIgnoreCase(this.blocked, name) != null;
    }

    public boolean addAutoAccept(String name) {
        if (name == null || name.isEmpty()) return false;
        removeIgnoreCase(this.blocked, name);
        return addIgnoreCase(this.autoAccept, name);
    }

    public boolean addBlocked(String name) {
        if (name == null || name.isEmpty()) return false;
        removeIgnoreCase(this.autoAccept, name);
        return addIgnoreCase(this.blocked, name);
    }

    public boolean removeAutoAccept(String name) {
        return removeIgnoreCase(this.autoAccept, name);
    }

    public boolean unblock(String name) {
        return removeIgnoreCase(this.blocked, name);
    }

    private static boolean addIgnoreCase(Set<String> set, String name) {
        if (name == null) return false;
        if (findIgnoreCase(set, name) != null) return false;
        set.add(name);
        return true;
    }

    private static boolean removeIgnoreCase(Set<String> set, String name) {
        if (name == null) return false;
        String existing = findIgnoreCase(set, name);
        if (existing == null) return false;
        set.remove(existing);
        return true;
    }

    private static String findIgnoreCase(Set<String> set, String name) {
        if (name == null) return null;
        for (String entry : set) {
            if (entry != null && entry.equalsIgnoreCase(name)) return entry;
        }
        return null;
    }
}