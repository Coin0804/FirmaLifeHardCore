package com.yukimods.firmalifehardcore.util;

import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodTrait;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 地窖保鲜共享工具 —— 供 Jarbnet / LargeVessel 等特定容器的 ClimateReceiver Mixin 使用。
 */
public final class CellarInventoryHelper {

    static final Set<Holder<FoodTrait>> ALL_SHELVED = Set.of(
        FLFoodTraits.SHELVED,
        FLFoodTraits.SHELVED_2,
        FLFoodTraits.SHELVED_3
    );

    private CellarInventoryHelper() {}

    /** 根据 CellarTracker 查询结果返回对应的 SHELVED trait。null = 不在有效地窖中 */
    @Nullable
    public static Holder<FoodTrait> getCellarTrait(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return null;
        CellarTracker tracker = CellarAttachment.get(sl);
        CellarSpace.CellarResult result = tracker.query(pos);
        if (result == null || !result.valid()) return null;
        return traitForTier(tierFromResistance(result.avgResistance()));
    }

    /** tier → SHELVED trait */
    public static Holder<FoodTrait> traitForTier(int tier) {
        return switch (tier) {
            case 2 -> FLFoodTraits.SHELVED_3;
            case 1 -> FLFoodTraits.SHELVED_2;
            default -> FLFoodTraits.SHELVED;
        };
    }

    static int tierFromResistance(float avgR) {
        if (avgR >= 0.70f) return 2;
        if (avgR >= 0.45f) return 1;
        return 0;
    }

    /** 取出/解封时彻底清除所有 cellar trait */
    public static void clearCellarTraits(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (Holder<FoodTrait> trait : ALL_SHELVED) {
            FoodCapability.removeTrait(stack, trait);
        }
    }

    /** 确保 stack 只保留 target trait，移除其余 cellar trait */
    public static boolean normalizeTraits(ItemStack stack, @Nullable Holder<FoodTrait> target) {
        if (FoodCapability.get(stack) == null) return false;
        boolean changed = false;
        for (Holder<FoodTrait> trait : ALL_SHELVED) {
            if (trait != target && FoodCapability.hasTrait(stack, trait)) {
                FoodCapability.removeTrait(stack, trait);
                changed = true;
            }
        }
        if (target != null && !FoodCapability.hasTrait(stack, target)) {
            FoodCapability.applyTrait(stack, target);
            changed = true;
        }
        return changed;
    }
}
