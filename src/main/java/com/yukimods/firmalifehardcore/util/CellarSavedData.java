package com.yukimods.firmalifehardcore.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 持久化 CellarTracker 的空间数据。每个 ServerLevel 一个实例。
 */
public class CellarSavedData extends SavedData {

    private static final String ID = "firmalifehardcore_cellars";
    private CompoundTag cellarData = new CompoundTag();

    public static CellarSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(CellarSavedData::new, CellarSavedData::load), ID);
    }

    public CompoundTag getCellarData() {
        return cellarData;
    }

    public void setCellarData(CompoundTag tag) {
        this.cellarData = tag;
        setDirty(); // 标记需要保存
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("cellars", cellarData);
        return tag;
    }

    private static CellarSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CellarSavedData data = new CellarSavedData();
        if (tag.contains("cellars"))
            data.cellarData = tag.getCompound("cellars");
        return data;
    }
}
