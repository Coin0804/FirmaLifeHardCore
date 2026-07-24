package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


/**
 * 热传导率工具类 — 3 级热阻系统，通过 block tag 查询。
 */
public final class ThermalConductivity {

    // ---- Tag 定义 ----
    public static final TagKey<Block> TAG_HIGH   = create("thermal_insulation/high");
    public static final TagKey<Block> TAG_MEDIUM = create("thermal_insulation/medium");
    public static final TagKey<Block> TAG_LOW    = create("thermal_insulation/low");
    public static final TagKey<Block> TAG_DOOR       = create("cellar_doors");
    public static final TagKey<Block> TAG_GREENHOUSE_ROOF = create("greenhouse_roof");
    public static final TagKey<Block> TAG_CONTAINERS  = create("cellar_containers");
    public static final TagKey<Block> TAG_PLANTERS = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("firmalife", "planters"));

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(FirmaLifeHardCore.MOD_ID, path));
    }

    /** 根据 BlockState 获取热阻值（查 tag，未匹配返回 0） */
    public static float getResistance(BlockState state) {
        if (state.is(TAG_HIGH))   return FirmaLifeHardCoreConfig.SERVER.resistanceHigh.get().floatValue();
        if (state.is(TAG_MEDIUM)) return FirmaLifeHardCoreConfig.SERVER.resistanceMedium.get().floatValue();
        if (state.is(TAG_LOW))    return FirmaLifeHardCoreConfig.SERVER.resistanceLow.get().floatValue();
        return 0f;
    }

    /** 获取热阻等级名称（调试用） */
    public static String getTierName(BlockState state) {
        if (state.is(TAG_HIGH))   return "HIGH";
        if (state.is(TAG_MEDIUM)) return "MEDIUM";
        if (state.is(TAG_LOW))    return "LOW";
        return "NONE";
    }

    /** 方块是否参与地窖热阻计算（在 insulation tag 或 door tag 中） */
    public static boolean isRelevant(BlockState state) {
        return state.is(TAG_HIGH) || state.is(TAG_MEDIUM) || state.is(TAG_LOW) || state.is(TAG_DOOR);
    }

    /** 检查是否门/活板门 */
    public static boolean isDoor(BlockState state) {
        return state.is(TAG_DOOR);
    }

    /** 检查门/活板门是否处于开启状态（Door/TrapDoor/Firmalife 门统一用 OPEN 属性） */
    public static boolean isOpenDoor(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
    }

    /** 双门检测：面对方向及其反方向相邻位置是否有关闭的门 */
    public static boolean hasDoubleDoor(Level level, BlockPos doorPos, BlockState doorState) {
        Direction facing = getDoorFacing(doorState);
        for (Direction dir : new Direction[]{facing, facing.getOpposite()}) {
            BlockPos nextPos = doorPos.relative(dir);
            BlockState nextState = level.getBlockState(nextPos);
            if (isDoor(nextState))
                return true;
        }
        return false;
    }

    /** 检查方块是否为温室棚顶（玻璃类透明方块，用于顶部透光） */
    public static boolean isGreenhouseRoof(BlockState state) {
        return state.is(TAG_GREENHOUSE_ROOF);
    }

    private static Direction getDoorFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING);
        return null;
    }
}
