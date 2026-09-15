package de.fr4ncyilgrande.autotpa.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class ModKeyBindings {
    public static final KeyMapping OPEN_MENU = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.autotpa.open_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    KeyMapping.Category.register(
                            Identifier.fromNamespaceAndPath("autotpa", "main"))
            )
    );

    private ModKeyBindings() {}

    public static void register(Runnable onOpenMenuPressed) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU.consumeClick()) {
                if (client.gui.screen() == null) {
                    onOpenMenuPressed.run();
                }
            }
        });
    }
}
