package com.yukimods.firmalifehardcore.util;

import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Map;
import java.util.Set;

/**
 * 容器类型 → 保鲜倍率映射。
 * 使用 Firmalife 的 block tag 进行匹配。
 */
public final class ContainerModifiers {

    private ContainerModifiers() {}

    /**
     * 根据方块获取容器修正系数。
     * 查询 Firmalife 的 block tag 来判断容器类型。
     * 返回 1.0 表示无额外修正（等同普通容器）。
     */
    public static float getModifier(Block block) {
        // 使用 Firmalife 的 block tag 常量
        // FLTags.Blocks.FOOD_SHELVES, FLTags.Blocks.HANGERS, etc.
        var state = block.defaultBlockState();

        // 通过 tag 匹配
        if (state.is(com.eerussianguy.firmalife.common.FLTags.Blocks.FOOD_SHELVES))
            return FirmaLifeHardCoreConfig.SERVER.containerModifierFoodShelf.get().floatValue();
        if (state.is(com.eerussianguy.firmalife.common.FLTags.Blocks.HANGERS))
            return FirmaLifeHardCoreConfig.SERVER.containerModifierHanger.get().floatValue();
        if (state.is(com.eerussianguy.firmalife.common.FLTags.Blocks.JARBNETS))
            return FirmaLifeHardCoreConfig.SERVER.containerModifierJarbnet.get().floatValue();
        if (state.is(com.eerussianguy.firmalife.common.FLTags.Blocks.KEGS))
            return FirmaLifeHardCoreConfig.SERVER.containerModifierKeg.get().floatValue();

        return 1.0f; // 默认无修正
    }
}
