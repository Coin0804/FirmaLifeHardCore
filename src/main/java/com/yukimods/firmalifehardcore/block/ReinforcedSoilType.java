package com.yukimods.firmalifehardcore.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * TFC 土壤变体枚举 — 每个变体对应一个带支撑土方块。
 * 变体名称与 TFC SoilBlockType.Variant 保持一致。
 */
public enum ReinforcedSoilType {
    ENTISOL("新成土", "Entisol"),
    ARIDISOL("旱成土", "Aridisol"),
    OXISOL("氧化土", "Oxisol"),
    FLUVISOL("冲积土", "Fluvisol"),
    ANDISOL("火山土", "Andisol"),
    PODZOL("灰化土", "Podzol"),
    ALFISOL("淋溶土", "Alfisol"),
    MOLLISOL("软土", "Mollisol");

    private final String zhName;
    private final String enName;
    private final String tfcDirtId;  // tfc:dirt/<variant>

    ReinforcedSoilType(String zhName, String enName) {
        this.zhName = zhName;
        this.enName = enName;
        this.tfcDirtId = "tfc:dirt/" + name().toLowerCase();
    }

    public String blockName() { return "reinforced_" + name().toLowerCase(); }
    public String zhName() { return zhName; }
    public String enName() { return enName; }

    /** 运行时获取对应的 TFC 泥土物品（掉落/拾取用） */
    public ItemStack getDirtStack() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tfcDirtId));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    /** 从方块注册表键（如 tfc:dirt/entisol）提取变体，不能识别则回退 ENTISOL */
    public static ReinforcedSoilType fromBlockId(String blockId) {
        if (blockId == null) return ENTISOL;
        int slash = blockId.lastIndexOf('/');
        if (slash >= 0) {
            try {
                return valueOf(blockId.substring(slash + 1).toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return ENTISOL;
    }

    /** 从方块实例提取变体 */
    public static ReinforcedSoilType fromBlock(Block block) {
        return fromBlockId(BuiltInRegistries.BLOCK.getKey(block).toString());
    }
}
