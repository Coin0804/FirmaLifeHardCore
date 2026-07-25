# FirmaLife HardCore

[English](#english) | [中文](#中文)

---

## English

A NeoForge 1.21.1 mod that completely overhauls Firmalife's cellar and greenhouse mechanics — **no Climate Station required**. Enclosed spaces are auto-detected via BFS floodfill with dynamic AABB tracking. Any insulating blocks form a functional cellar or greenhouse.

### Cellar & Greenhouse System

Enclosed spaces are detected automatically when wall/container blocks are placed or broken. Every solid wall block contributes a **thermal resistance value** based on block tags:

| Tier | Resistance | Typical Blocks |
|------|-----------|----------------|
| **HIGH** | 0.75 | Mud bricks, sealed bricks (`#firmalife:cellar_insulation`), reinforced soil |
| **MEDIUM** | 0.55 | Stone, cobblestone, rock, dirt, grass, stone bricks, planks, logs, wattle |
| **LOW** | 0.25 | Glass, glass panes, metal blocks, sandstone, support beams |

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

### Reinforced Soil

8 soil variants × visual states based on `axis_x`/`axis_z` connection properties. Created with **support beam** (main hand) + **hammer** (offhand), right-clicking reinforceable ground. Sneak+right-click extends downward up to 3.

**Visual system** — 6 texture types per variant:
- End texture (centered/top-edge beam cross-section)
- Vertical crack / cross crack / horizontal crack on sides
- Becomes horizontal beam when both ends connected to support beams
- Auto-connects/disconnects on neighbor changes

### Commands

```
/firmalifehardcore cellar info     — Cellar/greenhouse debug info
/firmalifehardcore cellar recalc   — Force rescan (permission 4)
/firmalifehardcore cellar list     — List all tracked spaces
```

### Patchouli

Registers a separate **FirmaLife HardCore** category in TFC's field guide (sortnum=11, after Firmalife), with thermometer icon. Covers the cellar/greenhouse overhaul mechanics.

### Configuration

`config/firmalifehardcore-server.toml`: `maxHorizontalSpan` (default 24), `maxVerticalSpan` (8), resistance values, preservation tier thresholds, greenhouse canopy multiplier/ratio, double door multiplier, tick limits.

### Build

```bash
./gradlew build
# JAR → build/libs/firmalifehardcore-neoforge-0.0.1.jar
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
| **HIGH** | 0.75 | 泥砖、密封砖 (`#firmalife:cellar_insulation`)、带支撑土 |
| **MEDIUM** | 0.55 | 石头、圆石、岩块、泥土、草地、石砖、木板、原木、wattle |
| **LOW** | 0.25 | 玻璃、玻璃板、金属块、砂岩、支撑梁 |

门算中等热阻，**双门 4 倍**加成。

**地窖**：T_室内 = 基准 + (T_室外 − 基准) × (1 − 平均热阻)，基准 = 4°C。温度越低保鲜越好（≤16°C SHELVED / ≤8°C SHELVED_2 / ≤0°C SHELVED_3）。

**温室**：屋顶方块下方是室内空间（含障碍物）→ 计入屋顶。玻璃屋顶 ≥ 50% 可见天空 → 温室。基准 = 4 + 40 × 棚顶比例。

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

### 带支撑土

8 种土壤变体，`axis_x`/`axis_z` 属性控制视觉。**主手支撑梁 + 副手锤**右键地面，潜行向下最多 3 格。

6 种纹理：端头（居中/贴边）、纵裂纹、纵横裂纹、横裂纹。两端连接支撑梁自动变横梁，断开自动恢复。

### 指令

```
/firmalifehardcore cellar info     — 地窖/温室诊断信息
/firmalifehardcore cellar recalc   — 强制重算（4 级权限）
/firmalifehardcore cellar list     — 列出所有已追踪空间
```

### 帕秋莉

在 TFC 手册中注册独立 **FirmaLife HardCore** 分类（sortnum=11，排在 Firmalife 之后），温度计图标，介绍新版地窖/温室机制。

### 配置

`config/firmalifehardcore-server.toml`：`maxHorizontalSpan`（默认 24）、`maxVerticalSpan`（8）、热阻值、保鲜温度阈值、温室棚顶参数、双门倍率、tick 处理上限。

### 构建

```bash
./gradlew build
# JAR → build/libs/firmalifehardcore-neoforge-0.0.1.jar
```

### 许可证

MIT
