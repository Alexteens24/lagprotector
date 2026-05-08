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

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-entity-type rules ({@code EntityTypes.NAME.*}).
 */
public record EntityTypeRules(boolean use, int limit, boolean autoClean, boolean naturalSpawn) {

    public static final @NotNull EntityTypeRules DISABLED = new EntityTypeRules(false, -1, false, true);

    public static @NotNull EntityTypeRules fromSection(@Nullable ConfigurationSection s) {
        if (s == null) {
            return DISABLED;
        }
        return new EntityTypeRules(
                s.getBoolean("Use", true),
                s.getInt("Limit", -1),
                s.getBoolean("AutoClean", true),
                s.getBoolean("NaturalSpawn", true)
        );
    }

    /** Effective cap for spawn/cull; {@code -1} means unlimited. */
    public int effectiveLimit() {
        if (!use || limit < 0) {
            return -1;
        }
        return limit;
    }
}
