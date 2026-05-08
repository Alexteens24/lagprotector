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

package net.alexisbinh.listener;

import java.util.List;
import net.alexisbinh.LagProtectorPlugin;
import net.alexisbinh.config.EntityTypeRules;
import net.alexisbinh.config.PluginConfig;
import net.alexisbinh.util.ChunkGroupUtil;
import net.alexisbinh.util.ThrottledLog;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EntitySpawnLimiter implements Listener {

    private static final Entity[] NO_ENTITIES = new Entity[0];

    private final LagProtectorPlugin plugin;
    private final ThrottledLog log;

    public EntitySpawnLimiter(@NotNull LagProtectorPlugin plugin) {
        this.plugin = plugin;
        this.log = new ThrottledLog(plugin, 8, 1000L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
        PluginConfig cfg = plugin.configState();
        if (!cfg.mobs().entitySpawnCheck()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) {
            return;
        }
        if (!cfg.mobs().worlds().allows(event.getEntity().getWorld())) {
            return;
        }
        if (cfg.mobs().isChunkExcluded(event.getEntity().getWorld(), event.getLocation().getChunk().getX(), event.getLocation().getChunk().getZ())) {
            return;
        }
        if (!cfg.mobs().spawnReasonAllowed(event.getSpawnReason())) {
            event.setCancelled(true);
            return;
        }
        var type = event.getEntityType();
        if (!cfg.mobs().naturalSpawn(type) && NaturalSpawnReasons.isNaturalStyle(event.getSpawnReason())) {
            event.setCancelled(true);
            log.debug("Cancelled creature spawn (NaturalSpawn=false) type=" + type + " reason=" + event.getSpawnReason());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(@NotNull EntitySpawnEvent event) {
        PluginConfig cfg = plugin.configState();
        if (!cfg.mobs().entitySpawnCheck()) {
            return;
        }
        World world = event.getEntity().getWorld();
        if (!cfg.mobs().worlds().allows(world)) {
            return;
        }
        Chunk chunk = event.getLocation().getChunk();
        if (cfg.mobs().isChunkExcluded(world, chunk.getX(), chunk.getZ())) {
            return;
        }
        var type = event.getEntityType();
        Entity self = event.getEntity();
        EntityTypeRules rule = cfg.mobs().rule(type);
        if (rule == null || !rule.use() || rule.effectiveLimit() < 0) {
            return;
        }
        int limit = rule.effectiveLimit();
        int totalLimit = cfg.mobs().totalMaxPerChunk();
        boolean needTotal = totalLimit >= 0
                && self instanceof LivingEntity
                && !(self instanceof Player)
                && !cfg.mechanics().tracksEntityType(type);

        int count;
        int totalLiving = 0;
        if (needTotal) {
            TypeLivingCount both = countTypeAndLivingForSpawn(chunk, type, cfg, self);
            count = both.typeCount();
            totalLiving = both.livingTotal();
        } else {
            count = countEntitiesOfType(chunk, type, cfg, self);
        }
        if (count >= limit) {
            event.setCancelled(true);
            log.debug("Cancelled spawn type=" + type + " chunk=" + chunk.getX() + "," + chunk.getZ());
            return;
        }
        if (needTotal && totalLiving >= totalLimit) {
            event.setCancelled(true);
            log.debug("Cancelled spawn (total cap) chunk=" + chunk.getX() + "," + chunk.getZ());
        }
    }

    private record TypeLivingCount(int typeCount, int livingTotal) {}

    /**
     * Single pass over the chunk group: count entities of {@code type} excluding {@code exclude}, and count living
     * non-players (excluding mechanics-tracked) excluding {@code exclude}.
     */
    private @NotNull TypeLivingCount countTypeAndLivingForSpawn(
            @NotNull Chunk origin,
            @NotNull org.bukkit.entity.EntityType type,
            @NotNull PluginConfig cfg,
            @Nullable Entity exclude
    ) {
        int typeN = 0;
        int livingN = 0;
        for (Chunk ch : chunksForCount(origin, cfg)) {
            for (Entity e : entitiesForCount(ch)) {
                if (e == exclude) {
                    continue;
                }
                if (e.getType() == type) {
                    typeN++;
                }
                if (e instanceof LivingEntity && !(e instanceof Player)) {
                    if (cfg.mechanics().tracksEntityType(e.getType())) {
                        continue;
                    }
                    livingN++;
                }
            }
        }
        return new TypeLivingCount(typeN, livingN);
    }

    private int countEntitiesOfType(
            @NotNull Chunk origin,
            @NotNull org.bukkit.entity.EntityType type,
            @NotNull PluginConfig cfg,
            @Nullable Entity exclude
    ) {
        int n = 0;
        for (Chunk ch : chunksForCount(origin, cfg)) {
            for (Entity e : entitiesForCount(ch)) {
                if (e != exclude && e.getType() == type) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Same rationale as cull: {@link Chunk#getEntities()} forces loads when entities are not loaded (Paper API).
     */
    private static @NotNull Entity[] entitiesForCount(@NotNull Chunk ch) {
        if (!ch.isLoaded() || !ch.isEntitiesLoaded()) {
            return NO_ENTITIES;
        }
        return ch.getEntities();
    }

    private @NotNull Iterable<Chunk> chunksForCount(@NotNull Chunk origin, @NotNull PluginConfig cfg) {
        if (!cfg.mobs().groupChunks()) {
            return List.of(origin);
        }
        return ChunkGroupUtil.chunksInGroup(origin.getWorld(), origin.getX(), origin.getZ(), cfg.mobs().groupChunkRadius());
    }
}
