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

import net.alexisbinh.TestFixtures;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigUtilTest {

    @Test
    void sectionCIFindsExactKey() {
        FileConfiguration c = TestFixtures.loadYaml("Foo:\n  bar: 1\n");
        assertNotNull(ConfigUtil.sectionCI(c, "Foo"));
    }

    @Test
    void sectionCIFindsCaseInsensitiveKey() {
        FileConfiguration c = TestFixtures.loadYaml("ignore:\n  Worlds: []\n");
        ConfigurationSection s = ConfigUtil.sectionCI(c, "Ignore");
        assertNotNull(s);
        assertNotNull(s.getStringList("Worlds"));
    }

    @Test
    void sectionCITriesAliasesInOrder() {
        FileConfiguration c = TestFixtures.loadYaml("IGNORE:\n  x: 1\n");
        ConfigurationSection s = ConfigUtil.sectionCI(c, "Ignore", "IGNORE");
        assertNotNull(s);
        assertEquals(1, s.getInt("x"));
    }

    @Test
    void sectionCIReturnsNullWhenMissing() {
        FileConfiguration c = TestFixtures.loadYaml("Other: {}\n");
        assertNull(ConfigUtil.sectionCI(c, "Ignore"));
    }

    @Test
    void sectionCINullParent() {
        assertNull(ConfigUtil.sectionCI(null, "Ignore"));
    }
}
