package de.fr4ncyilgrande.autotpa.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
                if (config == null) {
                    throw new IllegalStateException("autotpa.json must contain a JSON object");
                }

                if (normalize(config)) {
                    save(config);
                }
                return config;
            } catch (RuntimeException | IOException e) {
                LOGGER.error("Failed to read autotpa.json; preserving a backup and restoring defaults", e);
                backupInvalidConfig();
            }
        }

        ModConfig fresh = new ModConfig();
        save(fresh);
        return fresh;
    }

    public static void save(ModConfig config) {
        if (config == null) {
            LOGGER.error("Refusing to save a null AutoTPA configuration");
            return;
        }

        normalize(config);

        Path temporaryFile = null;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            temporaryFile = Files.createTempFile(CONFIG_PATH.getParent(), "autotpa-", ".tmp");
            Files.writeString(temporaryFile, GSON.toJson(config), StandardCharsets.UTF_8);

            try {
                Files.move(temporaryFile, CONFIG_PATH,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException ignored) {
                Files.move(temporaryFile, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save autotpa.json", e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to remove temporary AutoTPA configuration file {}", temporaryFile, e);
                }
            }
        }
    }

    private static boolean normalize(ModConfig config) {
        boolean modified = false;

        if (config.patterns == null) {
            config.patterns = DefaultPatterns.createDefaults();
            modified = true;
        }

        if (config.normalize()) {
            modified = true;
        }
        return modified;
    }

    private static void backupInvalidConfig() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }

        try {
            Path backup = Files.createTempFile(CONFIG_PATH.getParent(), "autotpa-invalid-", ".json");
            Files.copy(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Saved the unreadable AutoTPA configuration as {}", backup.getFileName());
        } catch (IOException e) {
            LOGGER.error("Could not back up the unreadable AutoTPA configuration", e);
        }
    }
}
