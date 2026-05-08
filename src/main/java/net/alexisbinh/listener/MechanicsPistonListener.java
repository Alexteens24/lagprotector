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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alexisbinh.LagProtectorPlugin;
import net.alexisbinh.config.PluginConfig;
import net.alexisbinh.util.ThrottledLog;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * When {@code BlockTypes.CheckPistonMove} is true, cancels piston moves that would push/pull tracked
 * tiles across chunk boundaries such that a chunk would exceed its per-material cap.
 */
public final class MechanicsPistonListener implements Listener {

    private final LagProtectorPlugin plugin;
    private final ThrottledLog log;

    public MechanicsPistonListener(@NotNull LagProtectorPlugin plugin) {
        this.plugin = plugin;
        this.log = new ThrottledLog(plugin, 8, 1000L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExtend(@NotNull BlockPistonExtendEvent event) {
        if (wouldViolate(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRetract(@NotNull BlockPistonRetractEvent event) {
        BlockFace pull = event.getDirection().getOppositeFace();
        if (wouldViolate(event.getBlock(), event.getBlocks(), pull)) {
            event.setCancelled(true);
        }
    }

    private boolean wouldViolate(@NotNull Block pistonBlock, @NotNull List<Block> moving, @NotNull BlockFace step) {
        PluginConfig cfg = plugin.configState();
        if (!cfg.mechanics().enabled() || !cfg.mechanics().checkPistonMove()) {
            return false;
        }
        if (!cfg.mechanics().worlds().allows(pistonBlock.getWorld())) {
            return false;
        }
        Map<Chunk, EnumMap<Material, Integer>> delta = new HashMap<>();
        for (Block b : moving) {
            Material mat = b.getType();
            int lim = cfg.mechanics().limit(mat);
            if (lim < 0) {
                continue;
            }
            Chunk from = b.getChunk();
            Chunk to = b.getRelative(step).getChunk();
            if (from.equals(to)) {
                continue;
            }
            addDelta(delta, to, mat, 1);
            addDelta(delta, from, mat, -1);
        }
        Map<Chunk, EnumMap<Material, Integer>> tileCountCache = new HashMap<>();
        for (Map.Entry<Chunk, EnumMap<Material, Integer>> e : delta.entrySet()) {
            Chunk ch = e.getKey();
            for (Map.Entry<Material, Integer> me : e.getValue().entrySet()) {
                Material mat = me.getKey();
                int lim = cfg.mechanics().limit(mat);
                if (lim < 0) {
                    continue;
                }
                int d = me.getValue();
                int cur = cachedTileCount(tileCountCache, ch, mat);
                if (cur + d > lim) {
                    log.debug("Cancelled piston: chunk " + ch.getX() + "," + ch.getZ() + " material=" + mat + " would exceed " + lim);
                    return true;
                }
            }
        }
        return false;
    }

    private static int cachedTileCount(
            @NotNull Map<Chunk, EnumMap<Material, Integer>> cache,
            @NotNull Chunk chunk,
            @NotNull Material mat
    ) {
        return cache
                .computeIfAbsent(chunk, c -> new EnumMap<>(Material.class))
                .computeIfAbsent(mat, m -> countTileMaterial(chunk, m));
    }

    private static void addDelta(
            @NotNull Map<Chunk, EnumMap<Material, Integer>> delta,
            @NotNull Chunk ch,
            @NotNull Material mat,
            int inc
    ) {
        EnumMap<Material, Integer> m = delta.computeIfAbsent(ch, k -> new EnumMap<>(Material.class));
        int v = m.getOrDefault(mat, 0) + inc;
        m.put(mat, v);
    }

    private static int countTileMaterial(@NotNull Chunk chunk, @NotNull Material mat) {
        return chunk.getTileEntities((Block b) -> b.getType() == mat, false).size();
    }
}
