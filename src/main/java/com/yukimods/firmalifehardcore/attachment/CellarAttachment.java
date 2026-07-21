package com.yukimods.firmalifehardcore.attachment;

import com.yukimods.firmalifehardcore.FirmaLifeHardCore;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * NeoForge Attachment — 每个 ServerLevel 持有一个 CellarTracker 实例。
 */
public class CellarAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FirmaLifeHardCore.MOD_ID);

    public static final Supplier<AttachmentType<CellarTracker>> CELLAR_TRACKER =
        ATTACHMENT_TYPES.register("cellar_tracker",
            () -> AttachmentType.builder(() -> (CellarTracker) null).serializeNull().build()
        );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    /**
     * 从 ServerLevel 获取 CellarTracker。
     * 如果尚不存在则自动创建并附着。
     */
    @Nullable
    public static CellarTracker get(ServerLevel level) {
        if (!level.hasData(CELLAR_TRACKER.get())) {
            level.setData(CELLAR_TRACKER.get(), new CellarTracker());
        }
        return level.getData(CELLAR_TRACKER.get());
    }
}
