package com.yukimods.firmalifehardcore.mixin;

import com.eerussianguy.firmalife.common.blockentities.PumpingStationBlockEntity;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import com.yukimods.firmalifehardcore.util.PumpTickManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 水泵流体化——FluidTank 存储 + 每 tick 填充 + IFluidHandler。
 * 容量 = 500 + 上方直连水箱 × 500（≤maxTankBonus）。
 * 水箱扫描事件驱动：方块变更时通知泵重扫。
 */
@Mixin(value = PumpingStationBlockEntity.class, remap = false)
public abstract class PumpingStationBlockEntityMixin implements IFluidHandler {

    // ---- 已加载水泵 ----

    @Inject(method = "onLoadAdditional", at = @At("HEAD"), remap = false)
    private void registerTick(CallbackInfo ci) {
        PumpTickManager.register((PumpingStationBlockEntity) (Object) this);
        firmalifehardcore$cachedTankCount = -1; // 初次加载时重扫
    }

    @Inject(method = "onUnloadAdditional", at = @At("TAIL"), remap = false)
    private void unregisterTick(CallbackInfo ci) {
        PumpTickManager.unregister((PumpingStationBlockEntity) (Object) this);
    }

    // ---- FluidTank ----

    @Unique
    private final FluidTank firmalifehardcore$tank = new FluidTank(FirmaLifeHardCoreConfig.SERVER.pumpBaseCapacity.get()) {
        @Override
        protected void onContentsChanged() {
            PumpingStationBlockEntity self = (PumpingStationBlockEntity) (Object) PumpingStationBlockEntityMixin.this;
            self.markForSync();
        }
    };

    // ---- IFluidHandler 委托 ----

    @Override
    public int getTanks() { return firmalifehardcore$tank.getTanks(); }

    @Override
    public FluidStack getFluidInTank(int tank) { return firmalifehardcore$tank.getFluidInTank(tank); }

    @Override
    public int getTankCapacity(int tank) {
        firmalifehardcore$syncCapacity();
        return firmalifehardcore$tank.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) { return firmalifehardcore$tank.isFluidValid(tank, stack); }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        firmalifehardcore$syncCapacity();
        return firmalifehardcore$tank.drain(maxDrain, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        firmalifehardcore$syncCapacity();
        return firmalifehardcore$tank.drain(resource, action);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0; // 水泵从水源取水，不接受外部填充
    }

    /** 同步容量并裁剪超容的水（拆水箱后容量变小） */
    @Unique
    private void firmalifehardcore$syncCapacity() {
        if (firmalifehardcore$cachedTankCount < 0) firmalifehardcore$rescanTanksAbove();
        int cap = firmalifehardcore$getCapacity();
        firmalifehardcore$tank.setCapacity(cap);
        int excess = firmalifehardcore$tank.getFluidAmount() - cap;
        if (excess > 0) {
            firmalifehardcore$tank.drain(excess, FluidAction.EXECUTE);
        }
    }

    /** tickAll 用——绕过 fill()=0 限制 */
    @Unique
    public void firmalifehardcore$internalFill(int amount) {
        firmalifehardcore$syncCapacity();
        firmalifehardcore$tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount), FluidAction.EXECUTE);
    }

    // ---- 容量 & 水箱（事件驱动扫描）----

    /** 缓存的水箱数：-1=待重扫，≥0=有效 */
    @Unique
    private int firmalifehardcore$cachedTankCount = -1;

    @Unique
    private int firmalifehardcore$getCapacity() {
        int base = FirmaLifeHardCoreConfig.SERVER.pumpBaseCapacity.get();
        int bonus = FirmaLifeHardCoreConfig.SERVER.tankCapacityBonus.get();
        int max = FirmaLifeHardCoreConfig.SERVER.maxTankBonus.get();
        return base + Math.min(firmalifehardcore$cachedTankCount, max) * bonus;
    }

    /** 暴露水箱数供泵压计算 */
    @Unique
    public int firmalifehardcore$getTankCount() {
        if (firmalifehardcore$cachedTankCount < 0) firmalifehardcore$rescanTanksAbove();
        return firmalifehardcore$cachedTankCount;
    }

    /**
     * 从泵正上方向上一格格扫描，连续遇到灌溉水箱则计数。
     * 结果缓存在 cachedTankCount，方块变更事件驱动 invalidate。
     * 遇到非水箱方块立即停止。
     */
    @Unique
    private void firmalifehardcore$rescanTanksAbove() {
        PumpingStationBlockEntity self = (PumpingStationBlockEntity) (Object) this;
        if (self.getLevel() == null) return;
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(self.getBlockPos());
        int max = FirmaLifeHardCoreConfig.SERVER.maxTankBonus.get();
        for (int i = 0; i < max; i++) {
            cursor.move(0, 1, 0);
            if (self.getLevel().getBlockState(cursor).getBlock() == FLBlocks.IRRIGATION_TANK.get()) {
                count++;
            } else {
                break;
            }
        }
        firmalifehardcore$cachedTankCount = count;
    }

    /** 外部通过 PumpTickManager.notifyBlockChanged 调用——invalidate 缓存 */
    @Unique
    private void firmalifehardcore$invalidateTankCache() {
        firmalifehardcore$cachedTankCount = -1;
    }

    // ---- NBT ----

    /** Mixin 覆盖 BlockEntity.saveAdditional——父类无实现，不会丢数据 */
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider access) {
        CompoundTag tankTag = new CompoundTag();
        firmalifehardcore$tank.writeToNBT(access, tankTag);
        tag.put("firmalifehardcore:tank", tankTag);
    }

    /** Mixin 覆盖 BlockEntity.loadAdditional——父类无实现，不会丢数据 */
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider access) {
        if (tag.contains("firmalifehardcore:tank")) {
            firmalifehardcore$tank.readFromNBT(access, tag.getCompound("firmalifehardcore:tank"));
        }
    }
}
