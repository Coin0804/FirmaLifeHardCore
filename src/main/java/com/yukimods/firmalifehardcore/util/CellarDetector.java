package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 地窖检测引擎 — BFS floodfill + 热阻计算。
 * 从种子位置出发，检测封闭空间并计算材质热阻和有效温度。
 */
public final class CellarDetector {

    private CellarDetector() {}

    /**
     * 从种子位置尝试 floodfill 检测封闭地窖空间。
     * @param level      服务端 Level
     * @param seedPos    种子起始位置（通常是容器所在位置）
     * @param scanRadius 搜索半径
     * @return CellarSpace 如果检测到封闭空间，null 如果空间不封闭或热阻不足
     */
    @Nullable
    public static CellarSpace detect(Level level, BlockPos seedPos, int scanRadius) {
        int diameter = scanRadius * 2 + 1;
        BoundingBox bounds = new BoundingBox(
            seedPos.getX() - scanRadius, seedPos.getY() - scanRadius, seedPos.getZ() - scanRadius,
            seedPos.getX() + scanRadius, seedPos.getY() + scanRadius, seedPos.getZ() + scanRadius
        );
        int maxSize = bounds.getXSpan() * bounds.getYSpan() * bounds.getZSpan();

        CellarSpace space = new CellarSpace(seedPos);

        // BFS floodfill
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        visited.add(seedPos);
        queue.add(seedPos);

        while (!queue.isEmpty()) {
            if (visited.size() > maxSize) {
                return null; // floodfill 溢出 — 空间不封闭
            }

            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            // 排除气候站方块（如果 Firmalife 的 ClimateStation 碰巧在这个位置）
            if (isClimateStation(currentState)) {
                continue;
            }

            // 内部分类：空气/流体/植物 → 是内部空间
            if (isInterior(currentState)) {
                space.interiorPositions.add(current.immutable());
            } else if (ThermalConductivity.isRelevant(currentState)) {
                // 墙体方块
                space.wallPositions.add(current.immutable());
                continue; // 墙不加入 BFS 扩展
            } else {
                // 既不是内部也不是墙 → 不封闭
                return null;
            }

            // 向 6 个方向扩展
            for (Direction dir : Direction.values()) {
                mutable.set(current).move(dir);
                if (!bounds.isInside(mutable)) {
                    return null; // 超出边界 → 空间太大或不封闭
                }
                if (!visited.contains(mutable)) {
                    visited.add(mutable);
                    queue.add(mutable.immutable());
                }
            }
        }

        if (space.interiorPositions.isEmpty() || space.wallPositions.isEmpty()) {
            return null; // 没有内部空间或没有墙体
        }

        // 计算热阻
        calculateThermal(level, space);
        if (space.avgResistance < FirmaLifeHardCoreConfig.SERVER.minThermalResistance.get()) {
            space.valid = false;
            return space; // 返回但标记为无效（调试需要）
        }

        space.valid = true;
        space.lastCheckedTick = level.getServer().getTickCount();
        return space;
    }

    /**
     * 计算 CellarSpace 的热阻和有效温度。
     * 遍历所有墙方块，查 tag 获取热阻等级，处理门状态。
     */
    public static void calculateThermal(Level level, CellarSpace space) {
        int high = 0, medium = 0, low = 0, unmatched = 0;
        float totalResistance = 0f;
        int validWallCount = 0;

        for (BlockPos wallPos : space.wallPositions) {
            BlockState wallState = level.getBlockState(wallPos);

            // 处理门/活板门
            if (ThermalConductivity.isDoor(wallState)) {
                if (ThermalConductivity.isOpenDoor(wallState)) {
                    // 开启的门 — 热阻归零，不计入墙体
                    continue;
                }
                // 双门加成
                Direction facing = getDoorFacing(wallState);
                if (facing != null && ThermalConductivity.hasDoubleDoor(level, wallPos, wallState, facing)) {
                    // 第一扇门给予双门加成，第二扇门正常计算
                    float doorResistance = ThermalConductivity.getResistance(wallState);
                    totalResistance += doorResistance * FirmaLifeHardCoreConfig.SERVER.doubleDoorMultiplier.get().floatValue();
                    validWallCount++;
                    // 跳过错开第二扇门的位置（避免重复计算）
                    BlockPos pairedPos = wallPos.relative(facing);
                    if (space.wallPositions.contains(pairedPos)) {
                        // 第二扇门单独计算
                        continue;
                    }
                }
                // 正常关门 — 查 tag 获取热阻
                float res = ThermalConductivity.getResistance(wallState);
                if (res > 0) {
                    totalResistance += res;
                    validWallCount++;
                }
                // 门的热阻来自它自己的材质（通常是 LOW 或 MEDIUM）
                // 如果门不在 insulation tag 中，赋予最低热阻
                if (res == 0) {
                    totalResistance += FirmaLifeHardCoreConfig.SERVER.resistanceMedium.get().floatValue();
                    validWallCount++;
                    medium++;
                }
                continue;
            }

            // 非门墙体
            float res = ThermalConductivity.getResistance(wallState);
            String tier = ThermalConductivity.getTierName(wallState);
            switch (tier) {
                case "HIGH"   -> high++;
                case "MEDIUM" -> medium++;
                case "LOW"    -> low++;
                default       -> unmatched++;
            }

            if (res > 0) {
                totalResistance += res;
                validWallCount++;
            } else {
                unmatched++;
            }
        }

        space.highCount = high;
        space.mediumCount = medium;
        space.lowCount = low;
        space.unmatchedCount = unmatched;

        if (validWallCount > 0) {
            space.avgResistance = Math.min(1.0f, totalResistance / validWallCount);
        } else {
            space.avgResistance = 0f;
        }

        // 计算有效温度
        float outdoorTemp = Climate.getAverageTemperature(level, space.seedPos);
        float clampedResistance = Math.min(
            FirmaLifeHardCoreConfig.SERVER.maxPreservationCap.get().floatValue(),
            space.avgResistance
        );
        space.effectiveTemperature = outdoorTemp * (1.0f - clampedResistance);
    }

    /** 方块是否为内部空间（空气、流体、植物等可穿透方块） */
    private static boolean isInterior(BlockState state) {
        if (state.isAir()) return true;
        // 流体、植物、火把、地毯等可替换方块
        if (state.canBeReplaced()) return true;
        // 非实心方块通常不阻挡 floodfill
        if (!state.isSolid()) return true;
        return false;
    }

    /** 方块是否为 Firmalife ClimateStation */
    private static boolean isClimateStation(BlockState state) {
        // 通过方块 ID 检查（避免直接引用 Firmalife 类）
        var key = state.getBlockHolder().getKey();
        return key != null && key.location().toString().equals("firmalife:climate_station");
    }

    /** 获取门/活板门的朝向 */
    @Nullable
    private static Direction getDoorFacing(BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return null;
    }
}
