package com.yukimods.firmalifehardcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;

public class ReinforcedSoilBeamBlock extends Block {

    final ReinforcedSoilType soilType;

    public ReinforcedSoilBeamBlock(ReinforcedSoilType soilType) {
        super(Properties.of()
            .mapColor(MapColor.DIRT).strength(1.8f, 6.0f)
            .sound(SoundType.GRAVEL).requiresCorrectToolForDrops());
        this.soilType = soilType;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide()) trySwapToNormal(level, pos);
    }

    static void trySwapToNormal(Level level, BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (b instanceof ReinforcedSoilBeamBlock beam && !ReinforcedSoilBlock.scanConnected(level, pos)) {
            level.setBlock(pos, ModBlocks.getAsBlock(beam.soilType).defaultBlockState(), 3);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
                                        BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.getAsBlock(soilType));
    }
}
