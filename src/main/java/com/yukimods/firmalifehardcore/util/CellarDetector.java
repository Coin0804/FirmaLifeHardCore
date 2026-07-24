package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public final class CellarDetector {

    private CellarDetector() {}

    private enum Type { TRANSPARENT, WALL, OBSTACLE }

    private static Type classify(BlockState state) {
        if (state.isAir() || state.canBeReplaced()) return Type.TRANSPARENT;
        if (ThermalConductivity.isRelevant(state) || ThermalConductivity.isDoor(state)) return Type.WALL;
        return Type.OBSTACLE;
    }

    @Nullable
    public static CellarSpace detectFromSeed(Level level, BlockPos seedPos) {
        return detectOne(level, seedPos);
    }

    public static List<CellarSpace> detectAll(Level level, BlockPos origin) {
        List<CellarSpace> results = new ArrayList<>();
        Set<BlockPos> seenInteriors = new HashSet<>();

        List<BlockPos> candidates = new ArrayList<>();
        if (classify(level.getBlockState(origin)) == Type.TRANSPARENT)
            candidates.add(origin.immutable());
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            if (classify(level.getBlockState(neighbor)) == Type.TRANSPARENT)
                candidates.add(neighbor);
        }

        for (BlockPos seed : candidates) {
            CellarSpace space = detectOne(level, seed);
            if (space != null && space.valid && !seenInteriors.containsAll(space.interiorPositions)) {
                seenInteriors.addAll(space.interiorPositions);
                results.add(space);
            }
        }
        return results;
    }

    /**
     * BFS floodfill 检测单个封闭空间。
     * 每步动态追踪 interior 的 AABB——任一方向 span 超过配置限制即 OOB，
     * 避免跑完整个 BFS 才发现房间太大，节省算力。
     * 完成后以 AABB 最小角点作为规范种子位置（{@code (minX, minY, minZ)}）。
     */
    @Nullable
    private static CellarSpace detectOne(Level level, BlockPos seedPos) {
        int maxHorizSpan = FirmaLifeHardCoreConfig.SERVER.maxHorizontalSpan.get();
        int maxVertSpan  = FirmaLifeHardCoreConfig.SERVER.maxVerticalSpan.get();
        // visited 上限：全覆盖体积，防止极端情况的最后防线
        int maxSize = (maxHorizSpan + 5) * (maxHorizSpan + 5) * (maxVertSpan + 5);

        CellarSpace space = new CellarSpace(seedPos);
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // AABB 追踪——初始化为种子位置
        int minX = seedPos.getX(), maxX = seedPos.getX();
        int minY = seedPos.getY(), maxY = seedPos.getY();
        int minZ = seedPos.getZ(), maxZ = seedPos.getZ();

        visited.add(seedPos);
        queue.add(seedPos);
        space.interiorPositions.add(seedPos.immutable());

        while (!queue.isEmpty()) {
            if (visited.size() > maxSize) return null;  // 最后防线

            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                mutable.set(current).move(dir);
                if (visited.contains(mutable)) continue;

                BlockPos pos = mutable.immutable();
                switch (classify(level.getBlockState(mutable))) {
                    case TRANSPARENT -> {
                        visited.add(pos);
                        queue.add(pos);
                        space.interiorPositions.add(pos);
                        // fall-through: 更新 AABB + OOB 检查
                        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                        if (x < minX) minX = x; else if (x > maxX) maxX = x;
                        if (y < minY) minY = y; else if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z; else if (z > maxZ) maxZ = z;
                        if (maxX - minX + 1 > maxHorizSpan
                            || maxY - minY + 1 > maxVertSpan
                            || maxZ - minZ + 1 > maxHorizSpan) {
                            return null;
                        }
                    }
                    case OBSTACLE -> {
                        visited.add(pos);
                        queue.add(pos);
                        space.obstaclePositions.add(pos);
                        // OBSTACLE 也更新 AABB，防止通过岩石泄漏
                        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                        if (x < minX) minX = x; else if (x > maxX) maxX = x;
                        if (y < minY) minY = y; else if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z; else if (z > maxZ) maxZ = z;
                        if (maxX - minX + 1 > maxHorizSpan
                            || maxY - minY + 1 > maxVertSpan
                            || maxZ - minZ + 1 > maxHorizSpan) {
                            return null;
                        }
                    }
                    case WALL -> {
                        visited.add(pos);
                        space.wallPositions.add(pos);
                    }
                }
            }
        }

        if (space.interiorPositions.isEmpty() || space.wallPositions.isEmpty()) return null;

        // 规范化种子位置：从 interior 中选 X 最小 → Y 最小 → Z 最小的真实位置
        // 不用 AABB 角点 (minX,minY,minZ)，因为三个轴的最小值可能来自不同方块，
        // 组合出的角点在 L 形等不规则房间中可能不在地窖内部
        BlockPos canonicalSeed = seedPos;
        int bestX = Integer.MAX_VALUE, bestY = Integer.MAX_VALUE, bestZ = Integer.MAX_VALUE;
        for (BlockPos ip : space.interiorPositions) {
            int x = ip.getX(), y = ip.getY(), z = ip.getZ();
            if (x < bestX || (x == bestX && y < bestY) || (x == bestX && y == bestY && z < bestZ)) {
                canonicalSeed = ip;
                bestX = x; bestY = y; bestZ = z;
            }
        }
        space.seedPos = canonicalSeed;

        calculateThermal(level, space);
        space.valid = true;
        space.lastCheckedTick = level.getServer().getTickCount();
        return space;
    }

    public static void calculateThermal(Level level, CellarSpace space) {
        int high = 0, medium = 0, low = 0, unmatched = 0;
        int doors = 0, doubleDoors = 0;
        float totalResistance = 0f;
        int validWallCount = 0;
        int totalRoof = 0, canopyRoof = 0;

        for (BlockPos wallPos : space.wallPositions) {
            BlockState wallState = level.getBlockState(wallPos);

            // 棚顶判定：墙块下方紧邻内部空间 → 这是屋顶
            boolean isRoof = space.interiorPositions.contains(wallPos.below());
            if (isRoof) {
                totalRoof++;
                // 棚顶需要玻璃且能看到天空才计入
                if (ThermalConductivity.isGreenhouseRoof(wallState) && level.canSeeSky(wallPos))
                    canopyRoof++;
            }

            float res = ThermalConductivity.getResistance(wallState);
            if (ThermalConductivity.isDoor(wallState)) { //门不算墙
                if (wallState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)){ // 普通门
                    if(wallState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER){
                        // 上半，不计入屋顶（门不算屋顶）
                        if (isRoof) { totalRoof--; if (ThermalConductivity.isGreenhouseRoof(wallState)) canopyRoof--; }
                        continue;
                    }else{
                        // 正常门只算下半，不计入屋顶
                        if (isRoof) { totalRoof--; if (ThermalConductivity.isGreenhouseRoof(wallState)) canopyRoof--; }
                        res = res==0?res:FirmaLifeHardCoreConfig.SERVER.resistanceMedium.get().floatValue();
                        if (ThermalConductivity.hasDoubleDoor(level, wallPos, wallState)) {
                            // 双门
                            doubleDoors++;
                            float multiplier = FirmaLifeHardCoreConfig.SERVER.doubleDoorMultiplier.get().floatValue();
                            totalResistance += res * multiplier ;
                            continue;
                        }// else 算单门
                    }
                } // else 是活板门
                // 单门，不计入屋顶
                if (isRoof) { totalRoof--; if (ThermalConductivity.isGreenhouseRoof(wallState)) canopyRoof--; }
                doors++;
                totalResistance += res ;
            }else{ //不是门
                switch (ThermalConductivity.getTierName(wallState)) {
                    case "HIGH" -> high++;
                    case "MEDIUM" -> medium++;
                    case "LOW" -> low++;
                    default -> unmatched++;
                }
                if(res>0){totalResistance += res; validWallCount++; }
            }
        }
        // 遍历结束
        space.highCount = high; space.mediumCount = medium; space.lowCount = low; space.unmatchedCount = unmatched;
        space.doorCount = doors; space.doubleDoorCount = doubleDoors;
        space.avgResistance = validWallCount > 0 ? Math.min(1f, totalResistance / validWallCount) : 0f;

        space.canopyRatio = totalRoof > 0 ? (float) canopyRoof / totalRoof : 0f;

        // 直接调 ClimateModel.getInstantTemperature 获取真实室外即时温度，绕过 ClimateMixin
        ICalendar cal = Calendars.get(level);
        float outdoor = Climate.get(level).getInstantTemperature(
            level, space.seedPos, cal.getCalendarTicks(), cal.getCalendarDaysInMonth());
        space.effectiveTemperature = space.getBaseTemperature()
            + (outdoor - space.getBaseTemperature()) * (1f - Math.min(1f, space.avgResistance));

        // 诊断日志：空间分类及关键参数
        FirmaLifeHardCore.LOGGER.debug("[CellarDetector] seed={} type={} avgR={} canopy={}% baseT={} outdoor={} effective={} walls={}",
            space.seedPos.toShortString(),
            space.isGreenhouse() ? "GREENHOUSE" : "CELLAR",
            String.format("%.2f", space.avgResistance),
            String.format("%.0f", space.canopyRatio * 100f),
            String.format("%.1f", space.getBaseTemperature()),
            String.format("%.1f", outdoor),
            String.format("%.1f", space.effectiveTemperature),
            space.wallPositions.size()
        );
    }
}
