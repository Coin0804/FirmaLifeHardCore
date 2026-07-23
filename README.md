# FirmaLife HardCore

[English](#english) | [中文](#中文)

---

## English

A NeoForge 1.21.1 patch mod that overhauls Firmalife's cellar (and eventually greenhouse) mechanics with a **Vintage Story-inspired physics simulation**.

Instead of the vanilla Firmalife approach — where only 6 sealed brick block types count as cellar insulation and a ClimateStation block is required — this mod introduces a **global, event-driven CellarTracker** attached to every ServerLevel. Any enclosed space built from high-insulation materials (stone, dirt, packed earth, bricks) functions as a cellar. No special blocks needed.

### Cellar Thermal Resistance System

Enclosed spaces are detected via BFS floodfill. Every solid wall block contributes a **thermal resistance value** based on block tags:

| Tier | Resistance | Typical Blocks |
|------|-----------|----------------|
| **HIGH** | 0.80 | Stone, dirt, bricks, packed mud, sealed bricks, reinforced soil |
| **MEDIUM** | 0.55 | Planks, logs, lumber, support beams, wattle |
| **LOW** | 0.15 | Glass, metal blocks |

Doors and trapdoors reduce insulation when open. Double doors grant a 1.2× bonus.

The effective cellar temperature is calculated as:

```
T_cellar = 4 + (T_outside − 4) × (1 − clamp(avgR, 0, 1))
```

### Container Preservation

Supported container types:

| Container | Preservation Mechanism |
|-----------|----------------------|
| Food Shelf | ClimateReceiver + 3-tier SHELVED trait |
| Hanger | ClimateReceiver + 3-tier HUNG trait |
| Large Vessel | onSeal appends SHELVED / onUnseal removes |

### Reinforced Soil

8 soil variants × 2 states (normal = vertical anchor, beam = horizontal support). Created by holding a **support beam** (main hand) + **hammer** (offhand), right-clicking reinforceable ground (dirt, grass, farmland, grass path). Sneak+right-click extends downward up to 3.

- **Auto-connection**: on placement/conversion, scans E/W and N/S axes (up to 5) for support beam endpoints. Both ends found → becomes `_beam` variant (provides TFC support). Endpoints lost → reverts. Adjacent blocks re-check on state change.
- **Textures**: normal = beam mark on top/bottom, TFC dirt on sides. Beam = TFC dirt on top/bottom, beam mark on sides.
- **TFC Integration**: `_beam` variants in `tfc:support` (2/2/4) + `#tfc:support_beams` tag. Normal variants in `#tfc:support_beams` tag only.
- **Jade**: Optional tooltip shows support status on landslide-prone blocks.

### Commands

```
/firmalifehardcore cellar info     — Show cellar parameters at your position
/firmalifehardcore cellar recalc   — Force recalculation (admin only)
/firmalifehardcore cellar list     — List all tracked cellar spaces
```

### Architecture

```
CellarTracker (per ServerLevel, via NeoForge Attachment)
├── CellarDetector — BFS floodfill + thermal calculation
├── CellarEventHandler — Block place/break/door toggle → mark dirty
├── ThermalConductivity — Tag-based 3-tier resistance lookup
├── ContainerModifiers — Container type → preservation modifier
└── Tick-limited processing — max 3 spaces + 5 containers per tick
```

### Dependencies

- **Minecraft** 1.21.1
- **NeoForge** 21.1+
- **TerraFirmaCraft** 4.x
- **Firmalife** 2.x

### Configuration

All values are configurable via `config/firmalifehardcore-server.toml`:
- `scanRadius`, `minThermalResistance`, resistance values per tier
- Level 2/3 preservation thresholds
- Container modifier values
- Tick processing limits

### Build

```bash
./gradlew build
```

JAR output: `build/libs/firmalifehardcore-neoforge-0.0.1.jar`

### License

MIT — see [LICENSE](LICENSE)

---

## 中文

一个 NeoForge 1.21.1 补丁 Mod，用**复古物语式物理模拟**彻底改造 Firmalife 的地窖（以及后续的温室）机制。

原版 Firmalife 仅 6 种密封砖可用作地窖墙体，且必须放置 ClimateStation 气候站。本 Mod 引入了附着于每个 ServerLevel 的**全局事件驱动 CellarTracker**。任何由高保温材料（石头、泥土、砖块、夯实土）封闭的空间自动成为地窖——无需特殊方块。

### 地窖热阻系统

通过 BFS floodfill 检测封闭空间。每个墙体方块根据 block tag 提供热阻值：

| 等级 | 热阻 | 典型方块 |
|------|------|----------|
| **HIGH** | 0.80 | 石头、泥土、砖块、夯实泥、密封砖、带支撑土 |
| **MEDIUM** | 0.55 | 木板、原木、木材、支撑梁、编织墙 |
| **LOW** | 0.15 | 玻璃、金属方块 |

开关门影响保温——开门时该方向热阻归零。双门提供 1.2× 加成。

地窖有效温度：`T_地窖 = 4 + (T_室外 − 4) × (1 − 平均热阻)`

### 容器保鲜

已支持的容器类型：

| 容器 | 保鲜机制 |
|------|---------|
| 食物架 (FoodShelf) | ClimateReceiver + 三级 SHELVED trait |
| 悬挂架 (Hanger) | ClimateReceiver + 三级 HUNG trait |
| 大缸 (LargeVessel) | onSeal 追加 SHELVED / onUnseal 清除 |

### 带支撑的土

8 种土壤变体 × 2 状态（竖梁锚点 / 横梁支撑）。**主手支撑梁** + **副手锤**，右键地面方块（泥土/草地/耕地/草径）。潜行右键向下延伸最多 3 格。

- **自动连接**：放置/转换时扫描 E/W、N/S 轴（最多 5 格），两端均有端点→变 `_beam` 横梁（提供 TFC 支撑），端点消失→退回。状态变化时邻居重检。
- **纹理**：竖梁 = 顶底梁标记 + 侧面 TFC 土。横梁 = 顶底 TFC 土 + 侧面梁标记。
- **TFC 集成**：`_beam` 在 `tfc:support`(2/2/4) + `#tfc:support_beams`。普通变体仅 `#tfc:support_beams`。
- **Jade**：可选 tooltip，滑坡方块显示支撑状态。

### 指令

```
/firmalifehardcore cellar info     — 查看当前位置地窖参数
/firmalifehardcore cellar recalc   — 强制重算（管理员）
/firmalifehardcore cellar list     — 列出所有已追踪空间
```

### 依赖

- Minecraft 1.21.1 / NeoForge 21.1+ / TFC 4.x / Firmalife 2.x

### 构建

```bash
./gradlew build
```

### 许可证

MIT 协议 — 详见 [LICENSE](LICENSE)
