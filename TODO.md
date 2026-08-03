# TODO — FirmaLife HardCore

## 已完成 (2026-08-03) — 灌溉系统 bug 修复

### 洒水器 drain 契约
- [x] `searchForFluid` 尊重 drain 参数 — 探测调用（tfcfertigation 施肥周期，每 tick）不再误扣水泵水；仅水位 + 压力检查
- [x] 探测结论不依赖 SIMULATE 返回值 — 直接基于状态检查（水位 ≥ 所需 + 压力达标）

### 水泵注水
- [x] 维度过滤 — `LevelTickEvent.Post` 对每个已加载 ServerLevel 触发，`tickAll(Level)` 只注水本维度泵（修复多维度重复注水：实测 12 mB/s = 4 倍）
- [x] 注水周期 20 → 80 tick — 与洒水器浇水同周期，15 RPM 平衡点 `25.0 → 25 mB/80tick` 零 (int) 截断，与 5 洒水器扣水精确平衡

---

## 已完成 (2026-07-27) — 灌溉系统 Phase 1 + 2

### Phase 1 Hotfix
- [x] 灌溉水箱加入 LOW 热阻标签 — 水箱作为墙壁，管道穿墙不破坏温室密封
- [x] 洒水器 BFS 穿透水箱 — `isPipe`/`isPipeInDirection` Mixin redirect
- [x] 洒水器 Jade tooltip — 禁用原版 HoeOverlay，SprinklerProvider 从 CellarTracker 查询
- [x] 温室拆分检测 — replaceSpace 遗弃位置加入 pendingDiscoveries

### Phase 2 灌溉流体化
- [x] 水泵 FluidTank + IFluidHandler — 动态容量（500 + 上方水箱×500，≤3）
- [x] 每 tick 注水 — PumpTickManager + LevelTickEvent，按 Z 错峰每 80 tick ×80（2026-08-03 由 20 tick 改为 80 tick）
- [x] 洒水器真实消耗 — 5mB/80tick，泵压公式（pumpY + tanks + RPM − sprinklerY）
- [x] 水箱事件驱动扫描 — 放置/破坏水箱时 invalidate 缓存，100 tick 保底重扫
- [x] 持久化 — Mixin override saveAdditional/loadAdditional
- [x] NeoForge 能力注册 — RegisterCapabilitiesEvent 暴露 IFluidHandler
- [x] 诊断日志全部清理
- [x] 帕秋莉手册 + README 更新

---

## 已完成 (2026-07-26)

### 温度系统重做
- [x] `getAverageTemperature()` Mixin 拦截 — 灌木/果树气候判断走温室年均温
- [x] 温度实时计算 — `effectiveTemperature` 从字段改为 `getEffectiveTemperature(Level)` 方法
- [x] 删 `CellarResult` — `query()` 直接返回 `CellarSpace`，消除冗余中间层
- [x] `getAverageTemperature()` 返回基准 + (室外年均 − 基准) × (1 − avgR)

### 检测参数调整
- [x] `maxHorizontalSpan` 15→24, `maxVerticalSpan` 5→8

### 修复
- [x] 屋顶检测 — `isRoof` 加入 `obstaclePositions` 判定
- [x] Tag 崩溃 — `medium.json` 5 个虚假标签引用 → `#c:ores` + `tfc:thatch`
- [x] i18n 跨命名空间 — `assets/firmalife/lang/` 覆盖 4 个种植盆 Jade 键
- [x] CropTemperatureProvider — tooltip 合并为一行

### 帕秋莉手册迁移
- [x] 从覆盖 Firmalife 条目改为注册独立 `firmalifehardcore` 分类

### 研究
- [x] 63 个 Plantable 分类 / TFC 灌木/果树 vs 种植盆生长机制分析

---

## 待处理

### 功能
- [ ] 爆炸/塌方拆墙时触发重检
- [ ] 大型空间性能测试
- [ ] Plantable 加温度检查 — 温室温度不够时暂停生长（不死）
- [ ] 水箱单独存水（现在是泵集中存储，容量由上方水箱数决定）
