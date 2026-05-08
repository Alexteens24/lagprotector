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

public final class ConfigUtil {

    private ConfigUtil() {}

    /**
     * Finds a child section matching any alias (case-insensitive key match).
     */
    public static @Nullable ConfigurationSection sectionCI(@Nullable ConfigurationSection parent, @NotNull String... aliases) {
        if (parent == null) {
            return null;
        }
        for (String a : aliases) {
            ConfigurationSection s = parent.getConfigurationSection(a);
            if (s != null) {
                return s;
            }
        }
        for (String key : parent.getKeys(false)) {
            for (String a : aliases) {
                if (key.equalsIgnoreCase(a)) {
                    return parent.getConfigurationSection(key);
                }
            }
        }
        return null;
    }
}
