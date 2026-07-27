package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blocks.greenhouse.AbstractSprinklerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 禁用原版洒水器 HoeOverlay tooltip。
 * 原版 addHoeOverlayInfo() 的第二行读取 SprinklerBlockEntity.isValid()，
 * 该字段同步链不可靠。由 SprinklerProvider 替代提供相同格式的 tooltip。
 */
@Mixin(value = AbstractSprinklerBlock.class, remap = false)
public class AbstractSprinklerBlockMixin {

    @Inject(method = "addHoeOverlayInfo", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelHoeOverlay(Level level, BlockPos pos, BlockState state, Consumer<Component> text, boolean debug, CallbackInfo ci) {
        ci.cancel();
    }
}
