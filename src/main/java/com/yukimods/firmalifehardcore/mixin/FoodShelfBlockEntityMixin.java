package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity;
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
 * Mixin FoodShelfBlockEntity.getFoodTrait() — 使用 CellarTracker 的地窖有效温度替代全局气候温度。
 */
@Mixin(value = FoodShelfBlockEntity.class, remap = false)
public class FoodShelfBlockEntityMixin {

    /**
     * 在 getFoodTrait() 返回前拦截，用地窖有效温度重新选择 FoodTrait。
     * 如果 CellarTracker 未检测到有效空间，回退到原返回值。
     */
    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void onGetFoodTrait(CallbackInfoReturnable<Holder<FoodTrait>> cir) {
        FoodShelfBlockEntity self = (FoodShelfBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!self.isClimateValid()) return; // 不在有效气候空间中，保持原逻辑

        // 查询 CellarTracker
        CellarTracker tracker = CellarAttachment.get(serverLevel);
        if (tracker == null) return;

        BlockPos pos = self.getBlockPos();
        CellarSpace.CellarResult result = tracker.query(pos);
        if (result == null || !result.valid()) return;

        // 使用地窖有效温度判定保鲜等级
        float T_cellar = result.effectiveTemperature();
        float level2Threshold = FirmaLifeHardCoreConfig.SERVER.level2ResistanceThreshold.get().floatValue();
        float level3Threshold = FirmaLifeHardCoreConfig.SERVER.level3ResistanceThreshold.get().floatValue();
        float avgResistance = result.avgResistance();

        // 优先用热阻判定（复古物语式），其次用温度
        if (avgResistance >= level3Threshold) {
            cir.setReturnValue(FLFoodTraits.SHELVED_3);
        } else if (avgResistance >= level2Threshold) {
            cir.setReturnValue(FLFoodTraits.SHELVED_2);
        } else {
            cir.setReturnValue(FLFoodTraits.SHELVED);
        }
    }
}
