package com.yukimods.firmalifehardcore.util;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.dries007.tfc.util.climate.Climate;

import java.util.*;

/**
 * 全局地窖追踪器 — 每 ServerLevel 一个实例。
 * 事件驱动，每 tick 限量处理 dirty 队列，无全量轮询。
 */
public class CellarTracker {

    /** 所有已检测的地窖空间 (interiorPos → CellarSpace) */
    private final Map<BlockPos, CellarSpace> spacesByPos = new HashMap<>();

    /** 所有活跃空间集合 */
    private final Set<CellarSpace> allSpaces = new LinkedHashSet<>();

    /** 待重检的空间（去重） */
    private final Set<CellarSpace> dirtySpaces = new LinkedHashSet<>();

    /** 待检查的容器位置 */
    private final Deque<BlockPos> dirtyContainers = new ArrayDeque<>();

    /** 上次清理无效空间的时间 */
    private long lastCleanupTick = 0;
    private static final long CLEANUP_INTERVAL = 6000; // 5 分钟（20tps × 60s × 5）

    // ===== Tick 处理 =====

    /**
     * 每 tick 由事件处理器调用。
     * 优先处理 dirtySpaces，其次 dirtyContainers，限量避免卡顿。
     */
    public void tick(ServerLevel level) {
        int maxSpaces = FirmaLifeHardCoreConfig.SERVER.maxSpacesPerTick.get();
        int maxContainers = FirmaLifeHardCoreConfig.SERVER.maxContainersPerTick.get();
        int scanRadius = FirmaLifeHardCoreConfig.SERVER.scanRadius.get();

        // 1. 处理待重检空间
        int spacesProcessed = 0;
        Iterator<CellarSpace> spaceIt = dirtySpaces.iterator();
        while (spaceIt.hasNext() && spacesProcessed < maxSpaces) {
            CellarSpace space = spaceIt.next();
            spaceIt.remove();
            spacesProcessed++;

            // 重新检测
            CellarSpace newSpace = CellarDetector.detect(level, space.seedPos, scanRadius);
            updateSpace(level, space, newSpace);
        }

        // 2. 处理待检容器
        int containersProcessed = 0;
        Set<BlockPos> processed = new HashSet<>();
        while (!dirtyContainers.isEmpty() && containersProcessed < maxContainers) {
            BlockPos pos = dirtyContainers.poll();
            if (pos == null || processed.contains(pos)) continue;
            processed.add(pos);
            containersProcessed++;

            // 检查是否在已缓存空间内
            CellarSpace space = spacesByPos.get(pos);
            updateContainerState(level, pos, space);
        }

        // 3. 定期清理
        long currentTick = level.getServer().getTickCount();
        if (currentTick - lastCleanupTick > CLEANUP_INTERVAL) {
            cleanupInvalidSpaces(currentTick);
            lastCleanupTick = currentTick;
        }
    }

    // ===== 标记脏 =====

    /**
     * 方块变更时调用。
     */
    public void markDirty(BlockPos changedPos, int scanRadius) {
        // 标记相关 CellarSpace 重检
        for (CellarSpace space : allSpaces) {
            // 检查变更位置是否在该空间墙壁或内部附近
            if (isNearSpace(space, changedPos, scanRadius / 2)) {
                dirtySpaces.add(space);
            }
        }

        // 标记范围内容器待检
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dy = -scanRadius; dy <= scanRadius; dy++) {
                for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                    dirtyContainers.add(changedPos.offset(dx, dy, dz));
                }
            }
        }
    }

    /** 标记特定容器位置待检 */
    public void markContainerDirty(BlockPos containerPos) {
        dirtyContainers.add(containerPos);
    }

    /** 强制重算（调试指令用） */
    public void forceRecalc(BlockPos pos, ServerLevel level) {
        int scanRadius = FirmaLifeHardCoreConfig.SERVER.scanRadius.get();

        // 尝试从给定位置检测新空间
        CellarSpace newSpace = CellarDetector.detect(level, pos, scanRadius);
        if (newSpace != null && newSpace.valid) {
            // 移除旧的重叠空间
            for (BlockPos ip : newSpace.interiorPositions) {
                CellarSpace old = spacesByPos.remove(ip);
                if (old != null) allSpaces.remove(old);
            }
            // 注册新空间
            insertSpace(newSpace);
            broadcastToContainers(level, newSpace);
        } else {
            // 检测失败 — 使该位置所属空间失效
            CellarSpace existing = spacesByPos.get(pos);
            if (existing != null) {
                invalidateSpace(level, existing);
            }
        }
    }

    // ===== 查询 =====

    /** 查询位置是否在地窖中 */
    @Nullable
    public CellarSpace.CellarResult query(BlockPos pos) {
        CellarSpace space = spacesByPos.get(pos);
        if (space != null && space.valid) {
            return new CellarSpace.CellarResult(space.avgResistance, space.effectiveTemperature, true);
        }
        return null;
    }

    // ===== 调试 =====

    public CellarDebugInfo getDebugInfo(BlockPos pos, ServerLevel level) {
        CellarDebugInfo info = new CellarDebugInfo();
        info.pos = pos;
        info.space = spacesByPos.get(pos);
        info.outdoorTemp = Climate.getAverageTemperature(level, pos);
        info.totalTrackedSpaces = allSpaces.size();
        info.dirtySpacesQueue = dirtySpaces.size();
        info.dirtyContainersQueue = dirtyContainers.size();
        info.currentTick = level.getServer().getTickCount();

        // 扫描附近 10 格内的 ClimateReceiver
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos near = pos.offset(dx, dy, dz);
                    ClimateReceiver receiver = ClimateReceiver.get(level, near);
                    if (receiver != null) {
                        BlockState state = level.getBlockState(near);
                        String name = state.getBlock().getDescriptionId();
                        // 简化名称
                        if (name.contains("food_shelf")) name = "FoodShelf";
                        else if (name.contains("hanger")) name = "Hanger";
                        else if (name.contains("jarbnet")) name = "Jarbnet";
                        else if (name.contains("keg")) name = "Keg";
                        else if (name.contains("cheese")) name = "CheeseWheel";
                        else if (name.contains("wine")) name = "WineShelf";
                        else if (name.contains("vat")) name = "Vat";
                        else if (name.contains("planter")) name = "Planter";
                        else name = state.getBlock().getName().getString();

                        boolean climateValid = false;
                        if (receiver instanceof com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity fs) {
                            climateValid = fs.isClimateValid();
                        }
                        info.nearbyContainers.add(new CellarDebugInfo.ContainerInfo(near.immutable(), name, climateValid));
                    }
                }
            }
        }

        return info;
    }

    public String listAll() {
        StringBuilder sb = new StringBuilder();
        sb.append("======== CellarTracker: ").append(allSpaces.size()).append(" 个空间 ========\n");
        int i = 0;
        for (CellarSpace space : allSpaces) {
            i++;
            sb.append("  [").append(i).append("] ").append(space.seedPos.toShortString())
                .append(" valid=").append(space.valid)
                .append(" avgR=").append(String.format("%.2f", space.avgResistance))
                .append(" T=").append(String.format("%.1f", space.effectiveTemperature)).append("°C")
                .append(" interior=").append(space.interiorPositions.size())
                .append(" walls=").append(space.wallPositions.size())
                .append("\n");
        }
        if (allSpaces.isEmpty()) {
            sb.append("  (无已追踪空间)\n");
        }
        sb.append("dirtySpaces=").append(dirtySpaces.size())
            .append(" dirtyContainers=").append(dirtyContainers.size());
        return sb.toString();
    }

    // ===== 内部方法 =====

    private void updateSpace(ServerLevel level, CellarSpace oldSpace, CellarSpace newSpace) {
        if (newSpace != null && newSpace.valid) {
            // 移除旧映射
            for (BlockPos ip : oldSpace.interiorPositions) {
                spacesByPos.remove(ip);
            }
            // 注册新映射
            insertSpace(newSpace);
            broadcastToContainers(level, newSpace);
        } else {
            // 空间已失效
            invalidateSpace(level, oldSpace);
        }
    }

    private void insertSpace(CellarSpace space) {
        allSpaces.add(space);
        for (BlockPos ip : space.interiorPositions) {
            spacesByPos.put(ip, space);
        }
    }

    private void invalidateSpace(ServerLevel level, CellarSpace space) {
        space.invalidate();
        for (BlockPos ip : space.interiorPositions) {
            spacesByPos.remove(ip);
        }
        allSpaces.remove(space);
        broadcastToContainers(level, space); // 传播 invalid 状态
    }

    private void broadcastToContainers(ServerLevel level, CellarSpace space) {
        for (BlockPos interiorPos : space.interiorPositions) {
            ClimateReceiver receiver = ClimateReceiver.get(level, interiorPos);
            if (receiver != null) {
                receiver.setValid(level, interiorPos, space.valid, 0, ClimateType.CELLAR);
            }
        }
    }

    private void updateContainerState(ServerLevel level, BlockPos pos, CellarSpace space) {
        ClimateReceiver receiver = ClimateReceiver.get(level, pos);
        if (receiver != null) {
            boolean valid = space != null && space.valid;
            receiver.setValid(level, pos, valid, 0, ClimateType.CELLAR);
        }
    }

    private boolean isNearSpace(CellarSpace space, BlockPos pos, int margin) {
        // 先快速检查种子位置距离
        if (Math.abs(space.seedPos.getX() - pos.getX()) > margin * 4) return false;
        if (Math.abs(space.seedPos.getZ() - pos.getZ()) > margin * 4) return false;

        // 检查变更位置是否在空间墙壁或内部的扩展范围内
        for (BlockPos wallPos : space.wallPositions) {
            if (wallPos.distSqr(pos) <= margin * margin) return true;
        }
        return false;
    }

    private void cleanupInvalidSpaces(long currentTick) {
        long EXPIRY_TICKS = 12000; // 10 分钟
        allSpaces.removeIf(space -> {
            if (!space.valid && currentTick - space.lastCheckedTick > EXPIRY_TICKS) {
                for (BlockPos ip : space.interiorPositions) {
                    spacesByPos.remove(ip);
                }
                return true;
            }
            return false;
        });
    }
}
