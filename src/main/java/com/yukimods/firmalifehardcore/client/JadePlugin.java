package com.yukimods.firmalifehardcore.client;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade 插件入口 — 注册带支撑土的 tooltip 提供者。
 * Jade 通过 {@link WailaPlugin} 注解自动发现。
 */
@WailaPlugin(FirmaLifeHardCore.MOD_ID)
public class JadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        FirmaLifeHardCore.LOGGER.info("[Jade] registering collapse_support tooltip");
        registration.registerBlockComponent(
            ReinforcedDirtComponentProvider.INSTANCE, Block.class
        );
    }
}
