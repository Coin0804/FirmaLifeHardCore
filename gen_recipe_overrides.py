"""生成禁用 Firmalife 气候站与温室结构方块配方所需的 data 覆盖文件。
每个文件内容为 {}，放在与 Firmalife 配方相同的路径下。
FirmaLifeHardCore 作为后加载模组，其 data 会自动覆盖 Firmalife 的配方。

不删除：种植盆、温室设备（sprinkler/picker/sweeper/pumping/irrigation）
"""

import os

RESOURCES = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "src", "main", "resources"
)
RECIPE_DIR = os.path.join(RESOURCES, "data", "firmalife", "recipe", "crafting")
os.makedirs(RECIPE_DIR, exist_ok=True)

GREENHOUSE_MATERIALS = [
    "rusted_iron", "iron",
    "copper", "exposed_copper", "weathered_copper", "oxidized_copper",
    "treated_wood", "weathered_treated_wood",
    "stainless_steel",
]
GREENHOUSE_TYPES = [
    "door", "panel_roof", "panel_wall", "port",
    "roof", "roof_top", "trapdoor", "wall",
]

RECIPE_IDS = ["climate_station"]

for mat in GREENHOUSE_MATERIALS:
    for typ in GREENHOUSE_TYPES:
        RECIPE_IDS.append(f"{mat}_greenhouse_{typ}")
        # 氧化材质的清洁配方
        if mat in ("rusted_iron", "exposed_copper", "weathered_copper",
                    "oxidized_copper", "weathered_treated_wood"):
            RECIPE_IDS.append(f"{mat}_greenhouse_{typ}_cleaning")

count = 0
for recipe_id in RECIPE_IDS:
    filepath = os.path.join(RECIPE_DIR, f"{recipe_id}.json")
    with open(filepath, "w", encoding="utf-8") as f:
        f.write("{}")
    count += 1

print(f"生成完成: {count} 个配方覆盖文件")
print(f"输出目录: {RECIPE_DIR}")
print(f"气候站: 1, 温室结构: 9材质*8类型 = 72, 清洁配方: 42")
