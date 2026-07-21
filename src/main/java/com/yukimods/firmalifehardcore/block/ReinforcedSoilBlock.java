package com.yukimods.firmalifehardcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * "带支撑的xx土" — 夯实泥土，同时充当竖直和水平支撑。
 *
 * 特性：
 * - 热阻 HIGH (0.80)，通过 tag 自动映射
 * - 不会塌方/滑坡（不在 tfc:can_landslide 中）
 * - 充当 TFC 支撑（通过 TFC DataManager 注册 tfc:support）
 * - 破坏时掉落原版泥土，不掉落支撑梁
 */
public class ReinforcedSoilBlock extends Block {

    public static final String ID = "reinforced_dirt";

    public ReinforcedSoilBlock() {
        super(Properties.of()
            .mapColor(MapColor.DIRT)
            .strength(1.5f, 6.0f)       // 比普通泥土硬
            .sound(SoundType.PACKED_MUD)
            .requiresCorrectToolForDrops()
        );
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
                                        BlockPos pos, Player player) {
        // 拾取时返回原版泥土（gives the player dirt when pick-blocking）
        return new ItemStack(net.minecraft.world.level.block.Blocks.DIRT);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    /**
     * TFC 支撑兼容：使 TFC Support 系统认可此方块。
     * 通过 TFC DataManager 的 tfc:support 数据包注册。
     * 见: kubejs/data/tfc/tfc/support/reinforced_soil.json
     */
}
