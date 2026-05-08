# Configuration reference

All paths are under `plugins/LagProtector/config.yml` unless noted. Keys are matched **case-insensitively** where the plugin uses section lookup helpers.

The bundled `config.yml` in the JAR also contains **inline comments** (lag tuning, `EntityTypes` notes for farms). When in doubt, compare with the latest `src/main/resources/config.yml` in the project.

---

## `debug`

- **Type**: boolean  
- **Default**: `false` (when the key is missing)  
Throttles **plugin** debug lines (e.g. listener cancels) when `false`. Keep `false` on production unless you are actively troubleshooting.

---

## `Log`

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `Enabled` | boolean | `false` | When `true`, the server may log cull activity (see `Log.From` and `messages.yml` → `log.cull-removed`). |
| `From` | int | `3` | Minimum removals in one mob cull **chunk task** before that task logs (when logging is enabled). |

---

## `Ignore`

Skip culling for entities that match these flags (armor stands with gear, named animals, etc.).

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `FullArmorStands` | boolean | `true` | Armor stands with equipment |
| `EntityWithPickedItem` | boolean | `true` | Entities holding picked-up items |
| `FullItemFrames` | boolean | `true` | Item frames with an item |
| `TamedAnimals` | boolean | `true` | Tamed creatures |
| `NamedAnimals` | boolean | `true` | Custom-named creatures |
| `WithSadle` | boolean | `true` | Saddled rideables. `WithSaddle` is also accepted. |
| `BabyAnimals` | boolean | `true` | Baby ageable mobs |
| `Worlds` | list of strings | `[]` | **Deny list** (world names, compared case-insensitively). Together with `Mobs.WorldsAllow`: if allow-list is empty, every world is used except denied worlds; if allow-list is non-empty, only listed worlds that are not denied apply. |

---

## `EntitySpawn`

Spawn-time checks (`CreatureSpawnEvent` / `EntitySpawnEvent`).

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `Check` | boolean | `true` | If `false`, this plugin does **not** apply spawn limits (large perf win if you do not need them). |
| `GroupChunks` | boolean | `true` | Count across a chunk group instead of a single chunk (more accurate, more work per spawn). |
| `GroupChunksRadius` | int (1–3) | `2` | Display radius: `1` = 1 chunk, `2` = 3×3, `3` = 5×5. Stored internally as Chebyshev radius minus one. |

---

## `RemoveExisting` (legacy root key)

- **Type**: boolean  
- **Default if key absent**: `true`  

Used **only when `AutoClean.Enabled` is not set** under `AutoClean`. Same gate as before: periodic mob/block AutoClean schedules require this to be `true` (together with `AutoClean.Mobs.Use` / `AutoClean.Blocks.Use`), unless you use `AutoClean.Enabled` instead.

Prefer **`AutoClean.Enabled`** in new configs — clearer than this name.

---

## `ExcludedChunks`

- **Type**: list of strings  
- **Default**: `[]`  

Chunks where **mob** spawn limiting and **mob** cull skip (normalized: lowercase, spaces removed). Format is typically `worldname:chunkX:chunkZ`.

---

## `Mobs`

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `WorldsAllow` | list | `[]` | If **non-empty**, only these worlds get mob rules. If **empty**, all worlds except `Ignore.Worlds` denied entries. |
| `DefaultPerType` | int | `28` | For **mob-cap** types without an `EntityTypes` row. `-1` disables this fallback. |
| `FallbackNonLivingLimit` | int | `-1` | For **non–mob-cap** types without a row. `-1` disables. |
| `TotalMaxPerChunk` | int | `-1` | Optional cap on living non-player mobs in the counted area (mechanics-tracked entity types excluded from this total). `-1` disables. |

---

## `SpawnReasons` (optional)

Map of Bukkit `CreatureSpawnEvent.SpawnReason` enum names → boolean.  
**Default**: every reason `true` if the section is absent.

If a reason is `false`, spawns with that reason are cancelled in **`CreatureSpawnEvent`** (living mobs). Other paths may still hit **`EntitySpawnEvent`** limits.

---

## `EntityTypes`

Map **`EntityType`** enum names (e.g. `ZOMBIE`, `ITEM_FRAME`) to:

| Key | Type | Meaning |
| --- | --- | --- |
| `Use` | boolean | If `false`, this type ignores these rules |
| `Limit` | int | Max count in the counted region; **`-1` = unlimited** |
| `AutoClean` | boolean | Whether periodic mob cull may remove excess |
| `NaturalSpawn` | boolean | If `false`, “natural-style” spawn reasons may be blocked (`CreatureSpawnEvent`) |

`PLAYER` is injected as unlimited if omitted.

Non–mob-cap types with `Use: true` and a non-negative effective limit are also treated as **mechanics-style** entities (same bucket as hopper minecarts / frames for totals), alongside `BlockTypes`.

### Tuning that affects gameplay

- **`FALLING_BLOCK`**: gravity/sand-style machines can create **many** short-lived falling blocks. The sample config keeps a **high** limit on purpose. To exempt entirely, use **`Limit: -1`** or **`Use: false`**.
- **Projectiles** (`ARROW`, `TRIDENT`, splash/lingering potions, etc.): skeleton farms, raids, and PvP can spike counts — **raise** limits before tuning lag elsewhere.
- **`END_CRYSTAL`**: end fights, respawn anchors, and raids may place **several** crystals in one counted area. The sample uses a **forgiving** default; **lower** only if you are fighting intentional crystal spam lag.

---

## `BlockTypes`

| Key | Type | Meaning |
| --- | --- | --- |
| `Limit` | boolean | Master switch for per-material caps on **place** |
| `CheckPistonMove` | boolean | If `true`, piston moves that would exceed per-chunk caps across chunk borders are cancelled |
| `List` | map `Material` → int | Max tile blocks of that material **per chunk** (material names must match Bukkit `Material`; unknown keys are skipped) |

Only materials that actually have **block entities / tile logic** in your version matter for counts (e.g. **repeater** has no BE on Java — listing it does nothing). The bundled list mirrors common storage/redstone tiles (see `MechanicsMaterials` in code for plugin defaults when `List` is empty).

---

## `AutoClean`

Runs on the **global region scheduler** at a fixed period; work is executed on **region** threads per chunk (Folia-safe).

### `AutoClean.Enabled` (recommended master switch)

| Key | Type | Meaning |
| --- | --- | --- |
| `Enabled` | boolean | When **present** under `AutoClean`, gates **both** periodic mob and block cull schedules. Final mob cull runs only if `Enabled` ∧ `Mobs.Use` ∧ interval > 0; blocks similarly. |

If **`Enabled` is omitted** entirely from `AutoClean`, the plugin falls back to legacy root **`RemoveExisting`** (default `true` when that key is absent). Case-insensitive key name (`enabled` works).

### Interval semantics

Under each of `AutoClean.Mobs` and `AutoClean.Blocks`:

| Key | Meaning |
| --- | --- |
| `Interval` | **Seconds** between passes (via `IntervalSeconds` lookup); multiplied by 20 → ticks. |
| `IntervalSeconds` | Optional explicit seconds (same layer as `Interval`). |
| `IntervalTicks` | Used only when the **seconds** path resolves to `0` (e.g. set `Interval: 0` and `IntervalTicks: 400` for 20 seconds). |

### `AutoClean.Mobs`

| Key | Meaning |
| --- | --- |
| `Use` | Turns mob cull **on** for that section; still needs the master gate (`AutoClean.Enabled`, or legacy `RemoveExisting` if `Enabled` is absent), and interval > 0 |
| `MaxRemoved` | Max entity removals attempted **per chunk region task** in one inner loop |
| `SliceChunksPerTick` | Max **chunk tasks scheduled per server tick** while draining one wave. **`≤ 0`** = legacy burst (schedules all chunk tasks at once; worst MSPT spikes). |
| `DeduplicateGroupCull` | When `EntitySpawn.GroupChunks` is true, only **anchor** chunks on a grid (stride `2 × radius + 1`) run mob cull, avoiding redundant overlapping passes |

### `AutoClean.Blocks`

| Key | Meaning |
| --- | --- |
| `Use` | Turns block cull **on**; same master gate as mobs (`AutoClean.Enabled` or legacy `RemoveExisting`). |

Same interval keys as mobs. Additional:

| Key | Meaning |
| --- | --- |
| `BreakNaturally` | If `true`, excess tracked tiles break with natural drops where applicable |
| `SliceChunksPerTick` | Same idea as mobs: spread block-cull chunk tasks across ticks (`≤ 0` = burst) |

---

## `messages.yml`

Optional copy in `plugins/LagProtector/messages.yml` (created from the JAR default on first run). Holds player-facing strings and the **`log.cull-removed`** template (`{count}` placeholder). Reloaded together with **`/lagprotector reload`** (same as `config.yml`).

---

## Reloading

`/lagprotector reload` (permission **`lagprotector.reload`**, default **op**) reloads `config.yml`, `messages.yml`, and restarts AutoClean scheduling.
