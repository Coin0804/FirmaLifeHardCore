package com.yukimods.firmalifehardcore;

import com.yukimods.firmalifehardcore.block.ModBlocks;
import com.yukimods.firmalifehardcore.block.ReinforcedSoilType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式物品栏 — 展示本 mod 所有带支撑土变体。
 */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FirmaLifeHardCore.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
        CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.firmalifehardcore"))
            .icon(() -> new ItemStack(ModBlocks.get(ReinforcedSoilType.ENTISOL).get()))
            .displayItems((params, output) -> {
                for (var block : ModBlocks.all()) {
                    output.accept(block.get());
                }
            })
            .build()
        );

    private ModCreativeTab() {}
}
