package de.fr4ncyilgrande.autotpa.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum TpaRequestType {
    TPA,
    TPA_HERE,
    UNKNOWN
}
