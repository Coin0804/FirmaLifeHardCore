package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.HangerBlockEntity;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.yukimods.firmalifehardcore.util.CellarTierAccessor;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HangerBlockEntity.class, remap = false)
public class HangerBlockEntityMixin {

    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void onGetFoodTrait(CallbackInfoReturnable<Holder<FoodTrait>> cir) {
        HangerBlockEntity self = (HangerBlockEntity) (Object) this;
        if (!self.isClimateValid()) return;

        int tier = ((CellarTierAccessor) this).firmalifehardcore$getCellarTier();
        cir.setReturnValue(switch (tier) {
            case 2 -> FLFoodTraits.HUNG_3;
            case 1 -> FLFoodTraits.HUNG_2;
            default -> FLFoodTraits.HUNG;
        });
    }
}
