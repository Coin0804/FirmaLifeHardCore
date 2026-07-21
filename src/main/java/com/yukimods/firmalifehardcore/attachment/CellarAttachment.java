package com.yukimods.firmalifehardcore.attachment;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge Attachment — 每个 ServerLevel 持有一个 CellarTracker 实例。
 */
public class CellarAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FirmaLifeHardCore.MOD_ID);

    /** 自动创建 CellarTracker 的 default factory */
    private static final Supplier<CellarTracker> FACTORY = CellarTracker::new;

    public static final Supplier<AttachmentType<CellarTracker>> CELLAR_TRACKER =
        ATTACHMENT_TYPES.register("cellar_tracker",
            () -> AttachmentType.builder(FACTORY).build()
        );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    /**
     * 从 ServerLevel 获取 CellarTracker（自动创建，永不返回 null）。
     */
    public static CellarTracker get(ServerLevel level) {
        return level.getData(CELLAR_TRACKER.get());
    }
}
