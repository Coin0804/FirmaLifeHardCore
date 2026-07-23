# TODO — FirmaLife HardCore

## 已完成 (2026-07-21/22)

- [x] **项目骨架** — build.gradle, gradle.properties, settings.gradle, gradlew, .gitignore, LICENSE, README
- [x] **Mod 主类 + 配置** — Attachment 注册、事件注册、指令注册、NeoForge Config
- [x] **CellarDetector** — BFS floodfill + classify() 三路分发、V/H 分离半径、maxSize=H²×V
- [x] **CellarTracker** — dirty 队列、延迟破环(5tick)、健康检查(tick%100)、合并去重
- [x] **CellarEventHandler** — Place/Break/LevelTick/LevelEvent(Save/Load)
- [x] **CellarSavedData** — 空间持久化，随世界保存/加载
- [x] **Mixin FoodShelfBlockEntity** — setValid 存 tier、getFoodTrait 按等级返回、onLoadAdditional 跳过、use() 取物全清
- [x] **Mixin HangerBlockEntity** — getFoodTrait 通过 CellarTierAccessor 读 tier
- [x] **CellarTierAccessor** — 接口解耦父子 Mixin
- [x] **门处理** — 统一算关闭墙，双门加成查 facing+opposite，半门跳过
- [x] **保鲜等级** — updateContainerAt → setValid 传 tier → getFoodTrait 读 tier
- [x] **调试指令** — `/firmalifehardcore cellar info|recalc|list`
- [x] **内置 tag JSON** — high/medium/low/cellar_doors/container_blocks/reinforced_soils/reinforceable
- [x] **KubeJS tag 脚本** — firmalifehardcore_tags.js
- [x] **去噪** — 删除 CellarDetector/CellarTracker/CellarEventHandler 所有方块更新级日志
- [x] **OBSTACLE 穿透** — BFS 遇到障碍物(食物架等)继续探索后方空间

---

## Phase 1-A: 带支撑土交互 ✅

- [x] **ReinforcedSoilType 枚举** — 8 种 TFC 土壤变体
- [x] **ReinforcedSoilBlock + ReinforcedSoilBeamBlock** — 竖梁/横梁双态，`neighborChanged` 自动切换
- [x] **ModBlocks / ModItems / ModCreativeTab** — DeferredRegister 注册 16 方块 + 8 BlockItem
- [x] **ReinforcedDirtHandler** — 主手梁+副手锤右键转换、潜行向下堆叠、创造模式不消耗
- [x] **TFC Support 集成** — `tfc:support` 数据 + `#tfc:support_beams` tag
- [x] **纹理** — TFC 原版泥土+中间方形木梁截面
- [x] **自动连接** — `onPlace`+`neighborChanged`，两侧有端点→变横梁，端点消失→退回
- [x] **Tag 体系** — reinforceable, reinforced_soils, mineable/shovel, thermal_insulation/high
- [x] **Loot table** — beam 变体掉落普通变体
- [x] **去噪** — 删除 [Swap] 等所有调试日志，消除 Render+Server 双重输出
- [x] **热阻调整** — MEDIUM 0.45→0.55（对应木板 50-60% 热阻率）
- [x] **hasBeamEndpoint** — 简化为紧邻 1 格检查

## Phase 1-B: Jade 集成 ✅

- [x] **JadePlugin** — `@WailaPlugin` 自动发现
- [x] **ReinforcedDirtComponentProvider** — 滑坡方块显示支撑状态
- [x] **可选依赖** — build.gradle compileOnly + mods.toml optional

---

## Phase 3: 扩展容器保鲜

### 已支持

| 容器 | Mixin | 机制 |
|------|-------|------|
| FoodShelfBlockEntity | `FoodShelfBlockEntityMixin` | ClimateReceiver + CellarTierAccessor → 三级 SHELVED trait |
| HangerBlockEntity | `HangerBlockEntityMixin` | ClimateReceiver + CellarTierAccessor → 三级 HUNG trait |
| LargeVesselBlockEntity | `LargeVesselBlockEntityMixin` | ClimateReceiver + onSeal 追加 SHELVED / onUnseal 清除 |

共享工具类：`CellarInventoryHelper` (trait 等级映射、清除、归一化)

### 不支持

| 容器 | 原因 |
|------|------|
| Jarbnet | 罐内食物靠封装 (canning/pickling) 保鲜，非 cellar 机制 |
| Keg / WineShelf | 酒类不含 FoodCapability |
| TFC BarrelBlockEntity | 配方驱动密封系统，非简单 trait |
| TFC Chest / Crate | 通用存储，无保鲜机制 |
| TFC Small Vessel | 物品非方块实体，不在地窖检测范围 |

---

## 待处理

### Phase 4: 温室系统

- [ ] GreenhouseDetector
- [ ] SpaceTracker 重构
- [ ] 植物生长温度结合

### 打磨

- [ ] 爆炸/塌方拆墙验证
- [ ] 大型空间性能测试
