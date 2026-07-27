package com.yukimods.firmalifehardcore.client;

import com.eerussianguy.firmalife.common.blockentities.PumpingStationBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 水泵 Jade tooltip — 显示泵水高度（水箱数 + RPM）。
 */
public enum PumpProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "pump");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof PumpingStationBlockEntity pumpBe) {
            try {
                java.lang.reflect.Method getTankCount =
                    pumpBe.getClass().getMethod("firmalifehardcore$getTankCount");
                int tanks = (int) getTankCount.invoke(pumpBe);
                int rpm = 0;
                var rotation = pumpBe.getRotationNode().rotation();
                if (rotation != null) {
                    rpm = (int) Math.floor((rotation.positiveSpeed() / Mth.TWO_PI) * 20.0f * 60.0f);
                }
                tag.putInt("firmalifehardcore:pump_head", tanks + rpm);
            } catch (Exception ignored) { }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (!tag.contains("firmalifehardcore:pump_head")) return;
        int head = tag.getInt("firmalifehardcore:pump_head");
        tooltip.add(Component.translatable("jade.firmalifehardcore.pump_head", head));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
