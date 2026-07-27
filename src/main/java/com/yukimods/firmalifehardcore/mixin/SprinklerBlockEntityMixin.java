package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.PumpingStationBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.SprinklerBlockEntity;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.blocks.greenhouse.GreenhousePortBlock;
import com.eerussianguy.firmalife.common.blocks.greenhouse.SprinklerPipeBlock;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;

/**
 * Phase 1: 灌溉水箱 BFS 穿透 (isPipe / isPipeInDirection)
 * Phase 2: 真实水消耗 + 泵压公式
 */
@Mixin(value = SprinklerBlockEntity.class, remap = false)
public class SprinklerBlockEntityMixin {

    // ===== Phase 1: BFS 穿透水箱 =====

    @Redirect(
        method = "enqueueConnections",
        at = @At(value = "INVOKE", target = "Lcom/eerussianguy/firmalife/common/blockentities/SprinklerBlockEntity;isPipe(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
        remap = false)
    private static boolean redirectIsPipe(BlockState state) {
        return state.getBlock() instanceof SprinklerPipeBlock
            || state.getBlock() instanceof GreenhousePortBlock
            || state.getBlock() == FLBlocks.IRRIGATION_TANK.get();
    }

    @Redirect(
        method = "enqueueConnections",
        at = @At(value = "INVOKE", target = "Lcom/eerussianguy/firmalife/common/blockentities/SprinklerBlockEntity;isPipeInDirection(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"),
        remap = false)
    private static boolean redirectIsPipeInDirection(BlockState state, Direction direction) {
        if (state.getBlock() == FLBlocks.IRRIGATION_TANK.get()) {
            return direction.getAxis().isHorizontal();
        }
        return state.getBlock() instanceof SprinklerPipeBlock
            ? state.getValue(DirectionPropertyBlock.getProperty(direction))
            : state.getValue(GreenhousePortBlock.AXIS) == direction.getAxis();
    }

    /** BFS 最大距离：原版硬编码 32 → 配置项 pipeMaxCost */
    @Redirect(
        method = "enqueueConnections",
        at = @At(value = "FIELD", target = "Lcom/eerussianguy/firmalife/common/blockentities/SprinklerBlockEntity;MAX_COST:I"),
        remap = false)
    private static int redirectMaxCost() {
        return FirmaLifeHardCoreConfig.SERVER.pipeMaxCost.get();
    }

    // ===== Phase 2: 洒水器 Y 传递 + 泵压 + 水消耗 =====

    @Unique
    private static int firmalifehardcore$sprinklerY;

    @Inject(method = "searchForFluid", at = @At("HEAD"), remap = false)
    private static void captureSprinklerY(Level level, BlockPos start, Direction pipeDirection, boolean drain, CallbackInfoReturnable<Fluid> ci) {
        firmalifehardcore$sprinklerY = start.getY();
    }

    /**
     * 替代 hasConnection：向下扫描穿过水箱找泵，查 IFluidHandler 水位 + 泵压 + drain。
     */
    @Redirect(
        method = "enqueueConnections",
        at = @At(value = "INVOKE", target = "Lcom/eerussianguy/firmalife/common/blocks/greenhouse/PumpingStationBlock;hasConnection(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Z"),
        remap = false)
    private static boolean redirectHasConnection(LevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(pos);
        for (int i = 0; i < 4; i++) {
            if (level.getBlockEntity(cursor) instanceof PumpingStationBlockEntity pumpBe
                && pumpBe instanceof IFluidHandler fh) {
                int use = FirmaLifeHardCoreConfig.SERVER.sprinklerWaterUse.get();
                int water = fh.getFluidInTank(0).getAmount();
                if (water < use) return false;

                int pumpY = cursor.getY();
                int tankCount = firmalifehardcore$scanTanksAbove(level, cursor);
                int rpm = 0;
                var rotation = pumpBe.getRotationNode().rotation();
                if (rotation != null) {
                    rpm = (int) Math.floor((rotation.positiveSpeed() / Mth.TWO_PI) * 20.0f * 60.0f);
                }
                int pressure = pumpY + tankCount + rpm - firmalifehardcore$sprinklerY;
                if (pressure < 0) return false;

                FluidStack drained = fh.drain(use, FluidAction.EXECUTE);
                return !drained.isEmpty();
            } else if (level.getBlockState(cursor).getBlock() != FLBlocks.IRRIGATION_TANK.get()) {
                return false;
            }
            cursor.move(0, -1, 0);
        }
        return false;
    }

    /** 从泵正上方向上一格格扫描，连续遇到灌溉水箱则计数，最多 maxTankBonus 个。 */
    @Unique
    private static int firmalifehardcore$scanTanksAbove(LevelAccessor level, BlockPos pumpPos) {
        int max = FirmaLifeHardCoreConfig.SERVER.maxTankBonus.get();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(pumpPos);
        for (int i = 0; i < max; i++) {
            cursor.move(0, 1, 0);
            if (level.getBlockState(cursor).getBlock() != FLBlocks.IRRIGATION_TANK.get()) {
                return i;
            }
        }
        return max;
    }
}
