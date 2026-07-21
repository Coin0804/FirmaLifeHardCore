package com.yukimods.firmalifehardcore.util;

import net.minecraft.core.BlockPos;
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

    /** 种子位置（用于重检测的 floodfill 起点） */
    public BlockPos seedPos;

    /** 平均热阻 (0~1) */
    public float avgResistance;

    /** 地窖有效温度 */
    public float effectiveTemperature;

    /** 墙体统计 */
    public int highCount, mediumCount, lowCount, unmatchedCount;

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
        this.effectiveTemperature = 0f;
        this.interiorPositions.clear();
        this.wallPositions.clear();
    }

    /** 检测结果记录 — 不可变 */
    public record CellarResult(float avgResistance, float effectiveTemperature, boolean valid) {}
}
