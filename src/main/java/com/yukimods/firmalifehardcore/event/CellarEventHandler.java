package com.yukimods.firmalifehardcore.event;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import com.yukimods.firmalifehardcore.util.CellarSavedData;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.yukimods.firmalifehardcore.util.ThermalConductivity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.nbt.CompoundTag;

public class CellarEventHandler {

    /** 每 tick 推进延迟队列 + 处理脏标记 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            CellarTracker tracker = CellarAttachment.get(sl);
            if (tracker != null) tracker.tick(sl);
        }
    }

    /** 世界保存时持久化所有空间 */
    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            CellarTracker tracker = CellarAttachment.get(sl);
            if (tracker != null) {
                CompoundTag tag = new CompoundTag();
                tracker.save(tag);
                CellarSavedData.get(sl).setCellarData(tag);
            }
        }
    }

    /** 世界加载时恢复 */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            CellarTracker tracker = CellarAttachment.get(sl);
            if (tracker != null) {
                CompoundTag data = CellarSavedData.get(sl).getCellarData();
                if (data.contains("firmalifehardcore_cellar_tracker"))
                    tracker.load(sl, data);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState state = event.getPlacedBlock();
        FirmaLifeHardCore.LOGGER.debug("[EventHandler] PLACE pos={} block={} relevant={}",
            event.getPos().toShortString(), state.getBlock().getDescriptionId(), isRelevantBlock(state));
        if (isRelevantBlock(state)) trigger(event.getLevel(), event.getPos(), state, "PLACE");
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState oldState = event.getState();
        FirmaLifeHardCore.LOGGER.debug("[EventHandler] BREAK pos={} block={} relevant={}",
            event.getPos().toShortString(), oldState.getBlock().getDescriptionId(), isRelevantBlock(oldState));
        if (!isRelevantBlock(oldState)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        CellarTracker tracker = CellarAttachment.get(serverLevel);
        if (tracker != null) tracker.scheduleBreak(event.getPos().immutable());
    }

    private static boolean isRelevantBlock(BlockState state) {
        return ThermalConductivity.isRelevant(state)
            || ThermalConductivity.isDoor(state)
            || state.getBlock() instanceof ClimateReceiver
            || state.is(ThermalConductivity.TAG_PLANTERS);
    }

    private static void trigger(LevelAccessor lv, BlockPos pos, BlockState state, String action) {
        if (!(lv instanceof ServerLevel serverLevel)) return;

        CellarTracker tracker = CellarAttachment.get(serverLevel);
        if (tracker == null) return;

        FirmaLifeHardCore.LOGGER.debug("[EventHandler] trigger {} pos={} trackerSpaces={}",
            action, pos.toShortString(), tracker.spaceCount());
        tracker.markDirty(pos, FirmaLifeHardCoreConfig.SERVER.scanRadius.get());
    }
}
