package de.fr4ncyilgrande.autotpa.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("autotpa.json");

    private ConfigManager() {}

    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config != null) {
                    if (normalize(config)) {
                        save(config);
                    }
                    return config;
                }
            } catch (RuntimeException | IOException e) {
                LOGGER.error("Failed to read autotpa.json, falling back to defaults", e);
            }
        }

        ModConfig fresh = new ModConfig();
        fresh.patterns = DefaultPatterns.createDefaults();
        save(fresh);
        return fresh;
    }

    public static void save(ModConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save autotpa.json", e);
        }
    }

    private static boolean normalize(ModConfig config) {
        boolean modified = false;

        if (config.patterns == null || config.patterns.isEmpty()) {
            config.patterns = DefaultPatterns.createDefaults();
            modified = true;
        }
        if (config.autoAccept == null) {
            config.autoAccept = new LinkedHashSet<>();
            modified = true;
        }
        if (config.blocked == null) {
            config.blocked = new LinkedHashSet<>();
            modified = true;
        }
        if (config.suppressedSoundIds == null) {
            config.suppressedSoundIds = new LinkedHashSet<>();
            modified = true;
        }
        return modified;
    }
}