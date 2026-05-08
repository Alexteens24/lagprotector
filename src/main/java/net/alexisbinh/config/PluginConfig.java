/*
 * Copyright 2026 alexisbinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.alexisbinh.config;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.alexisbinh.util.EntityCategories;
import net.alexisbinh.util.WorldPolicy;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginConfig {

    private boolean debug;
    private MobSection mobs;
    private MechanicsSection mechanics;
    private IgnoreSettings ignore;
    private LogSettings log;

    public void reload(@NotNull JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        this.debug = c.getBoolean("debug", false);
        this.ignore = IgnoreSettings.load(c);
        this.log = LogSettings.load(c);
        this.mobs = MobSection.load(c, ignore);
        this.mechanics = MechanicsSection.load(c, ignore);
    }

    public boolean debug() {
        return debug;
    }

    public @NotNull MobSection mobs() {
        return mobs;
    }

    public @NotNull MechanicsSection mechanics() {
        return mechanics;
    }

    public @NotNull IgnoreSettings ignore() {
        return ignore;
    }

    public @NotNull LogSettings log() {
        return log;
    }

    public record LogSettings(boolean enabled, int minRemoved) {
        static @NotNull LogSettings load(@NotNull FileConfiguration c) {
            ConfigurationSection s = ConfigUtil.sectionCI(c, "Log");
            if (s == null) {
                return new LogSettings(false, 3);
            }
            return new LogSettings(s.getBoolean("Enabled", false), s.getInt("From", 3));
        }
    }

    public record CullSettings(
            boolean enabled,
            long periodTicks,
            int maxPerPass,
            boolean breakNaturally
    ) {

        /**
         * Periodic AutoClean (mob + block waves). Prefers {@code AutoClean.Enabled} when set;
         * otherwise legacy root {@code RemoveExisting} (default {@code true}).
         */
        static boolean periodicCullAllowed(@NotNull FileConfiguration c) {
            ConfigurationSection ac = ConfigUtil.sectionCI(c, "AutoClean");
            if (ac != null) {
                for (String key : ac.getKeys(false)) {
                    if (key.equalsIgnoreCase("Enabled")) {
                        return ac.getBoolean(key, true);
                    }
                }
            }
            return c.getBoolean("RemoveExisting", true);
        }

        static @NotNull CullSettings mobCullFromRoot(@NotNull FileConfiguration c) {
            boolean scheduleAllowed = periodicCullAllowed(c);
            ConfigurationSection ac = ConfigUtil.sectionCI(c, "AutoClean");
            boolean use = scheduleAllowed;
            long ticks = 120L;
            int max = 48;
            if (ac != null) {
                ConfigurationSection m = ConfigUtil.sectionCI(ac, "Mobs");
                if (m != null) {
                    use = scheduleAllowed && m.getBoolean("Use", true);
                    int sec = m.getInt("IntervalSeconds", m.getInt("Interval", 0));
                    if (sec > 0) {
                        ticks = sec * 20L;
                    } else if (m.getInt("IntervalTicks", 0) > 0) {
                        ticks = m.getLong("IntervalTicks");
                    }
                    max = m.getInt("MaxRemoved", m.getInt("maxRemoved", 48));
                }
            }
            return new CullSettings(use && ticks > 0, Math.max(1L, ticks), Math.max(0, max), false);
        }

        static @NotNull CullSettings blockCullFromRoot(@NotNull FileConfiguration c) {
            boolean scheduleAllowed = periodicCullAllowed(c);
            ConfigurationSection ac = ConfigUtil.sectionCI(c, "AutoClean");
            boolean use = scheduleAllowed;
            long ticks = 400L;
            int max = 12;
            boolean breakNat = false;
            if (ac != null) {
                ConfigurationSection b = ConfigUtil.sectionCI(ac, "Blocks");
                if (b != null) {
                    use = scheduleAllowed && b.getBoolean("Use", true);
                    int sec = b.getInt("IntervalSeconds", b.getInt("Interval", 0));
                    if (sec > 0) {
                        ticks = sec * 20L;
                    } else if (b.getInt("IntervalTicks", 0) > 0) {
                        ticks = b.getLong("IntervalTicks");
                    }
                    max = b.getInt("MaxRemoved", b.getInt("maxRemoved", 12));
                    breakNat = b.getBoolean("BreakNaturally", b.getBoolean("break-naturally", false));
                }
            }
            return new CullSettings(use && ticks > 0, Math.max(1L, ticks), Math.max(0, max), breakNat);
        }
    }

    public record MobSection(
            @NotNull WorldPolicy worlds,
            int defaultPerType,
            /** Cap for non-player, non-mob-cap types without an {@code EntityTypes} row. {@code -1} disables. */
            int fallbackNonLivingPerType,
            int totalMaxPerChunk,
            @NotNull EnumMap<EntityType, EntityTypeRules> entityTypeRules,
            boolean entitySpawnCheck,
            boolean groupChunks,
            int groupChunkRadius,
            @NotNull Set<String> excludedChunkKeys,
            @NotNull EnumMap<SpawnReason, Boolean> spawnReasons,
            @NotNull CullSettings cull,
            /**
             * Max chunk region tasks scheduled per server tick during mob AutoClean wave.
             * {@code <= 0} = one burst (legacy: all chunks same callback).
             */
            int mobCullSliceChunksPerTick,
            /**
             * When {@link #groupChunks} is true, only run mob cull from chunk anchors on a grid of stride {@code 2*groupChunkRadius+1}
             * so overlapping group passes are not repeated for every chunk.
             */
            boolean mobCullAnchorDedup
    ) {
        static @NotNull MobSection load(@NotNull FileConfiguration c, @NotNull IgnoreSettings ignore) {
            ConfigurationSection mobsRoot = ConfigUtil.sectionCI(c, "Mobs");
            List<String> allow = mobsRoot != null ? mobsRoot.getStringList("WorldsAllow") : List.of();
            if (allow.isEmpty() && mobsRoot != null) {
                allow = mobsRoot.getStringList("worldsallow");
            }
            WorldPolicy worlds = new WorldPolicy(allow, ignore.worldsLower());

            ConfigurationSection es = ConfigUtil.sectionCI(c, "EntitySpawn");
            boolean spawnCheck = es == null || es.getBoolean("Check", true);
            boolean group = es != null && es.getBoolean("GroupChunks", false);
            int gr = 0;
            if (es != null) {
                int raw = Math.clamp(es.getInt("GroupChunksRadius", 2), 1, 3);
                gr = Math.clamp(raw - 1, 0, 3);
            }

            int defPer = 28;
            int fallbackNl = -1;
            int total = -1;
            if (mobsRoot != null) {
                defPer = mobsRoot.getInt("DefaultPerType", mobsRoot.getInt("default-per-type", 28));
                fallbackNl = mobsRoot.getInt("FallbackNonLivingLimit", mobsRoot.getInt("fallback-non-living-limit", -1));
                total = mobsRoot.getInt("TotalMaxPerChunk", mobsRoot.getInt("total-max-per-chunk", -1));
            }

            EnumMap<EntityType, EntityTypeRules> rules = new EnumMap<>(EntityType.class);
            ConfigurationSection et = null;
            for (String key : c.getKeys(false)) {
                if (key.equalsIgnoreCase("EntityTypes")) {
                    et = c.getConfigurationSection(key);
                    break;
                }
            }
            if (et != null) {
                for (String name : et.getKeys(false)) {
                    EntityType t = parseEntityType(name);
                    if (t == null) {
                        continue;
                    }
                    EntityTypeRules parsed = EntityTypeRules.fromSection(et.getConfigurationSection(name));
                    rules.put(t, parsed);
                }
            }
            if (!rules.containsKey(EntityType.PLAYER)) {
                rules.put(EntityType.PLAYER, new EntityTypeRules(true, -1, false, true));
            }

            Set<String> excluded = loadExcludedChunks(c);
            EnumMap<SpawnReason, Boolean> reasons = loadSpawnReasons(c);

            int mobSlice = 64;
            boolean anchorDedup = true;
            ConfigurationSection acRoot = ConfigUtil.sectionCI(c, "AutoClean");
            ConfigurationSection acMobs = acRoot != null ? ConfigUtil.sectionCI(acRoot, "Mobs") : null;
            if (acMobs != null) {
                mobSlice = acMobs.getInt("SliceChunksPerTick", mobSlice);
                anchorDedup = acMobs.getBoolean("DeduplicateGroupCull", true);
            }

            return new MobSection(
                    worlds,
                    defPer,
                    fallbackNl,
                    total,
                    rules,
                    spawnCheck,
                    group,
                    gr,
                    excluded,
                    reasons,
                    CullSettings.mobCullFromRoot(c),
                    mobSlice,
                    anchorDedup
            );
        }

        public @Nullable EntityTypeRules rule(@NotNull EntityType type) {
            EntityTypeRules explicit = entityTypeRules.get(type);
            if (explicit != null) {
                return explicit;
            }
            if (EntityCategories.isMobCapTarget(type) && defaultPerType >= 0) {
                return new EntityTypeRules(true, defaultPerType, true, true);
            }
            if (fallbackNonLivingPerType >= 0 && type != EntityType.PLAYER && type != EntityType.UNKNOWN) {
                if (!EntityCategories.isMobCapTarget(type)) {
                    return new EntityTypeRules(true, fallbackNonLivingPerType, true, true);
                }
            }
            return null;
        }

        public int limit(@NotNull EntityType type) {
            EntityTypeRules r = rule(type);
            if (r == null) {
                return -1;
            }
            return r.effectiveLimit();
        }

        public boolean autoClean(@NotNull EntityType type) {
            EntityTypeRules r = rule(type);
            return r != null && r.use() && r.autoClean() && r.effectiveLimit() >= 0;
        }

        public boolean naturalSpawn(@NotNull EntityType type) {
            EntityTypeRules r = rule(type);
            return r == null || r.naturalSpawn();
        }

        public boolean spawnReasonAllowed(@NotNull SpawnReason reason) {
            return spawnReasons.getOrDefault(reason, true);
        }

        public boolean isChunkExcluded(@NotNull World world, int chunkX, int chunkZ) {
            String k = world.getName().toLowerCase(Locale.ROOT) + ":" + chunkX + ":" + chunkZ;
            return excludedChunkKeys.contains(k);
        }

        private static @NotNull Set<String> loadExcludedChunks(@NotNull FileConfiguration c) {
            List<String> raw = c.getStringList("ExcludedChunks");
            if (raw.isEmpty()) {
                raw = c.getStringList("excludedchunks");
            }
            Set<String> s = new HashSet<>();
            for (String line : raw) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                s.add(line.trim().toLowerCase(Locale.ROOT).replace(" ", ""));
            }
            return s.isEmpty() ? Set.of() : Set.copyOf(s);
        }

        private static @NotNull EnumMap<SpawnReason, Boolean> loadSpawnReasons(@NotNull FileConfiguration c) {
            EnumMap<SpawnReason, Boolean> map = new EnumMap<>(SpawnReason.class);
            ConfigurationSection sr = ConfigUtil.sectionCI(c, "SpawnReasons");
            if (sr != null) {
                for (String key : sr.getKeys(false)) {
                    try {
                        SpawnReason r = SpawnReason.valueOf(key.toUpperCase(Locale.ROOT));
                        map.put(r, sr.getBoolean(key, true));
                    } catch (IllegalArgumentException ignored) {
                        // skip unknown keys
                    }
                }
            }
            for (SpawnReason r : SpawnReason.values()) {
                map.putIfAbsent(r, true);
            }
            return map;
        }

        private static EntityType parseEntityType(String key) {
            try {
                return EntityType.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public record MechanicsSection(
            boolean enabled,
            @NotNull WorldPolicy worlds,
            int defaultPerMaterial,
            int defaultPerMechanicsEntity,
            @NotNull EnumMap<Material, Integer> perMaterial,
            @NotNull Set<EntityType> mechanicsEntityTypes,
            @NotNull EnumMap<EntityType, Integer> perMechanicsEntity,
            @NotNull CullSettings cull,
            boolean blockTypesLimitEnabled,
            boolean checkPistonMove,
            /** {@code <= 0} = burst all chunk tasks in one callback (legacy). */
            int mechanicsCullSliceChunksPerTick
    ) {
        static @NotNull MechanicsSection load(@NotNull FileConfiguration c, @NotNull IgnoreSettings ignore) {
            ConfigurationSection mobsRoot = ConfigUtil.sectionCI(c, "Mobs");
            List<String> allow = mobsRoot != null ? mobsRoot.getStringList("WorldsAllow") : List.of();
            WorldPolicy worlds = new WorldPolicy(allow, ignore.worldsLower());

            ConfigurationSection bt = ConfigUtil.sectionCI(c, "BlockTypes");
            boolean limitBlocks = bt == null || bt.getBoolean("Limit", true);
            boolean piston = bt != null && bt.getBoolean("CheckPistonMove", false);

            int defMat = 16;
            EnumMap<Material, Integer> perMat = new EnumMap<>(Material.class);
            if (bt != null) {
                ConfigurationSection list = ConfigUtil.sectionCI(bt, "List");
                if (list != null) {
                    for (String key : list.getKeys(false)) {
                        Material mat = Material.matchMaterial(key, false);
                        if (mat != null && mat.isBlock()) {
                            perMat.put(mat, list.getInt(key, defMat));
                        }
                    }
                }
            }
            if (perMat.isEmpty()) {
                for (Material m : MechanicsMaterials.defaultTracked()) {
                    perMat.put(m, defMat);
                }
            }

            EnumSet<EntityType> mechTypes = EnumSet.noneOf(EntityType.class);
            EnumMap<EntityType, Integer> mechLimits = new EnumMap<>(EntityType.class);
            ConfigurationSection et = null;
            for (String key : c.getKeys(false)) {
                if (key.equalsIgnoreCase("EntityTypes")) {
                    et = c.getConfigurationSection(key);
                    break;
                }
            }
            int defEnt = 12;
            if (et != null) {
                for (String name : et.getKeys(false)) {
                    EntityType t = parseEntityType(name);
                    if (t == null) {
                        continue;
                    }
                    EntityTypeRules r = EntityTypeRules.fromSection(et.getConfigurationSection(name));
                    if (!EntityCategories.isMobCapTarget(t) && r.use() && r.effectiveLimit() >= 0) {
                        mechTypes.add(t);
                        mechLimits.put(t, r.effectiveLimit());
                    }
                }
            }
            if (mechTypes.isEmpty()) {
                mechTypes.add(EntityType.HOPPER_MINECART);
                mechTypes.add(EntityType.ITEM_FRAME);
                mechTypes.add(EntityType.GLOW_ITEM_FRAME);
                mechLimits.put(EntityType.HOPPER_MINECART, 4);
                mechLimits.put(EntityType.ITEM_FRAME, 24);
                mechLimits.put(EntityType.GLOW_ITEM_FRAME, 24);
            }

            int mechSlice = 64;
            ConfigurationSection acRoot = ConfigUtil.sectionCI(c, "AutoClean");
            ConfigurationSection acBlocks = acRoot != null ? ConfigUtil.sectionCI(acRoot, "Blocks") : null;
            if (acBlocks != null) {
                mechSlice = acBlocks.getInt("SliceChunksPerTick", mechSlice);
            }

            return new MechanicsSection(
                    limitBlocks || !mechTypes.isEmpty(),
                    worlds,
                    defMat,
                    defEnt,
                    perMat,
                    mechTypes,
                    mechLimits,
                    CullSettings.blockCullFromRoot(c),
                    limitBlocks,
                    piston,
                    mechSlice
            );
        }

        public int limit(@NotNull Material material) {
            if (!blockTypesLimitEnabled) {
                return -1;
            }
            Integer v = perMaterial.get(material);
            return v != null ? v : -1;
        }

        public boolean tracksEntityType(@NotNull EntityType type) {
            return mechanicsEntityTypes.contains(type);
        }

        public int mechanicsEntityLimit(@NotNull EntityType type) {
            if (!tracksEntityType(type)) {
                return -1;
            }
            return perMechanicsEntity.getOrDefault(type, defaultPerMechanicsEntity);
        }

        public @NotNull Set<Material> trackedMaterialsView() {
            return perMaterial.keySet();
        }

        private static EntityType parseEntityType(String key) {
            try {
                return EntityType.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
