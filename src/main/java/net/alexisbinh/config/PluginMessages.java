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

import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Loads {@code messages.yml} from the plugin data folder (created from defaults on first run).
 */
public final class PluginMessages {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private FileConfiguration messages = new YamlConfiguration();

    public void reload(@NotNull JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public @NotNull Component commandNoPermission() {
        return LEGACY_AMPERSAND.deserialize(messages.getString("command.no-permission", "&cYou do not have permission."));
    }

    public @NotNull Component commandReloadSuccess() {
        return LEGACY_AMPERSAND.deserialize(messages.getString("command.reload-success", "&aLagProtector configuration reloaded."));
    }

    public @NotNull Component commandUsage() {
        return LEGACY_AMPERSAND.deserialize(messages.getString("command.usage", "&eUsage: /lagprotector reload"));
    }

    /** Plain text for {@link java.util.logging.Logger} (strip legacy § codes if present). */
    public @NotNull String logCullRemoved(int count) {
        String template = messages.getString("log.cull-removed", "[LagProtector] Removed {count} excess entities (cull pass).");
        Component line = LEGACY_AMPERSAND.deserialize(template.replace("{count}", String.valueOf(count)));
        return PlainTextComponentSerializer.plainText().serialize(line);
    }
}
