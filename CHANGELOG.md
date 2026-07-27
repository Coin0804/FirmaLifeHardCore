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
