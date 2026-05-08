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

package net.alexisbinh.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Allow-list (optional) plus deny-list (e.g. {@code Ignore.Worlds}). Empty allow = all worlds except denied.
 */
public final class WorldPolicy {

    private final Set<String> allowLower;
    private final Set<String> denyLower;

    public WorldPolicy(@NotNull List<String> allowWorlds, @NotNull Set<String> denyWorldsLower) {
        this.allowLower = toSet(allowWorlds);
        this.denyLower = denyWorldsLower.isEmpty() ? Set.of() : Set.copyOf(denyWorldsLower);
    }

    private static @NotNull Set<String> toSet(@NotNull List<String> worlds) {
        if (worlds.isEmpty()) {
            return Set.of();
        }
        HashSet<String> s = new HashSet<>();
        for (String w : worlds) {
            s.add(w.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(s);
    }

    public boolean allows(@NotNull World world) {
        String n = world.getName().toLowerCase(Locale.ROOT);
        if (!denyLower.isEmpty() && denyLower.contains(n)) {
            return false;
        }
        if (allowLower.isEmpty()) {
            return true;
        }
        return allowLower.contains(n);
    }
}
