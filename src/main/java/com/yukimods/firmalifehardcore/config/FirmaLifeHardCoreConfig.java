package com.yukimods.firmalifehardcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class FirmaLifeHardCoreConfig {

    public static final ServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<ServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
    }

    public static void init() {
        // 由 NeoForge 自动加载：ModContainer.registerConfig
    }

    public static ModConfigSpec getServerSpec() {
        return SERVER_SPEC;
    }

    public static class ServerConfig {
        public final ModConfigSpec.IntValue maxHorizontalSpan;
        public final ModConfigSpec.IntValue maxVerticalSpan;
        public final ModConfigSpec.IntValue maxSpacesPerTick;
        public final ModConfigSpec.DoubleValue doubleDoorMultiplier;

        public final ModConfigSpec.DoubleValue resistanceHigh;
        public final ModConfigSpec.DoubleValue resistanceMedium;
        public final ModConfigSpec.DoubleValue resistanceLow;

        public final ModConfigSpec.DoubleValue tier1Temperature;
        public final ModConfigSpec.DoubleValue tier2Temperature;
        public final ModConfigSpec.DoubleValue tier3Temperature;

        public final ModConfigSpec.DoubleValue greenhouseCanopyMultiplier;
        public final ModConfigSpec.DoubleValue greenhouseGlassRatio;

        public final ModConfigSpec.IntValue reinforcedSoilMaxDepth;
        public final ModConfigSpec.IntValue hammerDurabilityCost;

        ServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("FirmaLife HardCore — Cellar & Greenhouse Thermal System").push("server");

            maxHorizontalSpan = builder
                .comment("Maximum horizontal span (blocks) for cellar/greenhouse detection")
                .defineInRange("maxHorizontalSpan", 24, 3, 64);
            maxVerticalSpan = builder
                .comment("Maximum vertical span (blocks) for cellar/greenhouse detection")
                .defineInRange("maxVerticalSpan", 8, 2, 32);
            maxSpacesPerTick = builder
                .comment("Maximum spaces to recheck per tick")
                .defineInRange("maxSpacesPerTick", 20, 1, 1000);
            doubleDoorMultiplier = builder
                .comment("Multiplier applied to double doors for thermal resistance")
                .defineInRange("doubleDoorMultiplier", 4.0, 1.0, 10.0);

            builder.push("resistance");
            resistanceHigh = builder.defineInRange("high", 0.75, 0.0, 1.0);
            resistanceMedium = builder.defineInRange("medium", 0.55, 0.0, 1.0);
            resistanceLow = builder.defineInRange("low", 0.25, 0.0, 1.0);
            builder.pop();

            builder.push("preservationTiers");
            tier1Temperature = builder
                .comment("Tier 1 preservation threshold (≤ this = SHELVED). Above = no preservation")
                .defineInRange("tier1", 16.0, -50.0, 50.0);
            tier2Temperature = builder
                .comment("Tier 2 preservation threshold (≤ this = SHELVED_2)")
                .defineInRange("tier2", 8.0, -50.0, 50.0);
            tier3Temperature = builder
                .comment("Tier 3 preservation threshold (≤ this = SHELVED_3)")
                .defineInRange("tier3", 0.0, -50.0, 50.0);
            builder.pop();

            builder.push("greenhouse");
            greenhouseCanopyMultiplier = builder
                .comment("Greenhouse canopy temperature multiplier. Base temp = 4 + this × canopyRatio (default 40 → 44°C at 100% canopy)")
                .defineInRange("canopyMultiplier", 40.0, 0.0, 100.0);
            greenhouseGlassRatio = builder
                .comment("Minimum glass roof ratio required for greenhouse detection (glass_roof / total_roof)")
                .defineInRange("glassRatio", 0.5, 0.0, 1.0);
            builder.pop();

            builder.push("reinforcedSoil");
            reinforcedSoilMaxDepth = builder
                .comment("Maximum depth for reinforced soil downward placement")
                .defineInRange("maxDepth", 3, 1, 10);
            hammerDurabilityCost = builder
                .comment("Hammer durability cost per use on reinforced soil")
                .defineInRange("hammerDurabilityCost", 1, 1, 50);
            builder.pop();

            builder.pop();
        }
    }
}
