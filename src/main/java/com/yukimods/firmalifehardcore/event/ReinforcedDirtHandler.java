package com.yukimods.firmalifehardcore.event;

import com.yukimods.firmalifehardcore.block.ModBlocks;
import com.yukimods.firmalifehardcore.block.ReinforcedSoilBlock;
import com.yukimods.firmalifehardcore.block.ReinforcedSoilType;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;


/**
 * 带支撑土交互处理器。
 *
 * 交互规则：
 * - 主手 TFC 支撑梁 (tfc:support_beams)
 * - 副手锤 (c:tools/hammer)
 * - 右键 TFC 地面方块 (firmalifehardcore:reinforceable) → 转换为对应的带支撑土变体
 * - 潜行右键 → 向下延伸最多 maxDepth 格
 * - 每格消耗 1 支撑梁（主手） + 锤耐久（副手）
 */
public final class ReinforcedDirtHandler {

    private static final TagKey<Item> HAMMERS =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/hammer"));
    private static final TagKey<Item> SUPPORT_BEAMS =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tfc", "support_beams"));
    private static final TagKey<Block> REINFORCEABLE =
        TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("firmalifehardcore", "reinforceable"));

    private ReinforcedDirtHandler() {}

    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK) return;

        Player player = event.getPlayer();
        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();
        ItemStack mainHand = event.getItemStack();
        ItemStack offHand = player.getOffhandItem();

        // 主手梁 + 副手锤：尝试接管梁的放置事件，改为转换泥土
        if (!mainHand.is(SUPPORT_BEAMS) || !offHand.is(HAMMERS)) return;

        // 点击的不是可加固方块 → 不接管，回退正常放置梁
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!clickedState.is(REINFORCEABLE)) return;

        // 计算本次可转化的格数（世界状态与玩家输入在两侧同步，接管决定两侧一致）
        int maxDepth = FirmaLifeHardCoreConfig.SERVER.reinforcedSoilMaxDepth.get();
        int targetDepth = 1;
        if (player.isShiftKeyDown()) {
            int consecutive = 0;
            BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
            scan.set(clickedPos);
            for (int i = 0; i < maxDepth; i++) {
                if (level.getBlockState(scan).is(REINFORCEABLE)) {
                    consecutive++;
                    scan.move(Direction.DOWN);
                } else break;
            }
            targetDepth = consecutive;
        }

        // 主手梁数量不足 → 无法转化，回退正常放置梁
        int available = mainHand.getCount();
        if (targetDepth > available) targetDepth = available;
        if (targetDepth <= 0) return;

        // 转化可成功 → 接管事件，阻止梁的正常放置
        event.setCanceled(true);
        event.setCancellationResult(ItemInteractionResult.SUCCESS);
        if (level.isClientSide()) return;

        ServerLevel sl = (ServerLevel) level;
        int durabilityCost = FirmaLifeHardCoreConfig.SERVER.hammerDurabilityCost.get();

        ReinforcedSoilType soilType = ReinforcedSoilType.fromBlock(clickedState.getBlock());
        ReinforcedSoilBlock targetBlock = ModBlocks.get(soilType).get();
        BlockState reinforcedState = targetBlock.defaultBlockState();

        BlockPos.MutableBlockPos place = new BlockPos.MutableBlockPos();
        place.set(clickedPos);
        int converted = 0;

        for (int i = 0; i < targetDepth; i++) {
            if (!level.getBlockState(place).is(REINFORCEABLE)) break;
            if (!level.setBlock(place, reinforcedState, 3)) break;
            converted++;
            place.move(Direction.DOWN);
        }

        if (converted > 0) {
            if (!player.isCreative()) {
                mainHand.shrink(converted);
                offHand.hurtAndBreak(durabilityCost * converted, sl, player, new Consumer<Item>() {
                    @Override
                    public void accept(Item item) {
                    }
                });
            }
            level.playSound(null, clickedPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        }
    }
}
