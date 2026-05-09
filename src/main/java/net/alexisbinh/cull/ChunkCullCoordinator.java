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

package net.alexisbinh.cull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.alexisbinh.LagProtectorPlugin;
import net.alexisbinh.config.EntityTypeRules;
import net.alexisbinh.config.PluginConfig;
import net.alexisbinh.util.ChunkGroupUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ChunkCullCoordinator {

    private static final Entity[] NO_ENTITIES = new Entity[0];

    private final LagProtectorPlugin plugin;
    private @Nullable ScheduledTask mobTask;
    private @Nullable ScheduledTask mechTask;

    private @Nullable List<ChunkRef> mobCullQueue;
    private int mobCullQueueIndex;
    private @Nullable ScheduledTask mobCullDrainTask;

    private @Nullable List<ChunkRef> mechCullQueue;
    private int mechCullQueueIndex;
    private @Nullable ScheduledTask mechCullDrainTask;

    public ChunkCullCoordinator(@NotNull LagProtectorPlugin plugin, @NotNull PluginConfig ignored) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        PluginConfig cfg = plugin.configState();
        var server = plugin.getServer();
        var global = server.getGlobalRegionScheduler();
        if (cfg.mobs().cull().enabled() && cfg.mobs().cull().periodTicks() > 0) {
            long period = Math.max(1L, cfg.mobs().cull().periodTicks());
            mobTask = global.runAtFixedRate(plugin, t -> scheduleMobPass(), period, period);
        }
        if (cfg.mechanics().enabled() && cfg.mechanics().cull().enabled() && cfg.mechanics().cull().periodTicks() > 0) {
            long period = Math.max(1L, cfg.mechanics().cull().periodTicks());
            mechTask = global.runAtFixedRate(plugin, t -> scheduleMechanicsPass(), period, period);
        }
    }

    public void restart() {
        start();
    }

    public void stop() {
        cancelMobCullDrain();
        cancelMechCullDrain();
        if (mobTask != null) {
            mobTask.cancel();
            mobTask = null;
        }
        if (mechTask != null) {
            mechTask.cancel();
            mechTask = null;
        }
    }

    private void scheduleMobPass() {
        PluginConfig cfg = plugin.configState();
        if (!cfg.mobs().cull().enabled()) {
            return;
        }
        beginMobCullWave(collectMobCullTargets(cfg));
    }

    private static @NotNull List<ChunkRef> collectMobCullTargets(@NotNull PluginConfig cfg) {
        List<ChunkRef> list = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (!cfg.mobs().worlds().allows(world)) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                int cx = chunk.getX();
                int cz = chunk.getZ();
                if (cfg.mobs().isChunkExcluded(world, cx, cz)) {
                    continue;
                }
                if (!includeMobCullAnchor(cx, cz, cfg)) {
                    continue;
                }
                list.add(new ChunkRef(world, cx, cz));
            }
        }
        return list;
    }

    /**
     * When group culling uses overlapping windows, only schedule from grid anchors (stride {@code 2r+1})
     * so each chunk still lies inside some processed window without N redundant passes per wave.
     */
    private static boolean includeMobCullAnchor(int chunkX, int chunkZ, @NotNull PluginConfig cfg) {
        if (!cfg.mobs().groupChunks() || !cfg.mobs().mobCullAnchorDedup()) {
            return true;
        }
        int r = cfg.mobs().groupChunkRadius();
        int stride = 2 * r + 1;
        return Math.floorMod(chunkX, stride) == 0 && Math.floorMod(chunkZ, stride) == 0;
    }

    private void beginMobCullWave(@NotNull List<ChunkRef> targets) {
        cancelMobCullDrain();
        if (targets.isEmpty()) {
            return;
        }
        mobCullQueue = targets;
        mobCullQueueIndex = 0;
        GlobalRegionScheduler global = plugin.getServer().getGlobalRegionScheduler();
        global.run(plugin, t -> drainMobCullSlice(global));
    }

    private void drainMobCullSlice(@NotNull GlobalRegionScheduler global) {
        PluginConfig cfg = plugin.configState();
        List<ChunkRef> queue = mobCullQueue;
        if (queue == null || !cfg.mobs().cull().enabled()) {
            cancelMobCullDrain();
            return;
        }
        int slice = cfg.mobs().mobCullSliceChunksPerTick();
        if (slice <= 0) {
            slice = Integer.MAX_VALUE;
        }
        int end = Math.min(mobCullQueueIndex + slice, queue.size());
        var regionScheduler = Bukkit.getServer().getRegionScheduler();
        for (int i = mobCullQueueIndex; i < end; i++) {
            ChunkRef ref = queue.get(i);
            World world = ref.world;
            final int cx = ref.chunkX;
            final int cz = ref.chunkZ;
            // Folia: never call getChunkAt from GlobalRegionScheduler; anchor loc only uses coords + world height.
            Location loc = anchor(world, cx, cz);
            regionScheduler.run(plugin, loc, t -> {
                PluginConfig live = plugin.configState();
                if (!live.mobs().cull().enabled()) {
                    return;
                }
                if (live.mobs().isChunkExcluded(world, cx, cz)) {
                    return;
                }
                Chunk chunk = world.getChunkAt(cx, cz);
                if (!chunk.isLoaded()) {
                    return;
                }
                cullEntities(chunk, live);
            });
        }
        mobCullQueueIndex = end;
        if (mobCullQueueIndex < queue.size()) {
            mobCullDrainTask = global.runDelayed(plugin, t -> drainMobCullSlice(global), 1L);
        } else {
            cancelMobCullDrain();
        }
    }

    private void cancelMobCullDrain() {
        if (mobCullDrainTask != null) {
            mobCullDrainTask.cancel();
            mobCullDrainTask = null;
        }
        mobCullQueue = null;
        mobCullQueueIndex = 0;
    }

    private void scheduleMechanicsPass() {
        PluginConfig cfg = plugin.configState();
        if (!cfg.mechanics().enabled() || !cfg.mechanics().cull().enabled()) {
            return;
        }
        beginMechCullWave(collectMechanicsCullTargets(cfg));
    }

    private static @NotNull List<ChunkRef> collectMechanicsCullTargets(@NotNull PluginConfig cfg) {
        List<ChunkRef> list = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (!cfg.mechanics().worlds().allows(world)) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                list.add(new ChunkRef(world, chunk.getX(), chunk.getZ()));
            }
        }
        return list;
    }

    private void beginMechCullWave(@NotNull List<ChunkRef> targets) {
        cancelMechCullDrain();
        if (targets.isEmpty()) {
            return;
        }
        mechCullQueue = targets;
        mechCullQueueIndex = 0;
        GlobalRegionScheduler global = plugin.getServer().getGlobalRegionScheduler();
        global.run(plugin, t -> drainMechCullSlice(global));
    }

    private void drainMechCullSlice(@NotNull GlobalRegionScheduler global) {
        PluginConfig cfg = plugin.configState();
        List<ChunkRef> queue = mechCullQueue;
        if (queue == null || !cfg.mechanics().enabled() || !cfg.mechanics().cull().enabled()) {
            cancelMechCullDrain();
            return;
        }
        int slice = cfg.mechanics().mechanicsCullSliceChunksPerTick();
        if (slice <= 0) {
            slice = Integer.MAX_VALUE;
        }
        int end = Math.min(mechCullQueueIndex + slice, queue.size());
        var regionScheduler = Bukkit.getServer().getRegionScheduler();
        for (int i = mechCullQueueIndex; i < end; i++) {
            ChunkRef ref = queue.get(i);
            World world = ref.world;
            final int cx = ref.chunkX;
            final int cz = ref.chunkZ;
            Location loc = anchor(world, cx, cz);
            regionScheduler.run(plugin, loc, t -> {
                PluginConfig live = plugin.configState();
                if (!live.mechanics().enabled() || !live.mechanics().cull().enabled()) {
                    return;
                }
                Chunk chunk = world.getChunkAt(cx, cz);
                if (!chunk.isLoaded()) {
                    return;
                }
                cullMechanicsBlocks(chunk, live);
            });
        }
        mechCullQueueIndex = end;
        if (mechCullQueueIndex < queue.size()) {
            mechCullDrainTask = global.runDelayed(plugin, t -> drainMechCullSlice(global), 1L);
        } else {
            cancelMechCullDrain();
        }
    }

    private void cancelMechCullDrain() {
        if (mechCullDrainTask != null) {
            mechCullDrainTask.cancel();
            mechCullDrainTask = null;
        }
        mechCullQueue = null;
        mechCullQueueIndex = 0;
    }

    private static final class ChunkRef {
        final World world;
        final int chunkX;
        final int chunkZ;

        ChunkRef(@NotNull World world, int chunkX, int chunkZ) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    /** Region-scheduler anchor for a chunk key; safe to call from global (no chunk load). */
    private static @NotNull Location anchor(@NotNull World world, int chunkX, int chunkZ) {
        int y = Math.clamp((world.getMinHeight() + world.getMaxHeight()) / 2, world.getMinHeight(), world.getMaxHeight() - 1);
        return new Location(world, (chunkX << 4) + 8, y, (chunkZ << 4) + 8);
    }

    private @NotNull List<Chunk> chunksFor(@NotNull Chunk origin, @NotNull PluginConfig cfg) {
        if (!cfg.mobs().groupChunks()) {
            return List.of(origin);
        }
        return ChunkGroupUtil.chunksInGroup(origin.getWorld(), origin.getX(), origin.getZ(), cfg.mobs().groupChunkRadius());
    }

    /**
     * Avoid {@link Chunk#getEntities()} when entities are not loaded — Paper API documents that call
     * as forcing loads; skipping keeps cull passes lighter on Folia.
     */
    private static @NotNull Entity[] entitiesForCull(@NotNull Chunk ch) {
        if (!ch.isLoaded() || !ch.isEntitiesLoaded()) {
            return NO_ENTITIES;
        }
        return ch.getEntities();
    }

    private void cullEntities(@NotNull Chunk chunk, @NotNull PluginConfig cfg) {
        int budget = cfg.mobs().cull().maxPerPass();
        if (budget <= 0) {
            return;
        }
        List<Chunk> group = chunksFor(chunk, cfg);
        EnumMap<EntityType, Integer> counts = new EnumMap<>(EntityType.class);
        int[] livingTotal = new int[1];
        buildCullEntityStats(group, cfg, counts, livingTotal);

        int removed = 0;
        while (budget > 0) {
            Entity victim = null;
            for (Chunk ch : group) {
                for (Entity e : entitiesForCull(ch)) {
                    if (!(e instanceof Player) && e.isValid() && !cfg.ignore().skipCull(e)) {
                        EntityTypeRules r = cfg.mobs().rule(e.getType());
                        if (r != null && r.use() && r.autoClean() && r.effectiveLimit() >= 0) {
                            int have = counts.getOrDefault(e.getType(), 0);
                            if (have > r.effectiveLimit()) {
                                victim = e;
                                break;
                            }
                        }
                    }
                }
                if (victim != null) {
                    break;
                }
            }
            if (victim != null) {
                adjustCullStatsAfterRemove(victim, counts, livingTotal, cfg);
                victim.remove();
                removed++;
                budget--;
                continue;
            }
            int totalLim = cfg.mobs().totalMaxPerChunk();
            if (totalLim >= 0 && livingTotal[0] > totalLim) {
                Entity t = pickLivingCullVictim(group, cfg);
                if (t != null) {
                    adjustCullStatsAfterRemove(t, counts, livingTotal, cfg);
                    t.remove();
                    removed++;
                    budget--;
                    continue;
                }
            }
            break;
        }
        maybeLogCull(removed);
    }

    private static void buildCullEntityStats(
            @NotNull List<Chunk> group,
            @NotNull PluginConfig cfg,
            @NotNull EnumMap<EntityType, Integer> counts,
            int[] livingTotalOut
    ) {
        for (Chunk ch : group) {
            for (Entity e : entitiesForCull(ch)) {
                if (!(e instanceof Player) && e.isValid() && !cfg.ignore().skipCull(e)) {
                    EntityTypeRules r = cfg.mobs().rule(e.getType());
                    if (r != null && r.use() && r.autoClean() && r.effectiveLimit() >= 0) {
                        EntityType t = e.getType();
                        counts.put(t, counts.getOrDefault(t, 0) + 1);
                    }
                }
                if (e instanceof LivingEntity && !(e instanceof Player) && e.isValid() && !cfg.ignore().skipCull(e)) {
                    if (!cfg.mechanics().tracksEntityType(e.getType())) {
                        livingTotalOut[0]++;
                    }
                }
            }
        }
    }

    /**
     * Mirrors {@link #buildCullEntityStats} so {@code counts} / {@code livingTotal} stay aligned after a removal.
     */
    private static void adjustCullStatsAfterRemove(
            @NotNull Entity victim,
            @NotNull EnumMap<EntityType, Integer> counts,
            int[] livingTotal,
            @NotNull PluginConfig cfg
    ) {
        if (!(victim instanceof Player) && victim.isValid() && !cfg.ignore().skipCull(victim)) {
            EntityTypeRules r = cfg.mobs().rule(victim.getType());
            if (r != null && r.use() && r.autoClean() && r.effectiveLimit() >= 0) {
                EntityType t = victim.getType();
                int v = counts.getOrDefault(t, 0);
                if (v > 0) {
                    counts.put(t, v - 1);
                }
            }
        }
        if (victim instanceof LivingEntity && !(victim instanceof Player) && victim.isValid() && !cfg.ignore().skipCull(victim)) {
            if (!cfg.mechanics().tracksEntityType(victim.getType())) {
                livingTotal[0]--;
            }
        }
    }

    private @Nullable Entity pickLivingCullVictim(@NotNull List<Chunk> group, @NotNull PluginConfig cfg) {
        for (Chunk ch : group) {
            for (Entity e : entitiesForCull(ch)) {
                if (e instanceof LivingEntity && !(e instanceof Player) && e.isValid() && !cfg.ignore().skipCull(e)) {
                    if (cfg.mechanics().tracksEntityType(e.getType())) {
                        continue;
                    }
                    return e;
                }
            }
        }
        return null;
    }

    private void maybeLogCull(int removed) {
        PluginConfig cfg = plugin.configState();
        if (cfg.log().enabled() && removed >= cfg.log().minRemoved()) {
            plugin.getLogger().info(plugin.messages().logCullRemoved(removed));
        }
    }

    private void cullMechanicsBlocks(@NotNull Chunk chunk, @NotNull PluginConfig cfg) {
        int budget = cfg.mechanics().cull().maxPerPass();
        if (budget <= 0) {
            return;
        }
        boolean breakNaturally = cfg.mechanics().cull().breakNaturally();
        EnumMap<Material, ArrayList<BlockState>> byMaterial = new EnumMap<>(Material.class);
        for (BlockState state : chunk.getTileEntities((Block b) -> cfg.mechanics().limit(b.getType()) >= 0, false)) {
            Material m = state.getType();
            byMaterial.computeIfAbsent(m, k -> new ArrayList<>()).add(state);
        }
        for (Material mat : cfg.mechanics().trackedMaterialsView()) {
            if (budget <= 0) {
                break;
            }
            int lim = cfg.mechanics().limit(mat);
            if (lim < 0) {
                continue;
            }
            ArrayList<BlockState> tiles = byMaterial.get(mat);
            int size = tiles == null ? 0 : tiles.size();
            int over = size - lim;
            if (over <= 0) {
                continue;
            }
            int remove = Math.min(over, budget);
            int removedBlocks = 0;
            if (tiles != null) {
                for (BlockState state : tiles) {
                    if (removedBlocks >= remove) {
                        break;
                    }
                    Block block = state.getBlock();
                    if (breakNaturally) {
                        block.breakNaturally(false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                    removedBlocks++;
                }
            }
            budget -= removedBlocks;
        }
    }
}
