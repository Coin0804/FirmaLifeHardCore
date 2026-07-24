package com.yukimods.firmalifehardcore.mixin;

import net.dries007.tfc.common.blockentities.ThermometerBlockEntity;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 温度计直接调 ClimateModel 实例方法，绕过 ClimateMixin 的静态方法拦截。
 * 重定向到 Climate 静态方法，使地窖/温室温度正常显示。
 */
@Mixin(value = ThermometerBlockEntity.class, remap = false)
public class ThermometerMixin {

    @Redirect(method = "updatePower",
        at = @At(value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/ClimateModel;getInstantTemperature(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)F"),
        remap = false)
    private static float redirectGetInstantTemp(ClimateModel self, LevelReader levelReader, BlockPos pos) {
        if (levelReader instanceof Level level) {
            return Climate.getInstantTemperature(level, pos);
        }
        return self.getInstantTemperature(levelReader, pos);
    }
}
