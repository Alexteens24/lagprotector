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
import java.nio.file.Files;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginMessagesTest {

    @Test
    void reloadReadsBundledStyleFile(@TempDir File folder) throws Exception {
        File msg = new File(folder, "messages.yml");
        Files.writeString(msg.toPath(), """
                command:
                  no-permission: "&cNope"
                  reload-success: "&aOK"
                  usage: "&eUse reload"
                log:
                  cull-removed: "Removed {count} things"
                """);

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder);

        PluginMessages messages = new PluginMessages();
        messages.reload(plugin);

        assertEquals("Nope", PlainTextComponentSerializer.plainText().serialize(messages.commandNoPermission()));
        assertEquals("OK", PlainTextComponentSerializer.plainText().serialize(messages.commandReloadSuccess()));
        assertTrue(messages.logCullRemoved(3).contains("3"));
    }
}
