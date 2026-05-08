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

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Block materials tracked by the mechanics subsystem when merging defaults.
 * Server-specific materials are added via config keys; this set is the baseline.
 */
public final class MechanicsMaterials {

    private static final EnumSet<Material> BASE = EnumSet.noneOf(Material.class);

    static {
        add(Material.HOPPER);
        add(Material.FURNACE);
        add(Material.BLAST_FURNACE);
        add(Material.SMOKER);
        add(Material.CHEST);
        add(Material.TRAPPED_CHEST);
        add(Material.ENDER_CHEST);
        add(Material.BARREL);
        add(Material.DISPENSER);
        add(Material.DROPPER);
        add(Material.BREWING_STAND);
        add(Material.ENCHANTING_TABLE);
        add(Material.BEACON);
        add(Material.LECTERN);
        add(Material.JUKEBOX);
        add(Material.COMPOSTER);
        add(Material.DAYLIGHT_DETECTOR);
        add(Material.SCULK_SENSOR);
        add(Material.CALIBRATED_SCULK_SENSOR);
        add(Material.SCULK_SHRIEKER);
        add(Material.SCULK_CATALYST);
        add(Material.DECORATED_POT);
        add(Material.CRAFTER);
        add(Material.VAULT);
        for (Material m : Material.values()) {
            if (!m.isBlock()) {
                continue;
            }
            String n = m.name();
            if (n.endsWith("_SHULKER_BOX")) {
                BASE.add(m);
            }
        }
    }

    private static void add(Material m) {
        if (m != null && m.isBlock()) {
            BASE.add(m);
        }
    }

    private MechanicsMaterials() {}

    public static @NotNull Set<Material> defaultTracked() {
        return EnumSet.copyOf(BASE);
    }
}
