package com.yukimods.firmalifehardcore.mixin;

import com.yukimods.firmalifehardcore.attachment.CellarAttachment;
import com.yukimods.firmalifehardcore.util.CellarSpace;
import com.yukimods.firmalifehardcore.util.CellarTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截 TFC Climate.getInstantTemperature() 全部 4 个重载及 getAverageTemperature()，
 * 对地窖/温室坐标返回计算温度。使用全限定 JVM 描述符精确区分重载。
 */
@Mixin(value = Climate.class, remap = false)
public class ClimateMixin {

    /* (Level,BlockPos,long,int) → JVM: (Lnet/...Level;Lnet/...BlockPos;JI)F */
    @Inject(method = "getInstantTemperature(Lnet/minecraft/world/level/Level;"
        + "Lnet/minecraft/core/BlockPos;JI)F",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetInstant_4pr(Level level, BlockPos pos,
        long calendarTick, int daysInMonth, CallbackInfoReturnable<Float> cir)
    {
        interceptInstant(level, pos, cir);
    }

    /* (Level,BlockPos,ICalendar,long) → JVM: (Lnet/...Level;Lnet/...BlockPos;Lnet/...ICalendar;J)F */
    @Inject(method = "getInstantTemperature(Lnet/minecraft/world/level/Level;"
        + "Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/calendar/ICalendar;J)F",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetInstant_4ICal(Level level, BlockPos pos,
        ICalendar calendar, long calendarTick, CallbackInfoReturnable<Float> cir)
    {
        interceptInstant(level, pos, cir);
    }

    /* (Level,BlockPos,ICalendar) → JVM: (Lnet/...Level;Lnet/...BlockPos;Lnet/...ICalendar;)F */
    @Inject(method = "getInstantTemperature(Lnet/minecraft/world/level/Level;"
        + "Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/calendar/ICalendar;)F",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetInstant_3pr(Level level, BlockPos pos,
        ICalendar calendar, CallbackInfoReturnable<Float> cir)
    {
        interceptInstant(level, pos, cir);
    }

    /* (Level,BlockPos) → JVM: (Lnet/...Level;Lnet/...BlockPos;)F */
    @Inject(method = "getInstantTemperature(Lnet/minecraft/world/level/Level;"
        + "Lnet/minecraft/core/BlockPos;)F",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetInstant_2pr(Level level, BlockPos pos,
        CallbackInfoReturnable<Float> cir)
    {
        interceptInstant(level, pos, cir);
    }

    /* getAverageTemperature(Level,BlockPos) */
    @Inject(method = "getAverageTemperature(Lnet/minecraft/world/level/Level;"
        + "Lnet/minecraft/core/BlockPos;)F",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetAverage(Level level, BlockPos pos,
        CallbackInfoReturnable<Float> cir)
    {
        if (!(level instanceof ServerLevel sl)) return;

        CellarTracker tracker = CellarAttachment.get(sl);
        CellarSpace space = tracker.query(pos);
        if (space != null) {
            cir.setReturnValue(space.getAverageTemperature(level));
        }
    }

    private static void interceptInstant(Level level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(level instanceof ServerLevel sl)) return;

        CellarTracker tracker = CellarAttachment.get(sl);
        CellarSpace space = tracker.query(pos);
        if (space != null) {
            cir.setReturnValue(space.getEffectiveTemperature(level));
        }
    }
}
