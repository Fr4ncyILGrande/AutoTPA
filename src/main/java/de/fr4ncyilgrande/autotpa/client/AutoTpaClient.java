package de.fr4ncyilgrande.autotpa.client;

import de.fr4ncyilgrande.autotpa.client.config.ConfigManager;
import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import de.fr4ncyilgrande.autotpa.client.gui.TpaManagerScreen;
import de.fr4ncyilgrande.autotpa.client.keybind.ModKeyBindings;
import de.fr4ncyilgrande.autotpa.client.tpa.TpaEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class AutoTpaClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA");

    @Override
    public void onInitializeClient() {
        ModConfig config = ConfigManager.load();
        TpaEventHandler.init(config);

        ModKeyBindings.register(() ->
                Minecraft.getInstance().setScreen(new TpaManagerScreen(config))
        );

        if (config.debugMode) {
            LOGGER.info("[AutoTPA] Loaded - {} auto-accept, {} blocked, {} patterns.",
                    config.autoAccept.size(), config.blocked.size(), config.patterns.size());
        }
    }
}