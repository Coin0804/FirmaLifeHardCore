package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
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

    private enum Type { TRANSPARENT, WALL, OBSTACLE, RECEIVER }

    private static Type classify(BlockState state) {
        if (state.isAir() || state.canBeReplaced()) return Type.TRANSPARENT;
        if (ThermalConductivity.isRelevant(state) || ThermalConductivity.isDoor(state)) return Type.WALL;
        if (state.is(ThermalConductivity.TAG_CLIMATE_RECEIVERS)) return Type.RECEIVER;
        return Type.OBSTACLE;
    }

    /**
     * BFS 检测结果。区分"真正失败"和"被未加载 chunk 中断"两种 null 场景。
     */
    public static class DetectResult {
        @Nullable public final CellarSpace space;
        public final boolean interrupted;

        private DetectResult(@Nullable CellarSpace space, boolean interrupted) {
            this.space = space;
            this.interrupted = interrupted;
        }

        public static DetectResult success(CellarSpace s) { return new DetectResult(s, false); }
        public static DetectResult failed() { return new DetectResult(null, false); }
        public static DetectResult interrupted() { return new DetectResult(null, true); }
    }

    public static DetectResult detectFromSeed(Level level, BlockPos seedPos) {
        return detectOne(level, seedPos);
    }

    public static List<CellarSpace> detectAll(Level level, BlockPos origin) {
        List<CellarSpace> results = new ArrayList<>();
        Set<BlockPos> seenInteriors = new HashSet<>();

        List<BlockPos> candidates = new ArrayList<>();
        if (level.isLoaded(origin) && classify(level.getBlockState(origin)) != Type.WALL) {
            candidates.add(origin.immutable());
        }
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            if (classify(level.getBlockState(neighbor)) != Type.WALL) {
                candidates.add(neighbor);
            }
        }

        for (BlockPos seed : candidates) {
            DetectResult result = detectOne(level, seed);
            if (result.space != null && result.space.valid
                && !seenInteriors.containsAll(result.space.interiorPositions)) {
                seenInteriors.addAll(result.space.interiorPositions);
                results.add(result.space);
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
    private static DetectResult detectOne(Level level, BlockPos seedPos) {
        int maxHorizSpan = FirmaLifeHardCoreConfig.SERVER.maxHorizontalSpan.get();
        int maxVertSpan  = FirmaLifeHardCoreConfig.SERVER.maxVerticalSpan.get();
        // visited 上限：全覆盖体积，防止极端情况的最后防线
        int maxSize = (maxHorizSpan + 5) * (maxHorizSpan + 5) * (maxVertSpan + 5);

        CellarSpace space = new CellarSpace(seedPos);
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // AABB 追踪——初始化为种子位置
        // bounds[0]=minX, [1]=maxX, [2]=minY, [3]=maxY, [4]=minZ, [5]=maxZ
        int[] bounds = {seedPos.getX(), seedPos.getX(), seedPos.getY(), seedPos.getY(), seedPos.getZ(), seedPos.getZ()};

        visited.add(seedPos);
        queue.add(seedPos);
        // 种子位置按实际类型归类（RECEIVER 可作种子时不能硬编码为 interior）
        switch (classify(level.getBlockState(seedPos))) {
            case OBSTACLE -> space.obstaclePositions.add(seedPos.immutable());
            case RECEIVER -> space.receiverPositions.add(seedPos.immutable());
            default -> space.interiorPositions.add(seedPos.immutable());
        }

        while (!queue.isEmpty()) {
            if (visited.size() > maxSize) return DetectResult.failed();  // 最后防线

            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                mutable.set(current).move(dir);
                if (visited.contains(mutable)) continue;

                BlockPos pos = mutable.immutable();
                // 未加载 chunk —— 信息不完整，放弃本次检测
                if (!level.isLoaded(pos)) return DetectResult.interrupted();

                Type t = classify(level.getBlockState(mutable));
                if (t == Type.WALL) {
                    visited.add(pos);
                    space.wallPositions.add(pos);
                    continue;
                }
                // TRANSPARENT / OBSTACLE / RECEIVER — 可扩展节点，共用尾部逻辑
                Set<BlockPos> targetSet = switch (t) {
                    case TRANSPARENT -> space.interiorPositions;
                    case OBSTACLE -> space.obstaclePositions;
                    case RECEIVER -> space.receiverPositions;
                    default -> throw new IllegalStateException("Unexpected type: " + t);
                };
                visited.add(pos);
                queue.add(pos);
                targetSet.add(pos);
                if (updateBounds(pos, bounds, maxHorizSpan, maxVertSpan)) return DetectResult.failed();
            }
        }

        if (space.interiorPositions.isEmpty() || space.wallPositions.isEmpty()) return DetectResult.failed();

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
        return DetectResult.success(space);
    }

    /**
     * 更新 AABB 边界并检查是否超出尺寸限制。
     * bounds[0]=minX, [1]=maxX, [2]=minY, [3]=maxY, [4]=minZ, [5]=maxZ
     * @return true = 超出限制（调用方应中止 BFS）
     */
    private static boolean updateBounds(BlockPos pos, int[] bounds, int maxHorizSpan, int maxVertSpan) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (x < bounds[0]) bounds[0] = x; else if (x > bounds[1]) bounds[1] = x;
        if (y < bounds[2]) bounds[2] = y; else if (y > bounds[3]) bounds[3] = y;
        if (z < bounds[4]) bounds[4] = z; else if (z > bounds[5]) bounds[5] = z;
        return bounds[1] - bounds[0] + 1 > maxHorizSpan
            || bounds[3] - bounds[2] + 1 > maxVertSpan
            || bounds[5] - bounds[4] + 1 > maxHorizSpan;
    }

    public static void calculateThermal(Level level, CellarSpace space) {
        int high = 0, medium = 0, low = 0, unmatched = 0;
        int doors = 0, doubleDoors = 0;
        float totalResistance = 0f;
        int validWallCount = 0;
        int totalRoof = 0, canopyRoof = 0;

        for (BlockPos wallPos : space.wallPositions) {
            BlockState wallState = level.getBlockState(wallPos);

            // 棚顶判定：墙块下方紧邻内部空间或内部障碍物 → 这是屋顶
            BlockPos below = wallPos.below();
            boolean isRoof = space.interiorPositions.contains(below)
                || space.obstaclePositions.contains(below);
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

    }
}
