package com.yukimods.firmalifehardcore.util;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.dries007.tfc.util.climate.Climate;
import org.jetbrains.annotations.Nullable;

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

    /** 延迟处理的破坏事件 pos → 剩余tick（相同位置重复破坏刷新为5） */
    private final Map<BlockPos, Integer> delayedBreaks = new HashMap<>();
    private static final int BREAK_DELAY = 5;

    /** 上次清理无效空间的时间 */
    private long lastCleanupTick = 0;
    private static final long CLEANUP_INTERVAL = 6000;

    // ===== Tick 处理 =====

    /**
     * 每 tick 限量处理：先重检脏空间，再处理待检位置（自动发现新空间，更新容器状态）。
     */
    /** 调度一个延迟的破坏重检（5 tick 后，重复破坏刷新计时） */
    public void scheduleBreak(BlockPos pos) {
        delayedBreaks.put(pos, BREAK_DELAY);
    }

    public void tick(ServerLevel level) {
        int maxSpaces = FirmaLifeHardCoreConfig.SERVER.maxSpacesPerTick.get();
        int maxPositions = FirmaLifeHardCoreConfig.SERVER.maxContainersPerTick.get();
        int scanRadius = FirmaLifeHardCoreConfig.SERVER.scanRadius.get();
        long currentTick = level.getServer().getTickCount();

        // 0. 处理延迟的破坏事件：倒计时 → 到 0 的转为正式标记
        List<BlockPos> ready = new ArrayList<>();
        delayedBreaks.entrySet().removeIf(e -> {
            int remaining = e.getValue() - 1;
            if (remaining <= 0) { ready.add(e.getKey()); return true; }
            e.setValue(remaining);
            return false;
        });
        for (BlockPos pos : ready) {
            markDirty(pos, scanRadius);
        }

        // 1. 重检脏空间 — 从原种子重跑 BFS（不用 detectAll，只需检测一个种子）
        int spacesProcessed = 0;
        Iterator<CellarSpace> spaceIt = dirtySpaces.iterator();
        while (spaceIt.hasNext() && spacesProcessed < maxSpaces) {
            CellarSpace oldSpace = spaceIt.next();
            spaceIt.remove();
            spacesProcessed++;

            CellarSpace newSpace = CellarDetector.detectFromSeed(level, oldSpace.seedPos, scanRadius);
            if (newSpace != null && newSpace.valid) {
                replaceSpace(level, oldSpace, newSpace);
            } else {
                invalidateSpace(level, oldSpace);
            }
        }

        // 2. 处理待检位置 — 只对不在已知空间附近的尝试发现新空间
        int positionsProcessed = 0;
        while (!dirtyContainers.isEmpty() && positionsProcessed < maxPositions) {
            BlockPos pos = dirtyContainers.poll();
            if (pos == null) continue;
            positionsProcessed++;

            if (hasAdjacentSpace(pos)) continue;

            List<CellarSpace> results = CellarDetector.detectAll(level, pos, scanRadius);
            if (!results.isEmpty()) {
                for (CellarSpace ns : results) {
                    insertSpace(ns);
                    broadcastToContainers(level, ns);
                }
            } else {
                updateContainerAt(level, pos, false, 0);
            }
        }

        // 3. 定期健康检查 — tick%100 作为索引，每 tick 重检一个空间
        int idx = (int) (currentTick % 100);
        if (idx < allSpaces.size()) {
            int i = 0;
            for (CellarSpace s : allSpaces) { if (i++ == idx) { dirtySpaces.add(s); break; } }
        }

        // 4. 定期清理长期无效的空间
        if (currentTick - lastCleanupTick > CLEANUP_INTERVAL) {
            cleanupDeadSpaces(currentTick);
            lastCleanupTick = currentTick;
        }
    }

    // ===== 标记脏 =====

    /**
     * 方块变更时调用。仅在变更影响区域存在容器时才将对应位置加入待检队列。
     */
    public void markDirty(BlockPos changedPos, int scanRadius) {
        // 标记相关 CellarSpace 重检
        for (CellarSpace space : allSpaces) {
            if (isNearSpace(space, changedPos, scanRadius)) {
                dirtySpaces.add(space);
            }
        }

        // 标记变更位置本身（作为潜在新空间种子）
        dirtyContainers.add(changedPos.immutable());
    }

    /** 强制重算（调试指令用） */
    public void forceRecalc(BlockPos pos, ServerLevel level) {
        int scanRadius = FirmaLifeHardCoreConfig.SERVER.scanRadius.get();
        List<CellarSpace> results = CellarDetector.detectAll(level, pos, scanRadius);
        if (!results.isEmpty()) {
            // 移除旧的重叠空间
            for (CellarSpace ns : results) {
                for (BlockPos ip : ns.interiorPositions) {
                    CellarSpace old = spacesByPos.remove(ip);
                    if (old != null) allSpaces.remove(old);
                }
                insertSpace(ns);
                broadcastToContainers(level, ns);
            }
        } else {
            CellarSpace existing = spacesByPos.get(pos);
            if (existing != null) invalidateSpace(level, existing);
            else updateContainerAt(level, pos, false, 0);
        }
    }

    // ===== 查询 =====

    /** 查询位置是否在地窖中（含内部障碍物位置如食物架） */
    @Nullable
    public CellarSpace.CellarResult query(BlockPos pos) {
        CellarSpace space = spacesByPos.get(pos);
        if (space == null) {
            for (CellarSpace s : allSpaces)
                if (s.valid && s.obstaclePositions.contains(pos)) { space = s; break; }
        }
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

    /** 替换一个已有空间（重新 floodfill 后） */
    private void replaceSpace(ServerLevel level, CellarSpace oldSpace, CellarSpace newSpace) {
        for (BlockPos ip : oldSpace.interiorPositions) spacesByPos.remove(ip);
        allSpaces.remove(oldSpace);
        // 合并：新空间可能吞并了其他空间（如拆墙后两室合一），移除重叠的旧空间
        List<CellarSpace> merged = new ArrayList<>();
        for (CellarSpace other : allSpaces) {
            for (BlockPos nip : newSpace.interiorPositions) {
                if (other.interiorPositions.contains(nip)) { merged.add(other); break; }
            }
        }
        for (CellarSpace m : merged) {
            for (BlockPos ip : m.interiorPositions) spacesByPos.remove(ip);
            allSpaces.remove(m);
        }
        insertSpace(newSpace);
        broadcastToContainers(level, newSpace);
    }

    private void insertSpace(CellarSpace space) {
        allSpaces.add(space);
        for (BlockPos ip : space.interiorPositions) {
            spacesByPos.put(ip, space);
        }
    }

    private void invalidateSpace(ServerLevel level, CellarSpace space) {
        for (BlockPos ip : space.interiorPositions) spacesByPos.remove(ip);
        allSpaces.remove(space);
        for (BlockPos ip : space.interiorPositions) updateContainerAt(level, ip, false, 0);
        for (BlockPos op : space.obstaclePositions) updateContainerAt(level, op, false, 0);
        space.invalidate();
    }

    /** 通知单个位置的 ClimateReceiver */
    /** 根据热阻计算保鲜等级：0=SHELVED, 1=SHELVED_2, 2=SHELVED_3 */
    private static int tierFromResistance(float avgR) {
        if (avgR >= FirmaLifeHardCoreConfig.SERVER.level3ResistanceThreshold.get()) return 2;
        if (avgR >= FirmaLifeHardCoreConfig.SERVER.level2ResistanceThreshold.get()) return 1;
        return 0;
    }

    private void updateContainerAt(ServerLevel level, BlockPos pos, boolean valid, int tier) {
        ClimateReceiver receiver = ClimateReceiver.get(level, pos);
        if (receiver != null) {
            receiver.setValid(level, pos, valid, tier, ClimateType.CELLAR);
        }
    }

    /** 广播给空间内所有 ClimateReceiver（含内部障碍物位置中的容器） */
    private void broadcastToContainers(ServerLevel level, CellarSpace space) {
        int tier = tierFromResistance(space.avgResistance);
        for (BlockPos ip : space.interiorPositions)
            updateContainerAt(level, ip, space.valid, tier);
        for (BlockPos op : space.obstaclePositions)
            updateContainerAt(level, op, space.valid, tier);
    }

    /** 检查位置是否在任何空间的墙集中 */
    public boolean isInAnyWall(BlockPos pos) {
        for (CellarSpace space : allSpaces)
            if (space.wallPositions.contains(pos)) return true;
        return false;
    }
    public int spaceCount() { return allSpaces.size(); }
    public int dirtySpaceCount() { return dirtySpaces.size(); }

    /** 检查该位置 ±1 范围内是否已有已知空间 */
    private boolean hasAdjacentSpace(BlockPos pos) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (spacesByPos.containsKey(m)) return true;
                }
        return false;
    }

    private boolean isNearSpace(CellarSpace space, BlockPos pos, int margin) {
        return Math.abs(space.seedPos.getX() - pos.getX()) <= margin * 2
            && Math.abs(space.seedPos.getY() - pos.getY()) <= margin * 2
            && Math.abs(space.seedPos.getZ() - pos.getZ()) <= margin * 2;
    }

    /** 清理长期无效的空间 */
    private void cleanupDeadSpaces(long currentTick) {
        long EXPIRY_TICKS = 12000;
        allSpaces.removeIf(space -> {
            if (!space.valid && currentTick - space.lastCheckedTick > EXPIRY_TICKS) {
                for (BlockPos ip : space.interiorPositions) spacesByPos.remove(ip);
                return true;
            }
            return false;
        });
    }

    // ===== 持久化 =====

    private static final String DATA_KEY = "firmalifehardcore_cellar_tracker";

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (CellarSpace space : allSpaces) {
            if (!space.valid) continue;
            CompoundTag st = new CompoundTag();
            st.putLong("seed", space.seedPos.asLong());
            st.putFloat("avgR", space.avgResistance);
            st.putLong("lastTick", space.lastCheckedTick);
            st.put("interior", posSetToTag(space.interiorPositions));
            st.put("walls", posSetToTag(space.wallPositions));
            st.put("obstacles", posSetToTag(space.obstaclePositions));
            list.add(st);
        }
        tag.put(DATA_KEY, list);
    }

    public void load(ServerLevel level, CompoundTag tag) {
        ListTag list = tag.getList(DATA_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag st = list.getCompound(i);
            BlockPos seed = BlockPos.of(st.getLong("seed"));
            CellarSpace space = new CellarSpace(seed);
            space.avgResistance = st.getFloat("avgR");
            space.lastCheckedTick = st.getLong("lastTick");
            tagToPosSet(st.getList("interior", Tag.TAG_LONG), space.interiorPositions);
            tagToPosSet(st.getList("walls", Tag.TAG_LONG), space.wallPositions);
            tagToPosSet(st.getList("obstacles", Tag.TAG_LONG), space.obstaclePositions);
            // 重新计算温度和有效性
            CellarDetector.calculateThermal(level, space);
            space.valid = space.avgResistance >= FirmaLifeHardCoreConfig.SERVER.minThermalResistance.get();
            insertSpace(space);
        }
    }

    private static ListTag posSetToTag(Set<BlockPos> set) {
        ListTag list = new ListTag();
        for (BlockPos p : set) list.add(net.minecraft.nbt.LongTag.valueOf(p.asLong()));
        return list;
    }

    private static void tagToPosSet(ListTag list, Set<BlockPos> set) {
        for (int i = 0; i < list.size(); i++)
            set.add(BlockPos.of(((net.minecraft.nbt.LongTag) list.get(i)).getAsLong()));
    }
}
