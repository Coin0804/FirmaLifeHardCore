package com.yukimods.firmalifehardcore.util;

import com.eerussianguy.firmalife.common.blockentities.PumpingStationBlockEntity;
import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 水泵 tick 管理器——维护已加载泵集合，每 tick 注水。
 */
public final class PumpTickManager {

    private static final Set<PumpingStationBlockEntity> LOADED = ConcurrentHashMap.newKeySet();
    private static Method INTERNAL_FILL;
    private static Method INVALIDATE_CACHE;

    static {
        try {
            INTERNAL_FILL = PumpingStationBlockEntity.class.getMethod("firmalifehardcore$internalFill", int.class);
            INVALIDATE_CACHE = PumpingStationBlockEntity.class.getDeclaredMethod("firmalifehardcore$invalidateTankCache");
            INVALIDATE_CACHE.setAccessible(true);
        } catch (NoSuchMethodException e) {
            FirmaLifeHardCore.LOGGER.error("[PumpTickManager] mixin method not found", e);
        }
    }

    public static void register(PumpingStationBlockEntity pump) { LOADED.add(pump); }

    public static void unregister(PumpingStationBlockEntity pump) { LOADED.remove(pump); }

    /**
     * 方块变更时调用——从变更位置向下扫描找泵，invalidate 水箱缓存。
     * 向下穿过水箱直到找到泵，或超过 maxTankBonus+1 格放弃。
     */
    public static void notifyBlockChanged(LevelAccessor level, BlockPos changedPos) {
        if (INVALIDATE_CACHE == null) return;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(changedPos);
        int limit = FirmaLifeHardCoreConfig.SERVER.maxTankBonus.get() + 1;
        for (int i = 0; i < limit; i++) {
            if (!(level.getBlockEntity(cursor) instanceof PumpingStationBlockEntity)) {
                cursor.move(0, -1, 0);
                continue;
            }
            try {
                INVALIDATE_CACHE.invoke(level.getBlockEntity(cursor));
            } catch (Exception ignored) { }
            return;
        }
    }

    public static void tickAll(Level level) {
        if (INTERNAL_FILL == null) return;
        for (PumpingStationBlockEntity self : LOADED) {
            // LevelTickEvent 对每个已加载维度触发，只注水本维度的泵（否则多维度重复注水）
            if (self.getLevel() != level) continue;
            if (self.getLevel() == null || self.isRemoved() || !self.getLevel().isLoaded(self.getBlockPos())) continue;
            // 每 100 tick 保底重扫水箱（事件驱动为主，此为保证）
            if (INVALIDATE_CACHE != null && (self.getLevel().getGameTime() + self.getBlockPos().getZ()) % 100 == 0) {
                try { INVALIDATE_CACHE.invoke(self); } catch (Exception ignored) { }
            }
            if (!self.isPumping()) continue;
            // 每 80 tick 注水一次，按 Z 坐标错峰——与洒水器浇水同周期，15 rpm 时与 5 洒水器扣水精确平衡
            if ((self.getLevel().getGameTime() + self.getBlockPos().getZ()) % 80 != 0) continue;
            var rotation = self.getRotationNode().rotation();
            if (rotation == null) continue;
            float speed = rotation.positiveSpeed();
            // 一次补 80 tick 的量，避免 (int) 截断
            int amount = (int) (speed * FirmaLifeHardCoreConfig.SERVER.pumpRateFactor.get().floatValue() * 80.0f);
            if (amount <= 0) continue;
            try {
                INTERNAL_FILL.invoke(self, amount);
            } catch (Exception ignored) { }
        }
    }
}
