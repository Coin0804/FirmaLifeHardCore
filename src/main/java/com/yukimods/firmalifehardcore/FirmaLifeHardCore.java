package com.yukimods.firmalifehardcore;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import com.yukimods.firmalifehardcore.block.ModBlocks;
import com.yukimods.firmalifehardcore.config.FirmaLifeHardCoreConfig;
import com.yukimods.firmalifehardcore.event.CellarEventHandler;
import com.yukimods.firmalifehardcore.event.ReinforcedDirtHandler;
import com.yukimods.firmalifehardcore.item.ModItems;
import com.yukimods.firmalifehardcore.util.CellarDebugInfo;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import com.yukimods.firmalifehardcore.util.PumpTickManager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import com.eerussianguy.firmalife.common.blockentities.FLBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FirmaLifeHardCore.MOD_ID)
public class FirmaLifeHardCore {

    public static final String MOD_ID = "firmalifehardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("FirmaLife HardCore");

    public FirmaLifeHardCore(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("FirmaLife HardCore initializing — VS-style cellar/greenhouse global tracker");

        // 注册配置
        container.registerConfig(ModConfig.Type.SERVER, FirmaLifeHardCoreConfig.getServerSpec());

        // 注册 Attachment
        CellarAttachment.register(modEventBus);

        // 注册方块/物品/创造栏
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);

        // 注册 FluidHandler 能力给水泵站（管道连接需要）
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> {
            event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                FLBlockEntities.PUMPING_STATION.get(),
                (be, side) -> (IFluidHandler) be
            );
        });

        // 注册事件处理器
        NeoForge.EVENT_BUS.register(CellarEventHandler.class);
        NeoForge.EVENT_BUS.register(ReinforcedDirtHandler.class);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel) {
                PumpTickManager.tickAll();
            }
        });

        LOGGER.info("FirmaLife HardCore initialized");
    }

    // ===== /flhc cellar info|recalc|list|clear =====

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("flhc")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("cellar")
                    .then(Commands.literal("info")
                        .executes(this::cmdCellarInfo))
                    .then(Commands.literal("recalc")
                        .requires(src -> src.hasPermission(4))
                        .executes(this::cmdCellarRecalc))
                    .then(Commands.literal("list")
                        .executes(this::cmdCellarList))
                    .then(Commands.literal("clear")
                        .requires(src -> src.hasPermission(4))
                        .executes(this::cmdCellarClear))
                )
        );
        LOGGER.info("[Server] Registered /flhc cellar info|recalc|list|clear");
    }

    private int cmdCellarInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();

        CellarTracker tracker = CellarAttachment.get(level);
        CellarDebugInfo info = tracker.getDebugInfo(pos, level);
        ctx.getSource().sendSuccess(() -> Component.literal(info.format()), false);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdCellarRecalc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();

        CellarTracker tracker = CellarAttachment.get(level);
        tracker.forceRecalc(pos, level);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已强制重算位置 " + pos.toShortString() + " 的地窖状态"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdCellarList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();

        CellarTracker tracker = CellarAttachment.get(level);
        String list = tracker.listAll();
        ctx.getSource().sendSuccess(() -> Component.literal(list), false);
        return Command.SINGLE_SUCCESS;
    }

    private int cmdCellarClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();

        CellarTracker tracker = CellarAttachment.get(level);
        tracker.clearAll(level);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已清空所有地窖缓存，下次方块变更将重新检测"), true);
        return Command.SINGLE_SUCCESS;
    }
}
