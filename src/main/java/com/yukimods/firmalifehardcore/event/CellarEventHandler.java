package com.yukimods.firmalifehardcore.event;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import com.yukimods.firmalifehardcore.util.ThermalConductivity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 方块变更事件监听 — 事件驱动地窖重检。
 * 不轮询，仅在方块放置/破坏/门开关时触发。
 */
public class CellarEventHandler {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        handleBlockChange(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        handleBlockChange(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        // 仅处理门/活板门的邻居通知（开关门导致 OPEN 状态变化）
        BlockState state = event.getState();
        if (ThermalConductivity.isDoor(state)) {
            handleBlockChange(event.getLevel(), event.getPos());
        }
    }

    private static void handleBlockChange(Level level, net.minecraft.core.BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.isClientSide()) return;

        // O(1) 过滤：当前方块状态是否相关
        BlockState state = level.getBlockState(pos);
        if (!ThermalConductivity.isRelevant(state) && !ThermalConductivity.isDoor(state)) {
            return; // 99%+ 事件在此跳过
        }

        CellarTracker tracker = CellarAttachment.get(serverLevel);
        if (tracker == null) return;

        int scanRadius = FirmaLifeHardCoreConfig.SERVER.scanRadius.get();
        tracker.markDirty(pos, scanRadius);
        tracker.tick(serverLevel); // 立即处理（也可延迟到 LevelTickEvent）
    }
}
