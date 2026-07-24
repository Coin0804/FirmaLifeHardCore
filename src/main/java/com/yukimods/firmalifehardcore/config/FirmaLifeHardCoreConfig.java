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
        public final ModConfigSpec.IntValue maxSpacesPerTick;
        public final ModConfigSpec.IntValue maxContainersPerTick;
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
            builder.comment("FirmaLife HardCore — 地窖热阻系统配置").push("server");

            scanRadius = builder
                .comment("地窖扫描半径（种子位置向各方向的最大搜索距离，对标 Firmalife 原版 inflatedBy(15)）")
                .defineInRange("scanRadius", 15, 3, 32);
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

            builder.push("preservationTiers");
            tier1Temperature = builder
                .comment("一级保鲜温度阈值（≤此值 = SHELVED），高于此值 = 无效地窖")
                .defineInRange("tier1", 16.0, -50.0, 50.0);
            tier2Temperature = builder
                .comment("二级保鲜温度阈值（≤此值 = SHELVED_2）")
                .defineInRange("tier2", 8.0, -50.0, 50.0);
            tier3Temperature = builder
                .comment("三级保鲜温度阈值（≤此值 = SHELVED_3）")
                .defineInRange("tier3", 0.0, -50.0, 50.0);
            builder.pop();

            builder.push("greenhouse");
            greenhouseCanopyMultiplier = builder
                .comment("温室棚顶温度乘数，基准温度 = 4 + 此值 × 棚顶比例，默认 40 表示 100% 棚顶时基准 44°C")
                .defineInRange("canopyMultiplier", 40.0, 0.0, 100.0);
            greenhouseGlassRatio = builder
                .comment("温室判定所需的最小棚顶比例（玻璃屋顶 / 总屋顶）")
                .defineInRange("glassRatio", 0.5, 0.0, 1.0);
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
