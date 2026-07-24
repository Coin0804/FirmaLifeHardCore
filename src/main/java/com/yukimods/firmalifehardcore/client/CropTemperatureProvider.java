package com.yukimods.firmalifehardcore.client;

import net.dries007.tfc.common.blocks.crop.ICropBlock;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.config.TemperatureDisplayStyle;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 在 TFC 作物的 Jade tooltip 中显示地窖/温室有效温度及温度适宜度。
 *
 * <p>服务端: 查询 Climate.getInstantTemperature()（经 ClimateMixin 覆盖），
 * 并通过 ICropBlock.getClimateRange() 判断温度是否适宜。
 * <p>客户端: 读取 Jade 同步的 tag，用 TFC TemperatureDisplayStyle 格式化显示。
 */
public enum CropTemperatureProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "crop_temperature");

    // ===== 服务端 =====

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        var pos = accessor.getPosition();
        var state = accessor.getBlockState();

        if (!(state.getBlock() instanceof ICropBlock cropBlock)) return;

        ClimateRange range = cropBlock.getClimateRange();
        // 走 Mixin 的有效温度 vs 直调 ClimateModel 的原始室外温度
        float temp = Climate.getInstantTemperature(level, pos);
        float outdoorTemp = Climate.get(level).getInstantTemperature(level, pos);
        // 相等 → 户外，不显示此条
        if (Math.abs(temp - outdoorTemp) < 0.01f) return;

        ClimateRange.Result result = range.checkTemperature(temp, false);

        tag.putFloat("firmalifehardcore:effectiveTemp", temp);
        tag.putString("firmalifehardcore:suitability", result.name());
    }

    // ===== 客户端 =====

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (!tag.contains("firmalifehardcore:effectiveTemp")) return;

        float temp = tag.getFloat("firmalifehardcore:effectiveTemp");
        String suitability = tag.getString("firmalifehardcore:suitability");

        TemperatureDisplayStyle style = TFCConfig.CLIENT.climateTooltipStyle.get();
        MutableComponent tempComp = style.formatRange(temp);
        if (tempComp == null) return;

        Component suitComp = switch (suitability) {
            case "LOW" -> Component.translatable("jade.firmalifehardcore.crop_temp_low");
            case "HIGH" -> Component.translatable("jade.firmalifehardcore.crop_temp_high");
            default -> Component.translatable("jade.firmalifehardcore.crop_temp_valid");
        };

        tooltip.add(Component.translatable("jade.firmalifehardcore.crop_temperature", tempComp, suitComp));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
