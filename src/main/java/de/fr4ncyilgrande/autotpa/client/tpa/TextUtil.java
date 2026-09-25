package de.fr4ncyilgrande.autotpa.client.tpa;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class TextUtil {
    private TextUtil() {}

    public static String plain(Component text) {
        return text == null ? "" : text.getString();
    }
}