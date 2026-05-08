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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.NotNull;

/**
 * Spawn reasons treated as "natural-style" when {@code EntityTypes.*.NaturalSpawn: false} is enforced.
 */
public final class NaturalSpawnReasons {

    private static final Set<SpawnReason> NATURAL_STYLE;

    static {
        EnumSet<SpawnReason> s = EnumSet.of(
                SpawnReason.NATURAL,
                SpawnReason.JOCKEY,
                SpawnReason.SPAWNER,
                SpawnReason.TRIAL_SPAWNER,
                SpawnReason.PATROL,
                SpawnReason.RAID,
                SpawnReason.VILLAGE_DEFENSE,
                SpawnReason.VILLAGE_INVASION,
                SpawnReason.BEEHIVE,
                SpawnReason.NETHER_PORTAL,
                SpawnReason.REINFORCEMENTS,
                SpawnReason.DROWNED,
                SpawnReason.MOUNT
        );
        // Avoid direct SpawnReason.CHUNK_GEN reference (deprecated for removal in 1.21+ API).
        try {
            s.add(SpawnReason.valueOf("CHUNK_GEN"));
        } catch (IllegalArgumentException ignored) {
            // Enum constant removed in a future version
        }
        NATURAL_STYLE = Collections.unmodifiableSet(s);
    }

    private NaturalSpawnReasons() {}

    public static boolean isNaturalStyle(@NotNull SpawnReason reason) {
        return NATURAL_STYLE.contains(reason);
    }
}
