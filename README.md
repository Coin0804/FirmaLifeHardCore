# FirmaLife HardCore

[English](#english) | [中文](#中文)

---

## English

A NeoForge 1.21.1 mod that completely overhauls Firmalife's cellar and greenhouse mechanics — **no Climate Station required**. Enclosed spaces are auto-detected via BFS floodfill with dynamic AABB tracking. Any insulating blocks form a functional cellar or greenhouse.

### Cellar & Greenhouse System

Enclosed spaces are detected automatically when wall/container blocks are placed or broken. Every solid wall block contributes a **thermal resistance value** based on block tags:

| Tier | Resistance | Typical Blocks |
|------|-----------|----------------|
| **HIGH** | 0.75 | Sealed bricks |
| **MEDIUM** | 0.55 | Stones, ore blocks, dirt, grass, stone bricks, packed mud, planks, logs, wattle, thatch, mud bricks, reinforced soil |
| **LOW** | 0.25 | Glass (blocks/panes), storage blocks (metal, coal, etc.), sandstone, irrigation tank |

Doors count as medium resistance. **Double doors** grant a **4× multiplier**.

**Cellar** effective temperature: `T_indoor = baseTemp + (T_outdoor − baseTemp) × (1 − avgR)`, where `baseTemp = 4°C`. Lower effective temp → better preservation (SHELVED ≤ 16°C, SHELVED_2 ≤ 8°C, SHELVED_3 ≤ 0°C).

**Greenhouse** detection: roof blocks with interior (or interior obstacles) below → roof. Glass roof ≥ 50% visible to sky → greenhouse. `baseTemp = 4 + 40 × canopyRatio`.

**Temperature API**: Both `Climate.getInstantTemperature()` and `Climate.getAverageTemperature()` are intercepted. Instant returns the effective temperature (weather-dependent), average returns the indoor annual mean (stable microclimate, used by bushes/fruit trees for climate suitability).

### Container Preservation

| Container | Mechanism |
|-----------|-----------|
| Food Shelf | 3-tier SHELVED trait via ClimateReceiver |
| Hanger | 3-tier HUNG trait via ClimateReceiver |
| Large Vessel | SHELVED applied on seal, removed on unseal |

All container blocks are auto-detected via `#firmalifehardcore:cellar_containers` tag.

### Thermometer Integration

Thermometers display greenhouse/cellar temperature via redstone signal (server-side) and Jade tooltip ("Indoor Temp"). Outdoor thermometers show TFC's native tooltip.

### Irrigation System Overhaul (Phase 2)

The Firmalife irrigation system (Pumping Station, Irrigation Tank, Sprinklers) has been upgraded with real fluid mechanics.

**Pumping Station** — Now a NeoForge IFluidHandler container:
- Base 500mB storage, +500mB per Irrigation Tank stacked directly above (max 3)
- Draws water from below the pump; fill rate proportional to mechanical rotation speed
- Fluid capacity auto-synced via NeoForge capabilities; compatible with external fluid pipes
- Per-tick fill staggered by Z-coordinate (every 80 ticks, ×80 multiplier to avoid truncation — matches the sprinkler cycle, so 15 RPM injects exactly 25mB/80tick with zero `(int)` loss)
- Fill is dimension-scoped: each pump refills only when its own level ticks (no multi-dimension duplication)

**Sprinklers** — Real water consumption:
- Consume 5mB per spray cycle (~80 ticks); 5 sprinklers drain 25mB/80tick, exactly matched by a 15 RPM pump's 25mB/80tick injection — the design balance point
- `searchForFluid` honors its `drain` flag: probe calls (`drain=false`, e.g. other mods' periodic checks) never consume pump water — only water-level and pressure are checked
- **Pump Pressure** formula: `PumpY + Tanks + RPM − SprinklerY ≥ 0` — gravity + tank head + rotation determines reach
- Tank count is event-driven (cached, invalidated on tank place/break); 100-tick fallback rescan

**Irrigation Tank** — Added to LOW thermal insulation tag. Tank in wall acts as pipe passthrough; BFS traverses tanks as pipe nodes.

**Jade** — Sprinklers show greenhouse/cellar status via custom Jade provider. Pump fluid level shown via NeoForge's native fluid tooltip.

### Reinforced Soil

8 soil variants × visual states based on `axis_x`/`axis_z` connection properties. Created with **support beam** (main hand) + **hammer** (offhand), right-clicking reinforceable ground. Sneak+right-click extends downward up to 3.

**Visual system** — 6 texture types per variant:
- End texture (centered/top-edge beam cross-section)
- Vertical crack / cross crack / horizontal crack on sides
- Becomes horizontal beam when both ends connected to support beams
- Auto-connects/disconnects on neighbor changes

### Commands

```
/flhc cellar info     — Cellar/greenhouse debug info
/flhc cellar recalc   — Force rescan (permission 4)
/flhc cellar list     — List all tracked spaces
/flhc cellar clear    — Clear all tracked spaces (permission 4)
```

### Patchouli

Registers a separate **FirmaLife HardCore** category in TFC's field guide (sortnum=11, after Firmalife), with thermometer icon. Covers the cellar/greenhouse overhaul mechanics.

### Configuration

`config/firmalifehardcore-server.toml`: `maxHorizontalSpan` (default 24), `maxVerticalSpan` (8), resistance values, preservation tier thresholds, greenhouse canopy multiplier/ratio, double door multiplier, tick limits.

### Build

```bash
./gradlew build
# JAR → build/libs/firmalifehardcore-neoforge-0.3.1-beta.jar
```

### License

MIT

---

## 中文

一个 NeoForge 1.21.1 Mod，彻底重写 Firmalife 的地窖和温室机制——**不再需要气候站**。封闭空间通过 BFS floodfill + AABB 动态追踪自动检测，任意保温方块围成的空间即可生效。

### 地窖与温室

放置/破坏墙体或容器方块时自动扫描。每个墙体方块根据 block tag 提供热阻值：

| 等级 | 热阻 | 典型方块 |
|------|------|----------|
| **HIGH** | 0.75 | 密封砖 |
| **MEDIUM** | 0.55 | 岩石或矿石、泥土类（耕地和草径不算）、各种砖、木头类|
| **LOW** | 0.25 | 玻璃（块/板）、金属块煤块等、砂岩、灌溉水箱 |

门算中等热阻，**双门 4 倍**加成。

**地窖**：地窖基准温度 = 4 °C，室内温度 = 室外温度 - (室外温度-基准) × 平均热阻。温度越低保鲜越好（≤16°C SHELVED / ≤8°C SHELVED_2 / ≤0°C SHELVED_3）。

**温室**：屋顶玻璃屋顶 ≥ 50% 可见天空 → 温室。温室基准 = 4 + 40 × 棚顶比例（°C）。

**温度 API**：`getInstantTemperature()` 和 `getAverageTemperature()` 均被拦截。即时返回有效温度（随天气波动），年均返回室内基准温度（稳定微气候，供灌木/果树判气候适宜度）。

### 容器保鲜

| 容器 | 机制 |
|------|------|
| 食品架 | 三级 SHELVED trait（ClimateReceiver） |
| 风干架 | 三级 HUNG trait（ClimateReceiver） |
| 大缸 | 密封时附加 SHELVED，解封时清除 |

通过 `#firmalifehardcore:cellar_containers` tag 自动识别。

### 温度计

红石信号（服务端）+ Jade tooltip（"室内温度"）显示温室/地窖有效温度。户外自动回退 TFC 原生显示。

### 灌溉系统改造（Phase 2）

全面升级 Firmalife 灌溉系统（水泵站、灌溉水箱、洒水器），引入真实流体机制。

**水泵站** — 现在是 NeoForge IFluidHandler 流体容器：
- 基础 500mB 储水，正上方每堆叠一个灌溉水箱 +500mB（最多 3 个）
- 从下方水源取水，储水速率与机械转速成正比
- 通过 NeoForge 能力系统注册，兼容外部流体管道
- 每 tick 填充按 Z 坐标错峰（每 80 tick 一次，×80 避免截断——与洒水器同周期，15 RPM 时精确注入 25mB/80tick 零损失）
- 注水按维度隔离：水泵只在自己的维度 tick 时注水（避免多维度重复注水）

**洒水器** — 真实水消耗：
- 每次喷水消耗 5mB（~80 tick），5 个洒水器每 80 tick 消耗 25mB，与 15 RPM 水泵注入量（25mB/80tick）精确平衡——设计平衡点
- `searchForFluid` 尊重 drain 参数：探测调用（drain=false，如其他 mod 的周期检查）只检查水位与压力，绝不消耗水泵水
- **泵压**公式：`水泵Y + 水箱数 + RPM − 洒水器Y ≥ 0`——重力+水箱+转速决定水能送到的高度
- 水箱计数事件驱动（缓存，水箱放置/破坏时刷新；100 tick 保底重扫）

**灌溉水箱** — 加入 LOW 热阻标签。墙内水箱充当管道穿墙接口；BFS 将水箱视为管道节点穿越。

**Jade** — 洒水器显示温室/地窖状态（自定义 Jade 提供者）。水泵水位由 NeoForge 原生流体 tooltip 显示。

### 带支撑土

8 种土壤变体，`axis_x`/`axis_z` 属性控制视觉。**主手支撑梁 + 副手锤**右键地面，潜行向下最多 3 格。

6 种纹理：端头（居中/贴边）、纵裂纹、纵横裂纹、横裂纹。两端连接支撑梁自动变横梁，断开自动恢复。

### 指令

```
/flhc cellar info     — 地窖/温室诊断信息
/flhc cellar recalc   — 强制重算（4 级权限）
/flhc cellar list     — 列出所有已追踪空间
/flhc cellar clear    — 清空所有已追踪空间（4 级权限）
```

### 帕秋莉

在 TFC 手册中注册独立 **FirmaLife HardCore** 分类（sortnum=11，排在 Firmalife 之后），温度计图标，介绍新版地窖/温室机制。

### 配置

`config/firmalifehardcore-server.toml`：`maxHorizontalSpan`（默认 24）、`maxVerticalSpan`（8）、热阻值、保鲜温度阈值、温室棚顶参数、双门倍率、tick 处理上限。

### 构建

```bash
./gradlew build
# JAR → build/libs/firmalifehardcore-neoforge-0.3.1-beta.jar
```

### 许可证

MIT
