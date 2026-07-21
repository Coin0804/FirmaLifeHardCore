package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.HangerBlockEntity;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import com.yukimods.firmalifehardcore.util.CellarSpace;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin HangerBlockEntity.getFoodTrait() — 同 FoodShelfBlockEntityMixin，使用地窖有效温度。
 */
@Mixin(value = HangerBlockEntity.class, remap = false)
public class HangerBlockEntityMixin {

    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void onGetFoodTrait(CallbackInfoReturnable<Holder<FoodTrait>> cir) {
        HangerBlockEntity self = (HangerBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!self.isClimateValid()) return;

        CellarTracker tracker = CellarAttachment.get(serverLevel);
        if (tracker == null) return;

        BlockPos pos = self.getBlockPos();
        CellarSpace.CellarResult result = tracker.query(pos);
        if (result == null || !result.valid()) return;

        float avgResistance = result.avgResistance();
        float level2Threshold = FirmaLifeHardCoreConfig.SERVER.level2ResistanceThreshold.get().floatValue();
        float level3Threshold = FirmaLifeHardCoreConfig.SERVER.level3ResistanceThreshold.get().floatValue();

        if (avgResistance >= level3Threshold) {
            cir.setReturnValue(FLFoodTraits.HUNG_3);
        } else if (avgResistance >= level2Threshold) {
            cir.setReturnValue(FLFoodTraits.HUNG_2);
        } else {
            cir.setReturnValue(FLFoodTraits.HUNG);
        }
    }
}
