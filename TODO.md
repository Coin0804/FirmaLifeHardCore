# TODO — FirmaLife HardCore

## 近期完成 (2026-07-21)

- [x] **项目骨架** — build.gradle, gradle.properties, settings.gradle, gradlew, .gitignore, LICENSE
- [x] **Mod 主类 + 配置** — Attachment 注册、事件注册、指令注册、NeoForge Config
- [x] **核心引擎 CellarDetector** — BFS floodfill + 3 级热阻计算 + 门状态 + 双门加成
- [x] **全局追踪 CellarTracker** — 每 tick 限量处理、dirty 队列、定期清理
- [x] **事件驱动 CellarEventHandler** — BlockEvent 过滤 → markDirty
- [x] **工具类** — ThermalConductivity, CellarSpace, ContainerModifiers, CellarDebugInfo
- [x] **Mixin (P2)** — FoodShelfBlockEntity + HangerBlockEntity getFoodTrait
- [x] **调试指令** — `/firmalifehardcore cellar info|recalc|list`
- [x] **内置 tag JSON** — high/medium/low/cellar_doors/container_blocks/reinforced_soils
- [x] **KubeJS tag 脚本** — `kubejs/server_scripts/firmalifehardcore_tags.js`
- [x] **ReinforcedSoilBlock** — 带支撑的土方块类

---

## Phase 1-A: 带支撑土交互

- [ ] **方块注册** — DeferredRegister<Block> 注册 ReinforcedSoilBlock
- [ ] **物品注册** — BlockItem for reinforced_dirt
- [ ] **锤+支撑梁交互** — UseOnContext 事件：副手支撑梁 + 主手锤 → 泥土变 reinforced_dirt
- [ ] **向下堆叠** — 潜行右键 → 最多向下 3 格，消耗对应支撑梁
- [ ] **TFC Support 集成** — 注册 `#firmalifehardcore:reinforced_soils` 到 TFC DataManager `tfc:support`
- [ ] **纹理** — 泥土 + 木梁横截面纹理（可先复用 packed_mud）

## Phase 3: 扩展容器 ClimateReceiver

- [ ] **JarbnetBlockEntity Mixin** — 实现 ClimateReceiver，类似 FoodShelf 的保鲜 trait
- [ ] **KegBlockEntity Mixin** — 地窖温度影响酒类陈化速度
- [ ] **VatBlockEntity Mixin** — 密封状态下根据温度调整内容物腐败
- [ ] **WineShelfBlockEntity Mixin** — 地窖中减少酒品质衰减

## Phase 4: 温室系统

- [ ] **GreenhouseDetector** — 类似 CellarDetector，检测天空光 + 屋顶封闭
- [ ] **CellarTracker → SpaceTracker 重构** — 统一管理地窖和温室空间
- [ ] **植物生长温度结合** — 温室 T_indoor 影响种植盆生长速度

## 打磨

- [ ] **版本号 bump** → 0.0.2
- [ ] **JM/TFC API 兼容性测试** — 确认 TagKey 引用正确
- [ ] **Mixin 签名稳定性** — Firmalife 2.2.1 已测试，后续版本需验证
- [ ] **大型空间性能测试** — 50×50×20 级别

## 可选增强

- [ ] **JEI/EMI 信息提示** — 热阻等级在方块 tooltip 中显示
- [ ] **Jade/TOP 集成** — 地窖容器显示当前有效温度和保鲜等级
- [ ] **Ponder 思索场景** — 地窖建造教程 / 保温材料对比
- [ ] **地窖温度粒子效果** — 冷雾粒子在地窖入口
