package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.HashSet;
import java.util.Set;

/**
 * 单个地窖空间数据对象。
 * 由 CellarDetector 创建，CellarTracker 管理。
 */
public class CellarSpace {

    /** 内部空间所有位置 */
    public final Set<BlockPos> interiorPositions = new HashSet<>();

    /** 墙体方块位置 */
    public final Set<BlockPos> wallPositions = new HashSet<>();

    /** 内部障碍物位置（岩石、泥土等纯障碍物——不含 ClimateReceiver） */
    public final Set<BlockPos> obstaclePositions = new HashSet<>();

    /** ClimateReceiver 位置（食品架、风干架、大缸、洒水头、种植盆、奶酪轮等需要通知的方块） */
    public final Set<BlockPos> receiverPositions = new HashSet<>();

    /** 种子位置（用于重检测的 floodfill 起点） */
    public BlockPos seedPos;

    /** 平均热阻 (0~1) */
    public float avgResistance;

    /** 棚顶比例 (0~1) */
    public float canopyRatio;

    public boolean isGreenhouse() {
        return canopyRatio >= FirmaLifeHardCoreConfig.SERVER.greenhouseGlassRatio.get().floatValue();
    }

    public float getBaseTemperature() {
        if (!isGreenhouse()) return 4f;
        return 4f + FirmaLifeHardCoreConfig.SERVER.greenhouseCanopyMultiplier.get().floatValue() * canopyRatio;
    }

    /**
     * 室内即时温度：基准 + (室外即时 - 基准) × (1 - 平均热阻)
     * 走 ClimateModel 实例方法获取室外温度，绕过 ClimateMixin。
     */
    public float getEffectiveTemperature(Level level) {
        var cal = Calendars.get(level);
        float outdoor = Climate.get(level).getInstantTemperature(
            level, seedPos, cal.getCalendarTicks(), cal.getCalendarDaysInMonth());
        float base = getBaseTemperature();
        return base + (outdoor - base) * (1f - Math.min(1f, avgResistance));
    }

    /**
     * 室内平均温度：基准 + (室外年均 - 基准) × (1 - 平均热阻)
     * 用于替代 Climate.getAverageTemperature()，判断植物气候适宜度。
     */
    public float getAverageTemperature(Level level) {
        float outdoorAvg = Climate.get(level).getAverageTemperature(level, seedPos);
        float base = getBaseTemperature();
        return base + (outdoorAvg - base) * (1f - Math.min(1f, avgResistance));
    }

    /** 墙体统计 */
    public int highCount, mediumCount, lowCount, unmatchedCount;
    /** 门统计 */
    public int doorCount, doubleDoorCount;

    /** 上次成功检测的 tick */
    public long lastCheckedTick;

    /** 当前是否有效 */
    public boolean valid;

    public CellarSpace(BlockPos seedPos) {
        this.seedPos = seedPos;
    }

    /** 热阻容器的总方块数 */
    public int totalWallBlocks() {
        return highCount + mediumCount + lowCount + unmatchedCount;
    }

    /** 使空间失效 */
    public void invalidate() {
        this.valid = false;
        this.avgResistance = 0f;
        this.canopyRatio = 0f;
        this.interiorPositions.clear();
        this.wallPositions.clear();
        this.obstaclePositions.clear();
        this.receiverPositions.clear();
    }
}
