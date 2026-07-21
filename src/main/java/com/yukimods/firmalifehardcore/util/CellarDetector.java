package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public final class CellarDetector {

    private static final Logger LOG = FirmaLifeHardCore.LOGGER;

    /** 竖直方向最大扫描距离（上下各5格） */
    private static final int VERTICAL_RADIUS = 5;

    private CellarDetector() {}

    private enum Type { TRANSPARENT, WALL, OBSTACLE }

    private static Type classify(BlockState state) {
        if (state.isAir() || state.canBeReplaced()) return Type.TRANSPARENT;
        if (ThermalConductivity.isRelevant(state) || ThermalConductivity.isDoor(state)) return Type.WALL;
        return Type.OBSTACLE;
    }

    @Nullable
    public static CellarSpace detectFromSeed(Level level, BlockPos seedPos, int scanRadius) {
        return detectOne(level, seedPos, scanRadius, "REVALIDATE");
    }

    public static List<CellarSpace> detectAll(Level level, BlockPos origin, int scanRadius) {
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

        LOG.info("[CellarDetect] detectAll origin={} candidates={}", origin.toShortString(), candidates.size());
        for (BlockPos seed : candidates) {
            CellarSpace space = detectOne(level, seed, scanRadius, "DISCOVER");
            if (space != null && space.valid && !seenInteriors.containsAll(space.interiorPositions)) {
                seenInteriors.addAll(space.interiorPositions);
                results.add(space);
                LOG.info("[CellarDetect]  NEW seed={} interior={} walls={} avgR={:.2f}", seed.toShortString(), space.interiorPositions.size(), space.wallPositions.size(), space.avgResistance);
            }
        }
        return results;
    }

    @Nullable
    private static CellarSpace detectOne(Level level, BlockPos seedPos, int horizontalRadius, String tag) {
        int v = VERTICAL_RADIUS;
        int h = horizontalRadius;
        BoundingBox bounds = new BoundingBox(
            seedPos.getX() - h, seedPos.getY() - v, seedPos.getZ() - h,
            seedPos.getX() + h, seedPos.getY() + v, seedPos.getZ() + h
        );
        int maxSize = h * h * v;  // H² × V：圆柱/椭圆近似上限

        LOG.info("[CellarDetect] {} seed={} box H±{} V±{}", tag, seedPos.toShortString(), h, v);

        CellarSpace space = new CellarSpace(seedPos);
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int[] counts = new int[3]; // T, W, O

        visited.add(seedPos);
        queue.add(seedPos);
        space.interiorPositions.add(seedPos.immutable());

        while (!queue.isEmpty()) {
            if (visited.size() > maxSize) {
                LOG.info("[CellarDetect] {} seed={} OVERFLOW", tag, seedPos.toShortString());
                return null;
            }

            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                mutable.set(current).move(dir);
                if (!bounds.isInside(mutable)) {
                    BlockPos out = mutable.immutable();
                    LOG.info("[CellarDetect] {} seed={} OOB at {} dir={}", tag, seedPos.toShortString(), out.toShortString(), dir);
                    return null;
                }
                if (visited.contains(mutable)) continue;

                BlockPos pos = mutable.immutable();
                switch (classify(level.getBlockState(mutable))) {
                    case TRANSPARENT -> {
                        visited.add(pos);
                        queue.add(pos);
                        space.interiorPositions.add(pos);
                        counts[0]++;
                    }
                    case WALL -> {
                        visited.add(pos);
                        space.wallPositions.add(pos);
                        counts[1]++;
                    }
                    case OBSTACLE -> {
                        visited.add(pos);
                        space.obstaclePositions.add(pos);
                        counts[2]++;
                    }
                }
            }
        }

        LOG.info("[CellarDetect] {} seed={} T={} W={} O={}", tag, seedPos.toShortString(), counts[0], counts[1], counts[2]);

        if (space.interiorPositions.isEmpty() || space.wallPositions.isEmpty()) return null;

        calculateThermal(level, space);
        if (space.avgResistance < FirmaLifeHardCoreConfig.SERVER.minThermalResistance.get()) {
            LOG.info("[CellarDetect] {} seed={} FAIL avgR={:.2f}<min", tag, seedPos.toShortString(), space.avgResistance);
            space.valid = false;
            return space;
        }

        space.valid = true;
        space.lastCheckedTick = level.getServer().getTickCount();
        return space;
    }

    public static void calculateThermal(Level level, CellarSpace space) {
        int high = 0, medium = 0, low = 0, unmatched = 0;
        int doors = 0, doubleDoors = 0;
        float totalResistance = 0f;
        int validWallCount = 0;

        for (BlockPos wallPos : space.wallPositions) {
            BlockState wallState = level.getBlockState(wallPos);
            float res = ThermalConductivity.getResistance(wallState);
            if (ThermalConductivity.isDoor(wallState)) { //门不算墙
                if (wallState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)){ // 普通门
                    if(wallState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER){
                        // 上半
                        continue; 
                    }else{
                        // 正常门只算下半
                        res = res==0?res:FirmaLifeHardCoreConfig.SERVER.resistanceMedium.get().floatValue();
                        if (ThermalConductivity.hasDoubleDoor(level, wallPos, wallState)) {
                            // 双门
                            doubleDoors++;
                            float multiplier = FirmaLifeHardCoreConfig.SERVER.doubleDoorMultiplier.get().floatValue();
                            // 按双门计算热阻
                            totalResistance += res * multiplier ;
                            continue;
                        }// else 算单门
                    }
                } // else 是活板门
                // 单门
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

        float outdoor = Climate.getAverageTemperature(level, space.seedPos);
        float cap = FirmaLifeHardCoreConfig.SERVER.maxPreservationCap.get().floatValue();
        space.effectiveTemperature = outdoor * (1f - Math.min(cap, space.avgResistance));
    }
}
