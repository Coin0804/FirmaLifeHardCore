package com.yukimods.firmalifehardcore.client;

import com.eerussianguy.firmalife.common.blocks.greenhouse.AbstractSprinklerBlock;
import com.eerussianguy.firmalife.common.blocks.greenhouse.PumpingStationBlock;
import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import net.dries007.tfc.common.blocks.crop.CropBlock;
import net.dries007.tfc.common.blocks.devices.ThermometerBlock;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade 插件入口 — 注册带支撑土 tooltip + 作物温度 tooltip。
 * Jade 通过 {@link WailaPlugin} 注解自动发现。
 */
@WailaPlugin(FirmaLifeHardCore.MOD_ID)
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(CropTemperatureProvider.INSTANCE, CropBlock.class);
        registration.registerBlockDataProvider(ThermometerTemperatureProvider.INSTANCE, ThermometerBlock.class);
        registration.registerBlockDataProvider(SprinklerProvider.INSTANCE, AbstractSprinklerBlock.class);
        registration.registerBlockDataProvider(PumpProvider.INSTANCE, PumpingStationBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        FirmaLifeHardCore.LOGGER.info("[Jade] registering collapse_support tooltip");
        registration.registerBlockComponent(
            ReinforcedDirtComponentProvider.INSTANCE, Block.class
        );
        registration.registerBlockComponent(
            CropTemperatureProvider.INSTANCE, CropBlock.class
        );
        registration.registerBlockComponent(
            ThermometerTemperatureProvider.INSTANCE, ThermometerBlock.class
        );
        registration.registerBlockComponent(
            SprinklerProvider.INSTANCE, AbstractSprinklerBlock.class
        );
        registration.registerBlockComponent(
            PumpProvider.INSTANCE, PumpingStationBlock.class
        );
    }
}
