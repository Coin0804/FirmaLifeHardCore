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
        public final ModConfigSpec.IntValue scanRadius;
        public final ModConfigSpec.DoubleValue minThermalResistance;
        public final ModConfigSpec.IntValue maxSpacesPerTick;
        public final ModConfigSpec.IntValue maxContainersPerTick;
        public final ModConfigSpec.DoubleValue doubleDoorMultiplier;

        public final ModConfigSpec.DoubleValue resistanceHigh;
        public final ModConfigSpec.DoubleValue resistanceMedium;
        public final ModConfigSpec.DoubleValue resistanceLow;

        public final ModConfigSpec.DoubleValue level2ResistanceThreshold;
        public final ModConfigSpec.DoubleValue level3ResistanceThreshold;

        public final ModConfigSpec.IntValue reinforcedSoilMaxDepth;
        public final ModConfigSpec.IntValue hammerDurabilityCost;

        ServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("FirmaLife HardCore — 地窖热阻系统配置").push("server");

            scanRadius = builder
                .comment("地窖扫描半径（种子位置向各方向的最大搜索距离，对标 Firmalife 原版 inflatedBy(15)）")
                .defineInRange("scanRadius", 15, 3, 32);
            minThermalResistance = builder
                .comment("最小平均热阻值，低于此值地窖无效")
                .defineInRange("minThermalResistance", 0.2, 0.0, 1.0);
            maxSpacesPerTick = builder
                .comment("每 tick 最多重检的空间数")
                .defineInRange("maxSpacesPerTick", 3, 1, 20);
            maxContainersPerTick = builder
                .comment("每 tick 最多检查的容器数")
                .defineInRange("maxContainersPerTick", 5, 1, 50);
            doubleDoorMultiplier = builder
                .comment("双门加成倍率")
                .defineInRange("doubleDoorMultiplier", 1.2, 1.0, 2.0);

            builder.push("resistance");
            resistanceHigh = builder.defineInRange("high", 0.80, 0.0, 1.0);
            resistanceMedium = builder.defineInRange("medium", 0.55, 0.0, 1.0);
            resistanceLow = builder.defineInRange("low", 0.15, 0.0, 1.0);
            builder.pop();

            builder.push("thresholds");
            level2ResistanceThreshold = builder
                .comment("二级保鲜热阻阈值")
                .defineInRange("level2", 0.45, 0.0, 1.0);
            level3ResistanceThreshold = builder
                .comment("三级保鲜热阻阈值")
                .defineInRange("level3", 0.70, 0.0, 1.0);
            builder.pop();

            builder.push("reinforcedSoil");
            reinforcedSoilMaxDepth = builder
                .comment("带支撑土向下使用时最大深度")
                .defineInRange("maxDepth", 3, 1, 10);
            hammerDurabilityCost = builder
                .comment("锤每次使用消耗耐久")
                .defineInRange("hammerDurabilityCost", 1, 1, 50);
            builder.pop();

            builder.pop();
        }
    }
}
