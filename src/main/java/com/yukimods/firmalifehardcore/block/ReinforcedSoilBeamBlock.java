package com.yukimods.firmalifehardcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;

public class ReinforcedSoilBeamBlock extends Block {

    public static final BooleanProperty AXIS_X = ReinforcedSoilBlock.AXIS_X;
    public static final BooleanProperty AXIS_Z = ReinforcedSoilBlock.AXIS_Z;

    final ReinforcedSoilType soilType;

    public ReinforcedSoilBeamBlock(ReinforcedSoilType soilType) {
        super(Properties.of()
            .mapColor(MapColor.DIRT).strength(1.8f, 6.0f)
            .sound(SoundType.GRAVEL).requiresCorrectToolForDrops());
        this.soilType = soilType;
        registerDefaultState(stateDefinition.any().setValue(AXIS_X, false).setValue(AXIS_Z, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS_X, AXIS_Z);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide()) trySwapToNormal(level, pos);
    }

    static void trySwapToNormal(Level level, BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (!(b instanceof ReinforcedSoilBeamBlock beam)) return;
        boolean x = ReinforcedSoilBlock.hasSupportOnAxis(level, pos, Direction.Axis.X);
        boolean z = ReinforcedSoilBlock.hasSupportOnAxis(level, pos, Direction.Axis.Z);
        if (ReinforcedSoilBlock.hasBothSupportOnAxis(level, pos, Direction.Axis.X)
            || ReinforcedSoilBlock.hasBothSupportOnAxis(level, pos, Direction.Axis.Z)) {
            level.setBlock(pos, level.getBlockState(pos).setValue(AXIS_X, x).setValue(AXIS_Z, z), 3);
        } else {
            level.setBlock(pos, ModBlocks.getAsBlock(beam.soilType)
                .defaultBlockState().setValue(AXIS_X, x).setValue(AXIS_Z, z), 3);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
                                        BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.getAsBlock(soilType));
    }
}
