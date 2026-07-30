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
