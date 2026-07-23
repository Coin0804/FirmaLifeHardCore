package com.yukimods.firmalifehardcore.item;

import java.util.EnumMap;
import java.util.Map;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.block.ModBlocks;
import com.yukimods.firmalifehardcore.block.ReinforcedSoilBlock;
import com.yukimods.firmalifehardcore.block.ReinforcedSoilType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册中心 — 带支撑土 BlockItem × 8。
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(FirmaLifeHardCore.MOD_ID);

    private static final Map<ReinforcedSoilType, DeferredItem<BlockItem>> BY_TYPE =
        new EnumMap<>(ReinforcedSoilType.class);

    static {
        for (ReinforcedSoilType type : ReinforcedSoilType.values()) {
            DeferredBlock<ReinforcedSoilBlock> block = ModBlocks.get(type);
            DeferredItem<BlockItem> item = ITEMS.register(type.blockName(),
                () -> new BlockItem(block.get(), new Item.Properties()));
            BY_TYPE.put(type, item);
        }
    }

    private ModItems() {}

    public static DeferredItem<BlockItem> get(ReinforcedSoilType type) {
        return BY_TYPE.get(type);
    }

    public static Iterable<DeferredItem<BlockItem>> all() {
        return BY_TYPE.values();
    }
}
