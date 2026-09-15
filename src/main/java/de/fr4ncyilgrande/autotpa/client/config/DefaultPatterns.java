package de.fr4ncyilgrande.autotpa.client.config;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DefaultPatterns {
    private DefaultPatterns() {}

    public static List<PatternRule> createDefaults() {
        List<PatternRule> list = new ArrayList<>();
        list.add(new PatternRule("essentials_tpa_1",
                "%player% has requested to teleport to you\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpa_2",
                "%player% wants to teleport to you\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpa_3",
                "%player% sent you a teleport request\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpa_4",
                "%player% has requested to teleport to your location\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpa_5",
                "%player% would like to teleport to you\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpahere_1",
                "%player% has requested that you teleport to (?:them|him|her)\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpahere_2",
                "%player% wants you to teleport to (?:them|him|her)\\.?", "/tpaccept"));
        list.add(new PatternRule("essentials_tpahere_3",
                "%player% has requested you teleport to their location\\.?", "/tpaccept"));
        return list;
    }
}