# TODO — FirmaLife HardCore

## 已完成

### 地窖系统
- [x] CellarDetector — BFS floodfill + 3 路分类 + 棚顶/温室检测
- [x] CellarTracker — 事件驱动 + tick 限量处理 + 健康检查 + 持久化
- [x] 保鲜等级 — 温度 tier（≤0/≤8/≤16°C → SHELVED_3/2/1）
- [x] 容器保鲜 — FoodShelf/Hanger/LargeVessel Mixin

### 温室系统
- [x] 棚顶检测 — 屋顶 + glass tag + canSeeSky → canopyRatio
- [x] 温室判定 — canopyRatio ≥ greenhouseGlassRatio（默认 50%）
- [x] 温度公式 — baseTemp = 4 + 40 × canopyRatio，T = baseTemp + (T_out − baseTemp) × (1 − avgR)
- [x] ClimateMixin — 4 重载全覆盖，拦截 TFC 温度查询
- [x] 种植盆通知 — GREENHOUSE 类型 + Integer.MAX_VALUE tier
- [x] 种植盆触发 — #firmalife:planters tag → 放置/破坏时重检

### 配方
- [x] 禁用气候站和温室结构方块配方（data pack 覆盖 113 个）

### Jade
- [x] 滑坡支撑状态 tooltip
- [x] 作物室内温度 tooltip — 服务端查温 + 自动同步 + 适宜度显示

### 带支撑土
- [x] 8 种土壤 × 2 状态 + TFC 支撑集成 + Jade tooltip

### 标签
- [x] 热阻 high/medium/low + greenhouse_roof + TFC grass 高热阻

---

## 待处理

### Bug
- [ ] 温室不成型 — 偶现，诊断 log 已加，待复现

### 打磨
- [ ] 爆炸/塌方拆墙验证
- [ ] 大型空间性能测试
- [ ] 诊断 log 注释掉（Debug 期结束后）
