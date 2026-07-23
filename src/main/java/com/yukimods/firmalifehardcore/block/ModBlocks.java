package com.yukimods.firmalifehardcore.block;

import java.util.EnumMap;
import java.util.Map;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册中心 — 每个 TFC 土壤变体注册竖梁 + 横梁两个方块。
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(FirmaLifeHardCore.MOD_ID);

    private static final Map<ReinforcedSoilType, DeferredBlock<ReinforcedSoilBlock>> NORMAL =
        new EnumMap<>(ReinforcedSoilType.class);
    private static final Map<ReinforcedSoilType, DeferredBlock<ReinforcedSoilBeamBlock>> BEAM =
        new EnumMap<>(ReinforcedSoilType.class);

    static {
        for (ReinforcedSoilType type : ReinforcedSoilType.values()) {
            NORMAL.put(type, BLOCKS.register(type.blockName(),
                () -> new ReinforcedSoilBlock(type)));
            BEAM.put(type, BLOCKS.register(type.blockName() + "_beam",
                () -> new ReinforcedSoilBeamBlock(type)));
        }
    }

    private ModBlocks() {}

    public static DeferredBlock<ReinforcedSoilBlock> get(ReinforcedSoilType type) {
        return NORMAL.get(type);
    }

    public static DeferredBlock<ReinforcedSoilBeamBlock> getBeam(ReinforcedSoilType type) {
        return BEAM.get(type);
    }

    public static ReinforcedSoilBlock getAsBlock(ReinforcedSoilType type) {
        return NORMAL.get(type).get();
    }

    public static ReinforcedSoilBeamBlock getBeamAsBlock(ReinforcedSoilType type) {
        return BEAM.get(type).get();
    }

    public static Iterable<DeferredBlock<ReinforcedSoilBlock>> all() {
        return NORMAL.values();
    }
}
