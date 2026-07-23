package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.yukimods.firmalifehardcore.util.CellarTierAccessor;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FoodShelfBlockEntity.class, remap = false)
public class FoodShelfBlockEntityMixin implements CellarTierAccessor {

    protected int firmalifehardcore$cellarTier = 0;

    @Override
    public int firmalifehardcore$getCellarTier() { return this.firmalifehardcore$cellarTier; }
    @Override
    public void firmalifehardcore$setCellarTier(int tier) { this.firmalifehardcore$cellarTier = tier; }

    @Inject(method = "setValid", at = @At("HEAD"))
    private void onSetValid(Level level, net.minecraft.core.BlockPos pos, boolean valid, int tier, ClimateType climate, CallbackInfo ci) {
        if (climate == ClimateType.CELLAR) {
            this.firmalifehardcore$cellarTier = valid ? tier : 0;
            if (valid) {
                // 先清旧 trait，原代码随后 updatePreservation(true) 会加新的，避免叠加
                FoodShelfBlockEntity self = (FoodShelfBlockEntity) (Object) this;
                self.updatePreservation(false);
            }
        }
    }

    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void onGetFoodTrait(CallbackInfoReturnable<Holder<FoodTrait>> cir) {
        FoodShelfBlockEntity self = (FoodShelfBlockEntity) (Object) this;
        if (!self.isClimateValid()) return;

        switch (this.firmalifehardcore$cellarTier) {
            case 2 -> cir.setReturnValue(FLFoodTraits.SHELVED_3);
            case 1 -> cir.setReturnValue(FLFoodTraits.SHELVED_2);
            default -> cir.setReturnValue(FLFoodTraits.SHELVED);
        }
    }

    /** 加载时跳过 updatePreservation，由 CellarTracker 的 setValid 统一处理 tier */
    @Redirect(method = "onLoadAdditional",
        at = @At(value = "INVOKE",
            target = "Lcom/eerussianguy/firmalife/common/blockentities/FoodShelfBlockEntity;updatePreservation(Z)V"),
        remap = false)
    private void skipUpdatePreservationOnLoad(FoodShelfBlockEntity instance, boolean preserved) {}

    /** 取物品时清掉所有可能的保鲜 trait，而非只清当前 getFoodTrait() 返回的那个 */
    @Redirect(method = "use",
        at = @At(value = "INVOKE",
            target = "Lnet/dries007/tfc/common/component/food/FoodCapability;removeTrait(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)V"),
        remap = false)
    private void onRemoveTraitInUse(ItemStack stack, Holder<FoodTrait> ignored) {
        FoodShelfBlockEntity self = (FoodShelfBlockEntity) (Object) this;
        for (Holder<FoodTrait> trait : self.getPossibleTraits()) {
            FoodCapability.removeTrait(stack, trait);
        }
    }
}
