package com.yukimods.firmalifehardcore.util;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 调试信息对象 — /firmalifehardcore cellar info 指令使用。
 */
public class CellarDebugInfo {

    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private static final DecimalFormat DF1 = new DecimalFormat("0.0");

    public BlockPos pos;
    public CellarSpace space;
    public float outdoorTemp;
    public List<ContainerInfo> nearbyContainers = new ArrayList<>();
    public int totalTrackedSpaces;
    public int dirtySpacesQueue;
    public int dirtyContainersQueue;
    public long currentTick;

    public record ContainerInfo(BlockPos pos, String name, boolean climateValid) {}

    /** 格式化为多行文本 */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n═══════════════════════════════════════════════════════\n");
        sb.append("  [Cellar] FirmaLifeHardCore — 地窖检测报告\n");
        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("  位置: ").append(pos.toShortString()).append("\n");

        if (space != null && space.valid) {
            sb.append("  地窖状态: [OK] 有效 (CellarTracker 已缓存)\n\n");

            sb.append("  [Wall] 墙体数据:\n");
            sb.append("     平均热阻: ").append(DF.format(space.avgResistance)).append(" / 1.00\n");
            sb.append("     墙体方块: ").append(space.totalWallBlocks()).append(" 个\n");
            if (space.totalWallBlocks() > 0) {
                sb.append("     HIGH (石/土/砖):  ").append(space.highCount)
                    .append(" 块 (").append(pct(space.highCount, space.totalWallBlocks())).append("%)\n");
                sb.append("     MEDIUM (木):        ").append(space.mediumCount)
                    .append(" 块 (").append(pct(space.mediumCount, space.totalWallBlocks())).append("%)\n");
                sb.append("     LOW (玻璃/金属):   ").append(space.lowCount)
                    .append(" 块 (").append(pct(space.lowCount, space.totalWallBlocks())).append("%)\n");
                sb.append("     未匹配:             ").append(space.unmatchedCount).append(" 块\n");
            }

            sb.append("\n  [Temp] 温度:\n");
            sb.append("     室外温度 (TFC):  ").append(DF1.format(outdoorTemp)).append("°C\n");
            sb.append("     地窖有效温度:    ").append(DF1.format(space.effectiveTemperature)).append("°C")
                .append("  (保温 ").append(Math.round(space.avgResistance * 100)).append("%)\n");

            sb.append("\n  [Door] 门:\n");
            sb.append("     普通门: ").append(space.doorCount > 0 ? space.doorCount : 0).append(" 扇\n");
            sb.append("     其中双层: ").append(space.doubleDoorCount).append(" 扇\n");

            sb.append("\n  [Level] 保鲜等级: ");
            if (space.avgResistance >= 0.70) {
                sb.append("SHELVED_3 (最佳防腐)");
            } else if (space.avgResistance >= 0.45) {
                sb.append("SHELVED_2 (中等防腐)");
            } else {
                sb.append("SHELVED (基础防腐)");
            }
            sb.append("\n");

        } else {
            sb.append("  地窖状态: [FAIL] 无效\n");
            sb.append("  原因: ");
            if (space == null) {
                sb.append("未检测到封闭空间");
            } else {
                sb.append("平均热阻 ").append(DF.format(space.avgResistance)).append(" 低于阈值");
            }
            sb.append("\n");
        }

        if (!nearbyContainers.isEmpty()) {
            sb.append("\n  [Cellar] 范围内容器 (10 格):\n");
            for (var ci : nearbyContainers) {
                sb.append("     ").append(ci.pos().toShortString())
                    .append(" ").append(ci.name())
                    .append("    — 地窖有效: ").append(ci.climateValid() ? "[OK]" : "[FAIL]").append("\n");
            }
        }

        sb.append("\n  [Track] 追踪状态:\n");
        sb.append("     已追踪地窖空间数: ").append(totalTrackedSpaces).append("\n");
        sb.append("     待重检空间队列: ").append(dirtySpacesQueue).append("\n");
        sb.append("     待检容器队列: ").append(dirtyContainersQueue).append("\n");
        if (space != null && space.valid) {
            long ticksAgo = currentTick - space.lastCheckedTick;
            sb.append("     最后检测: tick ").append(space.lastCheckedTick)
                .append(" (").append(ticksAgo / 20).append("s 前)\n");
        }

        sb.append("═══════════════════════════════════════════════════════");
        return sb.toString();
    }

    private static int pct(int part, int total) {
        if (total == 0) return 0;
        return Math.round(100f * part / total);
    }
}
