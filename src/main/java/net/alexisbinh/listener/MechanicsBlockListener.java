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

import java.util.Collection;
import net.alexisbinh.LagProtectorPlugin;
import net.alexisbinh.config.PluginConfig;
import net.alexisbinh.util.ThrottledLog;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

public final class MechanicsBlockListener implements Listener {

    private final LagProtectorPlugin plugin;
    private final ThrottledLog log;

    public MechanicsBlockListener(@NotNull LagProtectorPlugin plugin) {
        this.plugin = plugin;
        this.log = new ThrottledLog(plugin, 8, 1000L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        PluginConfig cfg = plugin.configState();
        if (!cfg.mechanics().enabled() || !cfg.mechanics().worlds().allows(event.getBlock().getWorld())) {
            return;
        }
        Material mat = event.getBlock().getType();
        int limit = cfg.mechanics().limit(mat);
        if (limit < 0) {
            return;
        }
        Chunk chunk = event.getBlock().getChunk();
        int existing = countTileMaterial(chunk, mat);
        if (existing + 1 > limit) {
            event.setCancelled(true);
            log.debug("Cancelled block place material=" + mat + " at " + event.getBlock().getLocation());
        }
    }

    private static int countTileMaterial(@NotNull Chunk chunk, @NotNull Material mat) {
        Collection<BlockState> states = chunk.getTileEntities((Block b) -> b.getType() == mat, false);
        return states.size();
    }
}
