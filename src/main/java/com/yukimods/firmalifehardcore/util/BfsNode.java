package com.yukimods.firmalifehardcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BFS 节点，用于洒水头管道网络寻路。
 * <p>
 * 必须在非 mixin 包下——Mixin 禁止被覆写代码直接引用 mixin 包内的类型。
 */
public record BfsNode(BlockState state, BlockPos pos, int cost) {}
