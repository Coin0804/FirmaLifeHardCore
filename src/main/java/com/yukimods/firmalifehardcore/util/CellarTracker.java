package com.yukimods.firmalifehardcore.util;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
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

    /** 待发现新空间的位置 */
    private final Deque<BlockPos> pendingDiscoveries = new ArrayDeque<>();

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
            markDirty(pos);
        }

        // 1. 重检脏空间 — 从原种子重跑 BFS（不用 detectAll，只需检测一个种子）
        int spacesProcessed = 0;
        Iterator<CellarSpace> spaceIt = dirtySpaces.iterator();
        while (spaceIt.hasNext() && spacesProcessed < maxSpaces) {
            CellarSpace oldSpace = spaceIt.next();
            spaceIt.remove();
            spacesProcessed++;

            // 种子未加载 → 信息不完整，保持旧空间不变
            if (!level.isLoaded(oldSpace.seedPos)) continue;

            CellarDetector.DetectResult result = CellarDetector.detectFromSeed(level, oldSpace.seedPos);
            if (result.interrupted) {
                // BFS 被未加载 chunk 中断 —— 信息不完整，保持旧空间
                continue;
            }
            CellarSpace newSpace = result.space;
            if (newSpace != null && newSpace.valid && !newSpace.receiverPositions.isEmpty()) {
                replaceSpace(level, oldSpace, newSpace);
            } else if (newSpace != null && newSpace.valid) {
                // BFS 成功但无 receiver → 碎片，销毁
                FirmaLifeHardCore.LOGGER.info("[CellarTracker] Destroying fragment at {} interior={}",
                    oldSpace.seedPos.toShortString(), newSpace.interiorPositions.size());
                invalidateSpace(level, oldSpace);
            } else {
                // newSpace == null —— BFS 真正失败，销毁
                invalidateSpace(level, oldSpace);
            }
        }

        // 2. 处理待发现位置 — 对不在已知空间附近的尝试发现新空间
        while (!pendingDiscoveries.isEmpty()) {
            BlockPos pos = pendingDiscoveries.poll();
            if (pos == null) continue;

            if (hasAdjacentSpace(pos)) continue;

            List<CellarSpace> results = CellarDetector.detectAll(level, pos);
            for (CellarSpace ns : results) {
                insertSpace(ns);
                FirmaLifeHardCore.LOGGER.info("[CellarTracker] Discovered new space at {} interior={} receivers={}",
                    ns.seedPos.toShortString(), ns.interiorPositions.size(), ns.receiverPositions.size());
                broadcastToContainers(level, ns);
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
    public void markDirty(BlockPos changedPos) {
        // int maxHorizSpan = FirmaLifeHardCoreConfig.SERVER.maxHorizontalSpan.get();
        // int nearCount = 0;
        // 标记相关 CellarSpace 重检
        for (CellarSpace space : allSpaces) {
            if (isNearSpace(space, changedPos)) {
                dirtySpaces.add(space);
                // nearCount++;
            }
        }

        // 标记变更位置本身（作为潜在新空间种子）
        pendingDiscoveries.add(changedPos.immutable());

    }

    /** 清空所有已追踪空间（调试指令用） */
    public void clearAll(ServerLevel level) {
        for (CellarSpace space : new ArrayList<>(allSpaces)) {
            invalidateSpace(level, space);
        }
        dirtySpaces.clear();
        pendingDiscoveries.clear();
        delayedBreaks.clear();
    }

    /** 强制重算（调试指令用） */
    public void forceRecalc(BlockPos pos, ServerLevel level) {
        List<CellarSpace> results = CellarDetector.detectAll(level, pos);
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
            else updateContainerAt(level, pos, false, 0, ClimateType.CELLAR);
        }
    }

    // ===== 查询 =====

    /** 查询位置是否在地窖中（含内部障碍物位置如食物架）。返回 CellarSpace 或 null。 */
    @Nullable
    public CellarSpace query(BlockPos pos) {
        CellarSpace space = spacesByPos.get(pos);
        if (space == null) {
            for (CellarSpace s : allSpaces) {
                if (s.valid && (s.obstaclePositions.contains(pos) || s.receiverPositions.contains(pos))) {
                    space = s;
                    break;
                }
            }
        }
        if (space != null && space.valid) {
            return space;
        }
        return null;
    }

    // ===== 调试 =====

    public CellarDebugInfo getDebugInfo(BlockPos pos, ServerLevel level) {
        CellarDebugInfo info = new CellarDebugInfo();
        info.pos = pos;
        info.space = spacesByPos.get(pos);
        // 直接调 ClimateModel.getInstantTemperature，绕过自己的 ClimateMixin
        ICalendar cal = Calendars.get(level);
        info.outdoorTemp = Climate.get(level).getInstantTemperature(
            level, pos, cal.getCalendarTicks(), cal.getCalendarDaysInMonth());
        if (info.space != null && info.space.valid) {
            info.indoorTemp = info.space.getEffectiveTemperature(level);
        }
        info.totalTrackedSpaces = allSpaces.size();
        info.dirtySpacesQueue = dirtySpaces.size();
        info.pendingDiscoveriesQueue = pendingDiscoveries.size();
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

                        var r = query(near);
                        boolean climateValid = r != null;
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
                .append(space.isGreenhouse() ? " 温室" : " 地窖")
                .append(" valid=").append(space.valid)
                .append(" avgR=").append(String.format("%.2f", space.avgResistance))
                .append(" canopy=").append(String.format("%.0f%%", space.canopyRatio * 100))
                .append(" base=").append(String.format("%.1f", space.getBaseTemperature())).append("°C")
                .append(" interior=").append(space.interiorPositions.size())
                .append(" walls=").append(space.wallPositions.size())
                .append("\n");
        }
        if (allSpaces.isEmpty()) {
            sb.append("  (无已追踪空间)\n");
        }
        sb.append("dirtySpaces=").append(dirtySpaces.size())
            .append(" pendingDiscoveries=").append(pendingDiscoveries.size());
        return sb.toString();
    }

    // ===== 内部方法 =====

    /** 替换一个已有空间（重新 floodfill 后） */
    private void replaceSpace(ServerLevel level, CellarSpace oldSpace, CellarSpace newSpace) {
        // 通知旧空间中不在新空间内的容器失效（如隔墙后的隔离侧容器）
        for (BlockPos rp : oldSpace.receiverPositions) {
            if (!newSpace.receiverPositions.contains(rp)) {
                updateContainerAt(level, rp, false, 0, ClimateType.CELLAR);
                updateContainerAt(level, rp, false, 0, ClimateType.GREENHOUSE);
            }
        }
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
        // 拆分检测：旧空间有但新空间没有的 interior → 拆墙后另一半，取一个种子加入待发现
        if (merged.isEmpty()) {
            for (BlockPos oldIp : oldSpace.interiorPositions) {
                if (!newSpace.interiorPositions.contains(oldIp)) {
                    pendingDiscoveries.add(oldIp);
                    break;
                }
            }
        }
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
        for (BlockPos rp : space.receiverPositions) {
            if (!level.isLoaded(rp)) continue;  // chunk 未加载，跳过
            updateContainerAt(level, rp, false, 0, ClimateType.CELLAR);
            updateContainerAt(level, rp, false, 0, ClimateType.GREENHOUSE);
        }
        space.invalidate();
    }

    /** 通知单个位置的 ClimateReceiver */
    private void updateContainerAt(ServerLevel level, BlockPos pos, boolean valid, int tier, ClimateType climate) {
        ClimateReceiver receiver = ClimateReceiver.get(level, pos);
        if (receiver != null) {
            receiver.setValid(level, pos, valid, tier, climate);
        }
    }

    /** 广播给空间内所有 ClimateReceiver */
    private void broadcastToContainers(ServerLevel level, CellarSpace space) {
        int tier = CellarInventoryHelper.tierFromTemperature(space.getEffectiveTemperature(level));
        boolean effective = space.valid && tier > 0; // tier=0 视为无效地窖
        for (BlockPos rp : space.receiverPositions) {
            if (!level.isLoaded(rp)) continue;  // chunk 未加载，跳过
            updateContainerAt(level, rp, effective, tier, ClimateType.CELLAR);
            updateContainerAt(level, rp, space.isGreenhouse(), Integer.MAX_VALUE, ClimateType.GREENHOUSE);
        }
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

    private boolean isNearSpace(CellarSpace space, BlockPos pos) {
        int maxHorizSpan = FirmaLifeHardCoreConfig.SERVER.maxHorizontalSpan.get();
        int maxVertSpan = FirmaLifeHardCoreConfig.SERVER.maxVerticalSpan.get();
        return Math.abs(space.seedPos.getX() - pos.getX()) <= maxHorizSpan * 2
            && Math.abs(space.seedPos.getY() - pos.getY()) <= maxVertSpan * 2
            && Math.abs(space.seedPos.getZ() - pos.getZ()) <= maxHorizSpan * 2;
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
            st.putFloat("canopyRatio", space.canopyRatio);
            st.putLong("lastTick", space.lastCheckedTick);
            st.put("interior", posSetToTag(space.interiorPositions));
            st.put("walls", posSetToTag(space.wallPositions));
            st.put("obstacles", posSetToTag(space.obstaclePositions));
            st.put("receivers", posSetToTag(space.receiverPositions));
            list.add(st);
        }
        tag.put(DATA_KEY, list);
    }

    public void load(ServerLevel level, CompoundTag tag) {
        ListTag list = tag.getList(DATA_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag st = list.getCompound(i);
                BlockPos seed = BlockPos.of(st.getLong("seed"));
                CellarSpace space = new CellarSpace(seed);
                space.avgResistance = st.getFloat("avgR");
                space.canopyRatio = st.getFloat("canopyRatio");
                space.lastCheckedTick = st.getLong("lastTick");
                tagToPosSet(st.getList("interior", Tag.TAG_LONG), space.interiorPositions);
                tagToPosSet(st.getList("walls", Tag.TAG_LONG), space.wallPositions);
                tagToPosSet(st.getList("obstacles", Tag.TAG_LONG), space.obstaclePositions);
                tagToPosSet(st.getList("receivers", Tag.TAG_LONG), space.receiverPositions);
                CellarDetector.calculateThermal(level, space);
                space.valid = true;
                insertSpace(space);
            } catch (Exception e) {
                FirmaLifeHardCore.LOGGER.warn("[CellarTracker] Failed to load space #{}: {}", i, e.toString());
            }
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
