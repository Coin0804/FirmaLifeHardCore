package com.yukimods.firmalifehardcore.mixin;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

import com.eerussianguy.firmalife.common.blockentities.PumpingStationBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.SprinklerBlockEntity;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.blocks.greenhouse.GreenhousePortBlock;
import com.eerussianguy.firmalife.common.blocks.greenhouse.SprinklerPipeBlock;
import com.eerussianguy.firmalife.config.FLConfig;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import com.yukimods.firmalifehardcore.util.BfsNode;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.dries007.tfc.util.Helpers;

/**
 * 洒水头 BFS 寻路重构。
 * <p>
 * 判断顺序：<b>终端优先，管道其次</b>。<br>
 * 水箱既是终端节点（向下扫水泵）又是管道节点（水平穿越到另一侧管道）。<br>
 * 终端检查先执行，成功则立即返回；失败则 fall through 走管道穿越。
 */
@Mixin(value = SprinklerBlockEntity.class, remap = false)
public class SprinklerBlockEntityMixin {

    // =========================================================================
    // 洒水头 Y 坐标（供 checkPumpConnection 压力公式使用）
    // =========================================================================

    @Unique
    private static int firmalifehardcore$sprinklerY;

    // =========================================================================
    // searchForFluid 完整覆写
    // =========================================================================

    /**
     * 在管道网络中 BFS 寻找可用的水泵/水箱水源。
     * <p>
     * 与原版的区别：
     * <ul>
     * <li>终端判断（水泵/水箱）优先于管道穿越</li>
     * <li>水箱同时是终端和管道——终端先检查，失败后走管道穿越</li>
     * <li>管道穿越增加了 {@code isPipeInDirection(adj, opposite)} 回连检查</li>
     * </ul>
     *
     * @param level         世界
     * @param start         洒水头位置
     * @param pipeDirection 洒水头朝向管道方向（上方为 UP，地面为 DOWN）
     * @param drain         是否实际消耗水源（false 仅水位/压力检查，不扣水）
     * @return 找到的流体类型，或 {@code null}
     */
    @Overwrite
    public static @Nullable Fluid searchForFluid(Level level, BlockPos start, Direction pipeDirection, boolean drain) {
        firmalifehardcore$sprinklerY = start.getY();

        // —— 简单模式：不使用管道，直接在相邻方块找流体 ——
        if (!FLConfig.SERVER.usePipesForSprinklers.get()) {
            final BlockPos checkPos = start.relative(pipeDirection);
            final BlockEntity be = level.getBlockEntity(checkPos);
            if (be != null) {
                final IFluidHandler cap = Helpers.getCapability(Capabilities.FluidHandler.BLOCK, be, pipeDirection.getOpposite());
                if (cap != null) {
                    final Fluid fluid = cap.getFluidInTank(0).getFluid();
                    if (fluid != Fluids.EMPTY && Helpers.isFluid(fluid, TFCTags.Fluids.ANY_FRESH_WATER)) {
                        if (drain) {
                            final FluidStack drainedStack = cap.drain(1, FluidAction.EXECUTE);
                            if (!drainedStack.isEmpty()) {
                                return drainedStack.getFluid();
                            }
                        }
                    }
                }
            }
            return null;
        }

        // —— 管道 BFS 模式 ——
        final int maxCost = FirmaLifeHardCoreConfig.SERVER.pipeMaxCost.get();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final Queue<BfsNode> queue = new ArrayDeque<>();
        final Set<BlockPos> seen = new ObjectOpenHashSet<>(64);

        // 洒水头正对的管道是 BFS 起点
        final BlockPos firstPipePos = start.relative(pipeDirection);
        final BlockState firstPipeState = level.getBlockState(firstPipePos);

        if (!firmalifehardcore$isPipe(firstPipeState)) {
            return null;
        }

        queue.add(new BfsNode(firstPipeState, firstPipePos, 1));
        seen.add(firstPipePos);

        while (!queue.isEmpty()) {
            final BfsNode prev = queue.poll();

            for (final Direction direction : Helpers.DIRECTIONS) {
                cursor.setWithOffset(prev.pos(), direction);

                if (seen.contains(cursor)) {
                    continue;
                }

                final BlockState stateAdj = level.getBlockState(cursor);

                // ═══════════════════════════════════════════════════
                // ① 终端判断 —— 优先
                // ═══════════════════════════════════════════════════
                if (stateAdj.getBlock() == FLBlocks.PUMPING_STATION.get()
                    || stateAdj.getBlock() == FLBlocks.IRRIGATION_TANK.get()) {

                    if (direction.getAxis().isHorizontal()
                        && firmalifehardcore$isPipeInDirection(prev.state(), direction)) {

                        if (firmalifehardcore$checkPumpConnection(level, cursor, drain)) {
                            return stateAdj.getFluidState().getType();
                        }
                        // hasConnection 失败 → fall through，不 return
                    }
                }

                // ═══════════════════════════════════════════════════
                // ② 管道穿越 —— 终端失效后才进入
                // ═══════════════════════════════════════════════════
                if (firmalifehardcore$isPipe(stateAdj)) {
                    // 相邻方块必须回连当前方向
                    if (!firmalifehardcore$isPipeInDirection(stateAdj, direction.getOpposite())) {
                        continue;
                    }

                    // 跳过非同色管道（保底检查，与原版一致）
                    final boolean bothPipes = prev.state().getBlock() instanceof SprinklerPipeBlock
                        && stateAdj.getBlock() instanceof SprinklerPipeBlock;
                    final boolean differentPipes = bothPipes && stateAdj.getBlock() != prev.state().getBlock();
                    if (differentPipes) {
                        continue;
                    }

                    if (prev.cost() < maxCost) {
                        final BlockPos posAdj = cursor.immutable();
                        queue.add(new BfsNode(stateAdj, posAdj, prev.cost() + 1));
                        seen.add(posAdj);
                    }
                }
            }
        }

        return null;
    }

    // =========================================================================
    // 管道类型判断
    // =========================================================================

    /** 管道、温室端口、灌溉水箱均可作为管道穿越。 */
    @Unique
    private static boolean firmalifehardcore$isPipe(BlockState state) {
        return state.getBlock() instanceof SprinklerPipeBlock
            || state.getBlock() instanceof GreenhousePortBlock
            || state.getBlock() == FLBlocks.IRRIGATION_TANK.get();
    }

    /** 判断方块在指定方向是否有连接。水箱仅水平方向连通。 */
    @Unique
    private static boolean firmalifehardcore$isPipeInDirection(BlockState state, Direction direction) {
        if (state.getBlock() == FLBlocks.IRRIGATION_TANK.get()) {
            return direction.getAxis().isHorizontal();
        }
        if (state.getBlock() instanceof SprinklerPipeBlock) {
            return state.getValue(DirectionPropertyBlock.getProperty(direction));
        }
        // GreenhousePortBlock：轴向连通
        return state.getValue(GreenhousePortBlock.AXIS) == direction.getAxis();
    }

    // =========================================================================
    // 水泵逻辑
    // =========================================================================

    /**
     * 从给定位置向下扫描找水泵，检查水位 + 压力 + 实际耗水。
     * <p>
     * 压力公式：{@code pumpY + tankCount + rpm - sprinklerY >= 0} 才允许供水。
     * <p>
     * drain=true 时实际扣水（洒水器浇水）；drain=false 时仅探测——水位与压力已通过
     * 检查即视为连接有效，不执行任何 drain 操作，避免探测方（如 tfcfertigation 的
     * 施肥周期探测）误耗水泵水。
     */
    @Unique
    private static boolean firmalifehardcore$checkPumpConnection(LevelAccessor level, BlockPos pos, boolean drain) {
        final int use = FirmaLifeHardCoreConfig.SERVER.sprinklerWaterUse.get();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(pos);

        for (int i = 0; i < 4; i++) {
            final BlockEntity be = level.getBlockEntity(cursor);
            if (be instanceof PumpingStationBlockEntity pumpBe && be instanceof IFluidHandler fh) {
                final int water = fh.getFluidInTank(0).getAmount();
                if (water < use) {
                    return false;
                }

                final int pumpY = cursor.getY();

                // 从水泵自身容量反算上方水箱数（单一数据源，避免重复扫描）
                final int capacity = fh.getTankCapacity(0);
                final int base = FirmaLifeHardCoreConfig.SERVER.pumpBaseCapacity.get();
                final int bonus = FirmaLifeHardCoreConfig.SERVER.tankCapacityBonus.get();
                final int tankCount = bonus > 0 ? (capacity - base) / bonus : 0;

                int rpm = 0;
                final var rotation = pumpBe.getRotationNode().rotation();
                if (rotation != null) {
                    rpm = (int) Math.floor((rotation.positiveSpeed() / Mth.TWO_PI) * 20.0f * 60.0f);
                }

                final int pressure = pumpY + tankCount + rpm - firmalifehardcore$sprinklerY;
                if (pressure < 0) {
                    return false;
                }

                // 探测（drain=false）：水位与压力已通过检查即视为连接有效，不扣水
                if (!drain) {
                    return true;
                }

                final FluidStack drained = fh.drain(use, FluidAction.EXECUTE);
                return !drained.isEmpty();
            }

            // 非泵方块：若是水箱则继续向下扫，否则终止
            if (level.getBlockState(cursor).getBlock() != FLBlocks.IRRIGATION_TANK.get()) {
                return false;
            }
            cursor.move(0, -1, 0);
        }

        return false;
    }

}
