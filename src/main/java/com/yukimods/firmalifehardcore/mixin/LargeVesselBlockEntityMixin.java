package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.yukimods.firmalifehardcore.util.CellarInventoryHelper;
import com.yukimods.firmalifehardcore.util.CellarTierAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.dries007.tfc.common.blockentities.LargeVesselBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LargeVesselBlockEntity: 跟随原版 onSeal/onUnseal 密封逻辑。
 *
 * 原版路径: onSeal → apply PRESERVED trait     onUnseal → remove PRESERVED trait
 * 注入点:   追加 apply cellar SHELVED trait      追加 remove cellar SHELVED traits
 */
@Mixin(value = LargeVesselBlockEntity.class, remap = false)
public abstract class LargeVesselBlockEntityMixin implements ClimateReceiver, CellarTierAccessor {

    @Unique
    private int firmalifehardcore$cellarTier;
    @Unique
    private boolean firmalifehardcore$climateValid;

    @Override
    public int firmalifehardcore$getCellarTier() { return firmalifehardcore$cellarTier; }
    @Override
    public void firmalifehardcore$setCellarTier(int tier) { this.firmalifehardcore$cellarTier = tier; }

    @Override
    public void setValid(Level level, BlockPos pos, boolean valid, int tier, ClimateType climate) {
        if (climate == ClimateType.CELLAR) {
            this.firmalifehardcore$climateValid = valid;
            this.firmalifehardcore$cellarTier = valid ? tier : 0;
        }
    }

    /** 密封时追加 cellar trait（原版已 apply PRESERVED） */
    @Inject(method = "onSeal", at = @At("TAIL"))
    private void applyCellarTraitOnSeal(CallbackInfo ci) {
        if (!firmalifehardcore$climateValid) return;
        LargeVesselBlockEntity self = (LargeVesselBlockEntity) (Object) this;
        Holder<FoodTrait> trait = CellarInventoryHelper.traitForTier(firmalifehardcore$cellarTier);
        for (int i = 0; i < LargeVesselBlockEntity.SLOTS; i++) {
            ItemStack stack = self.getInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                FoodCapability.applyTrait(stack, trait);
            }
        }
    }

    /** 解封时清除 cellar trait（原版已 remove PRESERVED） */
    @Inject(method = "onUnseal", at = @At("TAIL"))
    private void removeCellarTraitOnUnseal(CallbackInfo ci) {
        LargeVesselBlockEntity self = (LargeVesselBlockEntity) (Object) this;
        for (int i = 0; i < LargeVesselBlockEntity.SLOTS; i++) {
            ItemStack stack = self.getInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                CellarInventoryHelper.clearCellarTraits(stack);
            }
        }
    }
}
