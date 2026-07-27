package com.yukimods.firmalifehardcore.client;

import com.eerussianguy.firmalife.common.blocks.greenhouse.AbstractSprinklerBlock;
import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import com.yukimods.firmalifehardcore.util.CellarSpace;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 洒水器 Jade tooltip 提供者。
 * 服务端直接查询 {@link CellarTracker} 获取温室/地窖状态，
 * 客户端输出与原版 addHoeOverlayInfo 完全相同的两行格式：
 * <ol>
 *   <li>STASIS → firmalife.greenhouse.valid_sprinkler / invalid_sprinkler</li>
 *   <li>CellarTracker 结果 → firmalife.greenhouse.valid_generic / invalid_generic</li>
 * </ol>
 */
public enum SprinklerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "sprinkler");

    // ===== 服务端 =====

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        BlockState state = accessor.getBlockState();

        if (!(state.getBlock() instanceof AbstractSprinklerBlock)) return;

        boolean stasis = state.getValue(AbstractSprinklerBlock.STASIS);
        tag.putBoolean("firmalifehardcore:stasis", stasis);

        if (level instanceof ServerLevel serverLevel) {
            CellarTracker tracker = CellarAttachment.get(serverLevel);
            CellarSpace space = tracker.query(pos);
            if (space != null && space.valid) {
                tag.putBoolean("firmalifehardcore:in_greenhouse", space.isGreenhouse());
                tag.putBoolean("firmalifehardcore:in_cellar", !space.isGreenhouse());
            }
        }
    }

    // ===== 客户端 =====

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (!tag.contains("firmalifehardcore:stasis")) return;

        boolean stasis = tag.getBoolean("firmalifehardcore:stasis");
        boolean inSpace = tag.getBoolean("firmalifehardcore:in_greenhouse")
            || tag.getBoolean("firmalifehardcore:in_cellar");

        // 与原版 addHoeOverlayInfo 完全相同的两行格式
        tooltip.add(Component.translatable(
            stasis ? "firmalife.greenhouse.valid_sprinkler" : "firmalife.greenhouse.invalid_sprinkler"));
        tooltip.add(Component.translatable(
            inSpace ? "firmalife.greenhouse.valid_generic" : "firmalife.greenhouse.invalid_generic"));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
