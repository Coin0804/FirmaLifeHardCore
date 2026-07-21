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
| **MEDIUM** | 0.45 | Planks, logs, lumber, support beams, wattle |
| **LOW** | 0.15 | Glass, metal blocks |

Doors and trapdoors reduce insulation when open. Double doors grant a 1.2× bonus.

The effective cellar temperature is calculated as:

```
T_cellar = T_outside × (1 − clamp(avgResistance, 0, 0.95))
```

### Container Modifiers

Different storage types have different preservation multipliers (smaller = better):

| Container | Modifier |
|-----------|----------|
| Food Shelf | 0.50 |
| Hanger | 0.35 |
| Jarbnet (clay pot) | 0.20 |
| Keg | 0.40 |
| Cheese Wheel | 0.30 |

### Reinforced Soil

A new block — `firmalifehardcore:reinforced_dirt` — stabilizes TFC's collapsible dirt. Created by using a **hammer** (main hand) on dirt while holding **support beams** (offhand). The reinforced soil:
- Prevents collapse/landslides (acts as TFC support beam)
- Has HIGH thermal resistance (0.80)
- Drops regular dirt when broken (beams are not recovered)

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
| **MEDIUM** | 0.45 | 木板、原木、木材、支撑梁、编织墙 |
| **LOW** | 0.15 | 玻璃、金属方块 |

开关门影响保温——开门时该方向热阻归零。双门提供 1.2× 加成。

地窖有效温度：`T_地窖 = T_室外 × (1 − 热阻)`

### 容器修正

不同容器保鲜倍率不同（越小越好）：

| 容器 | 修正 |
|------|------|
| 食物架 | 0.50 |
| 悬挂架 | 0.35 |
| 罐架（陶罐） | 0.20 |
| 酒桶 | 0.40 |
| 奶酪轮 | 0.30 |

### 带支撑的土

新增方块 `firmalifehardcore:reinforced_dirt`，解决 TFC 泥土塌方问题。主手拿**锤**、副手拿**支撑梁**，右键泥土即可生成。特性：
- 防止塌方/滑坡（作为 TFC 支撑柱）
- HIGH 热阻 (0.80)
- 破坏时掉落原版泥土（支撑梁不可回收）

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
