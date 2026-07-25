# TODO — FirmaLife HardCore

## 已完成 (2026-07-26)

### 温度系统重做
- [x] `getAverageTemperature()` Mixin 拦截 — 灌木/果树气候判断走温室年均温
- [x] 温度实时计算 — `effectiveTemperature` 从字段改为 `getEffectiveTemperature(Level)` 方法
- [x] 删 `CellarResult` — `query()` 直接返回 `CellarSpace`，消除冗余中间层
- [x] `getAverageTemperature()` 返回基准 + (室外年均 − 基准) × (1 − avgR)

### 检测参数调整
- [x] `maxHorizontalSpan` 15→24, `maxVerticalSpan` 5→8

### 修复
- [x] 屋顶检测 — `isRoof` 加入 `obstaclePositions` 判定（屋顶下有食品架也能识别）
- [x] Tag 崩溃 — `medium.json` 5 个虚假标签引用 → `#c:ores` + `tfc:thatch`
- [x] i18n 跨命名空间 — `assets/firmalife/lang/` 覆盖 4 个种植盆 Jade 键
- [x] CropTemperatureProvider — tooltip 合并为一行

### 帕秋莉手册迁移
- [x] 从覆盖 Firmalife 条目改为注册独立 `firmalifehardcore` 分类
- [x] 温度计图标 + zh_cn 名称"群峦生活硬核版"

### 研究
- [x] 63 个 Plantable 分类：29 TFC 作物 + 20 果树灌木 + 14 Firmalife 独有
- [x] TFC 灌木/果树 vs 种植盆生长机制分析

---

## 已完成 (2026-07-25)

### 地窖系统
- [x] CellarDetector — BFS floodfill + AABB 动态追踪 + 每步 OOB + OBSTACLE 防泄漏
- [x] CellarTracker — 事件驱动 + tick 限量处理 + 健康检查 + 持久化 + 规范种子
- [x] 保鲜等级 — 温度 tier（≤0/≤8/≤16°C → SHELVED_3/2/1）
- [x] 容器保鲜 — FoodShelf/Hanger/LargeVessel Mixin
- [x] Seed-Independent 检测 — 移除 BoundingBox/maxSize

### 热阻系统
- [x] 三级热阻 tag（HIGH 0.75 / MEDIUM 0.55 / LOW 0.25）
- [x] 双门 4× 倍率
- [x] `#firmalifehardcore:cellar_containers` tag

### 温室系统
- [x] 棚顶检测 — 屋顶 + greenhouse_roof tag + canSeeSky → canopyRatio
- [x] ClimateMixin — getInstantTemperature 4 重载拦截
- [x] 种植盆通知 — GREENHOUSE + Integer.MAX_VALUE tier

### 温度计
- [x] ThermometerMixin + ThermometerTemperatureProvider + CropTemperatureProvider

### 带支撑土视觉
- [x] 6 种纹理 × 8 变体 + axis_x/axis_z 属性 + 破坏粒子修复

### 配方
- [x] 禁用气候站和温室结构方块配方

### Bug 修复
- [x] Food shelf 放置不触发检测 / 温度计不读温室温度 / BFS 岩石泄漏 / L 形 seedPos

---

## 待处理

### 功能
- [ ] 爆炸/塌方拆墙时触发重检
- [ ] 大型空间性能测试
- [ ] Plantable 加温度检查 — 温室温度不够时暂停生长（不死）

### 打磨
- [ ] 诊断 log 降级或移除（DEBUG → TRACE）
