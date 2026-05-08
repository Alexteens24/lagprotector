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

import net.alexisbinh.TestFixtures;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThrottledLogTest {

    @Test
    void debugOffDoesNotLog() {
        FileConfiguration cfg = TestFixtures.loadYaml("debug: false\n");
        JavaPlugin plugin = TestFixtures.pluginWithConfig(cfg);
        var logger = Mockito.mock(java.util.logging.Logger.class);
        when(plugin.getLogger()).thenReturn(logger);

        ThrottledLog log = new ThrottledLog(plugin, 2, 60_000L);
        log.debug("one");
        log.debug("two");
        verify(logger, never()).info(anyString());
    }

    @Test
    void debugOnRespectsPerWindowCap() {
        FileConfiguration cfg = TestFixtures.loadYaml("debug: true\n");
        JavaPlugin plugin = TestFixtures.pluginWithConfig(cfg);
        var logger = Mockito.mock(java.util.logging.Logger.class);
        when(plugin.getLogger()).thenReturn(logger);

        ThrottledLog log = new ThrottledLog(plugin, 2, 60_000L);
        for (int i = 0; i < 5; i++) {
            log.debug("msg");
        }
        verify(logger, times(2)).info(contains("[debug] msg"));
        verify(logger, times(1)).info(contains("throttled"));
    }
}
