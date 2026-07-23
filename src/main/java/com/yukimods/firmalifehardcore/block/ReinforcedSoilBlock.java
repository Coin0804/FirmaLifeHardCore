package com.yukimods.firmalifehardcore.block;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;

public class ReinforcedSoilBlock extends Block {

    final ReinforcedSoilType soilType;

    public ReinforcedSoilBlock(ReinforcedSoilType soilType) {
        super(Properties.of()
            .mapColor(MapColor.DIRT).strength(1.8f, 6.0f)
            .sound(SoundType.GRAVEL).requiresCorrectToolForDrops());
        this.soilType = soilType;
    }

    public ReinforcedSoilType getSoilType() { return soilType; }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide()) trySwapToBeam(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide()) trySwapToBeam(level, pos);
    }

    static void trySwapToBeam(Level level, BlockPos pos) {
        if (scanConnected(level, pos)) {
            level.setBlock(pos, ModBlocks.getBeamAsBlock(
                ((ReinforcedSoilBlock) level.getBlockState(pos).getBlock()).soilType
            ).defaultBlockState(), 3);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
                                        BlockPos pos, Player player) {
        return new ItemStack(this);
    }

    static boolean scanConnected(Level level, BlockPos pos) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction a = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            Direction b = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
            if (hasBeamEndpoint(level, pos, a) && hasBeamEndpoint(level, pos, b))
                return true;
        }
        return false;
    }

    private static boolean hasBeamEndpoint(Level level, BlockPos pos, Direction dir) {
        BlockPos neighbor = pos.relative(dir);
        return Helpers.isBlock(level.getBlockState(neighbor), TFCTags.Blocks.SUPPORT_BEAMS);
    }
}
