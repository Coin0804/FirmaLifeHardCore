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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
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
    public static final TagKey<Block> TAG_DOOR   = create("cellar_doors");
    public static final TagKey<Block> TAG_CONTAINER = create("container_blocks");

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

    /** 检查门/活板门是否处于开启状态 */
    public static boolean isOpenDoor(BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            return state.getValue(DoorBlock.OPEN);
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            return state.getValue(TrapDoorBlock.OPEN);
        }
        if (state.hasProperty(BlockStateProperties.OPEN)) {
            return state.getValue(BlockStateProperties.OPEN);
        }
        return false;
    }

    /** 双门检测：门所在方向的下一格是否也有一扇关闭的门 */
    public static boolean hasDoubleDoor(Level level, BlockPos doorPos, BlockState doorState, Direction facing) {
        // 检查面对方向上的下一个方块
        BlockPos nextPos = doorPos.relative(facing);
        BlockState nextState = level.getBlockState(nextPos);
        if (!isDoor(nextState)) return false;

        // 对门方块，检查是否同一方向且都关闭
        if (doorState.getBlock() instanceof DoorBlock && nextState.getBlock() instanceof DoorBlock) {
            // 双门 = 两个门方块都关闭 + 方向一致
            return !isOpenDoor(doorState) && !isOpenDoor(nextState)
                && doorState.getValue(DoorBlock.FACING) == nextState.getValue(DoorBlock.FACING);
        }
        // 活板门：同一方向的连续两个
        if (doorState.getBlock() instanceof TrapDoorBlock && nextState.getBlock() instanceof TrapDoorBlock) {
            return !isOpenDoor(doorState) && !isOpenDoor(nextState);
        }
        return false;
    }

    /** 检查方块是否在容器 tag 中 */
    public static boolean isContainer(BlockState state) {
        return state.is(TAG_CONTAINER);
    }
}
