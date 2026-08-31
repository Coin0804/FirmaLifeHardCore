## 0.3.4-beta

### Fixes
- **Cheese wheels can now actually age — receivers are split by climate type**: `CheeseWheelBlock.setValid` computes `shouldAge = valid && climate == ClimateType.CELLAR` in one pass, and `SprinklerBlockEntity.setValid` ignores `climate` entirely and just syncs `valid`. Both assume the receiver only ever receives *one* climate type — true for vanilla Firmalife, broken for us, because we broadcast both cellar and greenhouse to every receiver and the last call won. The unified `climate_receivers` tag is replaced by two tags: `cellar_receivers` (food shelves, hangers, large vessels, cheese wheels) and `greenhouse_receivers` (planters, sprinklers). `CellarDetector` sorts every receiver position into the matching set at scan time (greenhouse wins if both), and `broadcastToContainers` sends each climate only to its own receivers — cheese wheels only ever see `CELLAR`, sprinklers/planters only ever see `GREENHOUSE`. Old saves load the legacy `receivers` list as cellar receivers; the next re-scan reclassifies by tag.

---

## 0.3.3-beta

### Fixes
- **Cheese wheels can now actually age**: `LargePlanterBlockEntity.setValid` computes `shouldAge = valid && climate == ClimateType.CELLAR` in one pass, so the *last* broadcast wins. We broadcast both greenhouse and cellar, and cellar came first — the later greenhouse broadcast then recomputed `shouldAge` as `false` and stomped the valid cellar state, so wheels never started aging. Vanilla Firmalife only ever sends one climate type per receiver, so this never surfaced there (its planter `setValid` even branches on `if (climate == ClimateType.GREENHOUSE)` first — the bug was our broadcasting order, not the receiver). `broadcastToContainers` now sends greenhouse first, cellar last, so the final broadcast the wheel receives is the cellar one.

---

## 0.3.2-beta

### Fixes
- **Reinforced soil beams are now shovel-mineable**: the 8 beam variants were missing from `mineable/shovel`. Combined with `requiresCorrectToolForDrops()`, no tool ever counted as correct, so beams dropped nothing when mined. Now they drop the regular reinforced soil, as intended.
- **Reinforced soils now count as TFC insulation**: added all 16 variants to `tfc:insulation` (append, `replace: false`). Charcoal forge and bloomery multiblocks accept them as valid insulation walls/bottom — consistent with their medium (0.55) thermal resistance in the cellar/greenhouse system, and with real rammed-earth insulation.

---

## 0.3.1-beta

### Fixes
- **Pump refill is now dimension-scoped**: `tickAll(Level)` only fills pumps in the ticking level. `LevelTickEvent.Post` fires for every loaded `ServerLevel`, so the same pump was refilled once per dimension every tick (4× intended rate with multiple loaded dimensions).
- **Pump refill cycle 20 → 80 ticks** (matching sprinkler watering cadence): at the 15 rpm design balance point the injection amount is now `25.0 → 25 mB/80tick` with zero `(int)` loss, exactly matching 5 sprinklers' 25 mB/80tick consumption (previously 6.28 → 6, 4% short, so the balance point was unreachable).
- **Sprinkler `searchForFluid` now honors the `drain` flag in pipe mode**: probe calls (`drain=false`, e.g. tfcfertigation's per-tick fertilizing cycle check) no longer drain water from the pump tank — only water-level and pressure checks run. Actual watering (`drain=true`) is unchanged. Previously every probe call executed a real `drain(sprinklerWaterUse, EXECUTE)`, draining the pump tank every tick per sprinkler.
- **Reinforced dirt conversion no longer blocks beam placement**: `ReinforcedDirtHandler` only takes over `UseItemOnBlockEvent` when conversion is guaranteed to succeed — clicking a non-reinforceable block or holding too few beams now falls back to normal support beam placement.
- Conversion decision moved before event cancellation so client and server sides agree on whether to take over; `setBlock` failure now breaks the conversion loop instead of silently continuing.

---

## 0.3.0-beta

### Additions
- **4-set BFS model**: OBSTACLE split into pure obstacle + RECEIVER — containers, planters, sprinklers, and cheese wheels are now properly distinguished from inert blocks.
- Unified `#firmalifehardcore:climate_receivers` tag covering all ClimateReceiver blocks.
- BFS cell expansion logic extracted to shared `updateBounds()` helper.
- `/flhc cellar clear` debug command — clears all tracked spaces (permission 4).
- `CellarDetector.DetectResult` — distinguishes "BFS genuinely failed" from "interrupted by unloaded chunk".
- `receiverPositions` persisted in save data with full backward compatibility.

### Changes
- Command alias shortened: `/firmalifehardcore` → `/flhc`.
- `broadcastToContainers` and `invalidateSpace` now only iterate `receiverPositions` instead of all interior/obstacle positions.
- `CellarEventHandler.isRelevantBlock` → `affectsCellar` — clearer naming.
- Health check now destroys spaces with zero receivers (fragment cleanup).
- BFS aborts gracefully when encountering unloaded chunks, preserving existing space data.
- Pending discovery candidates expanded: any non-WALL block is a valid BFS seed.
- Load errors skip individual spaces instead of crashing.

### Fixes
- **Fragment cleanup**: 1-block cavities and receiver-less spaces are now auto-destroyed by the health check.
- **Seed candidate blind spot**: BFS can now start from obstacle/receiver blocks, not just air.
- **Orphaned containers**: When a cellar is split by a new wall, containers on the isolated side are immediately invalidated.
- **Chunk boundary resilience**: Spaces spanning chunk borders no longer get destroyed by incomplete BFS data.
- **PumpNBT log spam removed.**

---

## 0.2.1-beta

### Fixes
- Sprinkler BFS refactored: terminal check (pump/tank) now runs before pipe traversal, so tanks stacked on pumps are correctly found.
- Tank count resolved from pump's own fluid capacity instead of duplicate scanning — single source of truth.
- BFS inner class moved out of mixin package to fix `IllegalClassLoadError`.

### Changes
- `searchForFluid` fully rewritten via `@Overwrite` — cleaner structure, no scattered `@Redirect` patches on `enqueueConnections`.
- Pump connection check extracted as `@Unique` helper — no longer redirects `PumpingStationBlock.hasConnection` from the sprinkler mixin.

---

## 0.2.0-beta

### Additions
- Pumping Station is now a NeoForge fluid container (IFluidHandler) with base 500mB storage.
- Each Irrigation Tank stacked directly above the pump adds 500mB capacity (max 3).
- Per-tick water fill driven by mechanical rotation speed, staggered by Z-coordinate.
- Pump Pressure system: `pumpY + tanks + RPM − sprinklerY ≥ 0` determines if water reaches a sprinkler.
- Sprinklers now consume 5mB water per spray cycle (~80 ticks). 15RPM ≈ 5 sprinklers equilibrium.
- Pump fluid capability registered via `RegisterCapabilitiesEvent` for external pipe compatibility.
- Configurable pipe BFS search distance `pipeMaxCost` (default 64, was hardcoded 32).
- Pump head Jade tooltip displays effective pumping height (tanks + RPM).
- Configurable irrigation parameters: `pumpBaseCapacity`, `tankCapacityBonus`, `maxTankBonus`, `sprinklerWaterUse`, `pumpRateFactor`.
- Patchouli manual: "Irrigation Overhaul" and "Sprinklers & Pump Pressure" pages.

### Changes
- Irrigation Tank added to LOW thermal insulation tag — acts as wall, pipes through tank don't break greenhouse seal.
- Tank capacity scanning is event-driven (invalidated on tank place/break, 100-tick fallback).
- Sprinkler BFS treats Irrigation Tanks as pipe nodes for pass-through routing.
- Pipe BFS max distance increased from 32 to 64 (configurable).

### Fixes
- Greenhouse split detection: breaking a wall now correctly detects orphaned spaces.
- Sprinkler Jade tooltip: disabled unreliable Firmalife HoeOverlay, replaced with CellarTracker-based SprinklerProvider.
- Pump water persistence fix: `saveAdditional`/`loadAdditional` properly remapped for vanilla methods.
- `lastFillTick` initialization prevents instant full fill on first world load.
- Diagnostic log spam removed from cellar detection, pump BFS, and pressure calculation.
- Tank capacity no longer rescanned every access (event-driven with cache).

### Known Issues
- Sprinklers cannot draw water from isolated tanks without pump mechanical power (centralized pump storage limitation).
