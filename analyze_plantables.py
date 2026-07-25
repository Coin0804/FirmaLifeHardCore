import json, os, glob

plantable_dir = "../.fork/firmalife/bin/main/data/firmalife/firmalife/plantable"
tfc_climate_dir = "../.fork/TerraFirmaCraft/bin/main/data/tfc/tfc/climate_range/crop"

# 所有 plantable
plantables = {}
for f in sorted(glob.glob(plantable_dir + "/*.json")):
    d = json.load(open(f, encoding='utf-8'))
    name = os.path.basename(f).replace('.json', '')
    plantables[name] = d['crop']['id']

# TFC climate crops
tfc_crops = set()
for f in glob.glob(tfc_climate_dir + "/*.json"):
    tfc_crops.add(os.path.basename(f).replace('.json', ''))

print("=" * 60)
print("PLANTABLE 分析：哪些有 TFC 气候数据（= 有最低温度）")
print("=" * 60)

has_tfc = []
no_tfc = []

for name, crop_id in sorted(plantables.items()):
    # 从 crop_id 提取作物名：取最后一段；尝试多种匹配方式
    # tfc:food/wheat → wheat; tfc:alfalfa → alfalfa
    id_path = crop_id.split(':')[1]  # "food/wheat" or "alfalfa"
    crop_name = id_path.split('/')[-1]  # "wheat" or "alfalfa"
    has_climate = crop_name in tfc_crops or id_path in tfc_crops
    if has_climate:
        has_tfc.append((name, crop_id, crop_name))
    else:
        no_tfc.append((name, crop_id, crop_name))

print(f"\n✅ 已有 TFC 气候数据 ({len(has_tfc)} 个):")
print("-" * 40)
for name, crop_id, crop_name in has_tfc:
    print(f"  {name:25s} → {crop_id}")

print(f"\n❌ 没有 TFC 气候数据 ({len(no_tfc)} 个):")
print("-" * 40)
for name, crop_id, crop_name in no_tfc:
    print(f"  {name:25s} → {crop_id}")
