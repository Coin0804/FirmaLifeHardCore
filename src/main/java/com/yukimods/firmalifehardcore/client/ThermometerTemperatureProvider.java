package com.yukimods.firmalifehardcore.client;

import net.dries007.tfc.common.blocks.devices.ThermometerBlock;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.config.TemperatureDisplayStyle;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 温度计 Jade tooltip：
 * - 室内温度 != 室外温度（地窖/温室生效）→ 显示"室内温度"
 * - 室内温度 == 室外温度（户外）→ 不发送数据，让 TFC 原版 tooltip 正常显示
 */
public enum ThermometerTemperatureProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "thermometer_temperature");

    // ===== 服务端 =====

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        var pos = accessor.getPosition();
        var state = accessor.getBlockState();

        if (!(state.getBlock() instanceof ThermometerBlock)) return;
        if (state.getValue(net.dries007.tfc.common.blocks.TFCBlockStateProperties.THERMOMETER_ATTACHED)) return;

        // 室内温度（走 ClimateMixin → 温室/地窖修正）
        float indoorTemp = Climate.getInstantTemperature(level, pos);
        // 室外温度（走 ClimateModel 实例方法，绕过 ClimateMixin）
        var cal = Calendars.get(level);
        float outdoorTemp = Climate.get(level).getInstantTemperature(level, pos, cal.getCalendarTicks(), cal.getCalendarDaysInMonth());
        // 温度有差异才显示室内温度，否则交给 TFC 原版 tooltip
        if (Math.abs(indoorTemp - outdoorTemp) < 0.01f) return;

        tag.putFloat("firmalifehardcore:thermometer_temp", indoorTemp);
    }

    // ===== 客户端 =====

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (!tag.contains("firmalifehardcore:thermometer_temp")) return;

        float temp = tag.getFloat("firmalifehardcore:thermometer_temp");
        TemperatureDisplayStyle style = TFCConfig.CLIENT.climateTooltipStyle.get();
        var comp = style.formatRange(temp);
        if (comp != null) {
            tooltip.add(Component.translatable("jade.firmalifehardcore.indoor_temperature", comp));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
