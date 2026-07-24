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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;

public class ReinforcedSoilBlock extends Block {

    public static final BooleanProperty AXIS_X = BooleanProperty.create("axis_x");
    public static final BooleanProperty AXIS_Z = BooleanProperty.create("axis_z");

    final ReinforcedSoilType soilType;

    public ReinforcedSoilBlock(ReinforcedSoilType soilType) {
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
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ReinforcedSoilBlock normal)) return;
        // 属性：任意一端有支撑即标记（用于贴图）
        boolean x = hasSupportOnAxis(level, pos, Direction.Axis.X);
        boolean z = hasSupportOnAxis(level, pos, Direction.Axis.Z);
        // 转横梁：两端都有支撑才转
        if (hasBothSupportOnAxis(level, pos, Direction.Axis.X)
            || hasBothSupportOnAxis(level, pos, Direction.Axis.Z)) {
            level.setBlock(pos, ModBlocks.getBeamAsBlock(normal.soilType)
                .defaultBlockState().setValue(AXIS_X, x).setValue(AXIS_Z, z), 3);
        } else {
            level.setBlock(pos, state.setValue(AXIS_X, x).setValue(AXIS_Z, z), 3);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
                                        BlockPos pos, Player player) {
        return new ItemStack(this);
    }

    /** 任意一端有支撑（用于属性标记→贴图） */
    static boolean hasSupportOnAxis(Level level, BlockPos pos, Direction.Axis axis) {
        Direction a = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        Direction b = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        return hasBeamEndpoint(level, pos, a) || hasBeamEndpoint(level, pos, b);
    }

    /** 两端都有支撑（用于判定是否转横梁） */
    static boolean hasBothSupportOnAxis(Level level, BlockPos pos, Direction.Axis axis) {
        Direction a = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        Direction b = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        return hasBeamEndpoint(level, pos, a) && hasBeamEndpoint(level, pos, b);
    }

    private static boolean hasBeamEndpoint(Level level, BlockPos pos, Direction dir) {
        BlockPos neighbor = pos.relative(dir);
        return Helpers.isBlock(level.getBlockState(neighbor), TFCTags.Blocks.SUPPORT_BEAMS);
    }
}
