package com.yukimods.firmalifehardcore.client;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import net.dries007.tfc.util.data.Support;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 对滑坡方块显示支撑状态：
 * - {@code tfc:can_landslide} — 卵石/砂砾/沙子（tag 滑坡）
 * - {@code firmalifehardcore:reinforceable} — 泥土/草地/耕地/草径（配方滑坡）
 * 塌方方块已由 tfc_support_indicator 覆盖。
 */
public enum ReinforcedDirtComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "landslide_support");

    private static final Lazy<TagKey<Block>> CAN_LANDSLIDE =
        Lazy.of(() -> TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("tfc", "can_landslide")));
    private static final Lazy<TagKey<Block>> REINFORCEABLE =
        Lazy.of(() -> TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "reinforceable")));

    private static final Component SELF_SUPPORTED = Component.translatable(
        "jade.firmalifehardcore.collapse.self_supported").withStyle(ChatFormatting.DARK_GREEN);
    private static final Component SELF_UNSUPPORTED = Component.translatable(
        "jade.firmalifehardcore.collapse.self_unsupported").withStyle(ChatFormatting.GOLD);

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        TagKey<Block> landslide = CAN_LANDSLIDE.get();
        TagKey<Block> reinforceable = REINFORCEABLE.get();
        if (!state.is(landslide) && !state.is(reinforceable)) return;

        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();

        try {
            tooltip.add(Support.isSupported(level, pos) ? SELF_SUPPORTED : SELF_UNSUPPORTED);
        } catch (Exception e) {
            FirmaLifeHardCore.LOGGER.error("[Jade] appendTooltip failed at {}", pos, e);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
