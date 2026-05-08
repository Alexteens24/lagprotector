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
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTypeRulesTest {

    @Test
    void fromNullSectionIsDisabled() {
        EntityTypeRules r = EntityTypeRules.fromSection(null);
        assertEquals(EntityTypeRules.DISABLED, r);
        assertEquals(-1, r.effectiveLimit());
    }

    @Test
    void effectiveLimitUnlimitedWhenUseFalse() {
        FileConfiguration c = TestFixtures.loadYaml("Use: false\nLimit: 10\n");
        EntityTypeRules r = EntityTypeRules.fromSection(c);
        assertEquals(-1, r.effectiveLimit());
    }

    @Test
    void effectiveLimitUnlimitedWhenLimitNegative() {
        FileConfiguration c = TestFixtures.loadYaml("Use: true\nLimit: -1\n");
        EntityTypeRules r = EntityTypeRules.fromSection(c);
        assertEquals(-1, r.effectiveLimit());
    }

    @Test
    void effectiveLimitWhenCapped() {
        FileConfiguration c = TestFixtures.loadYaml("Use: true\nLimit: 7\nAutoClean: false\nNaturalSpawn: false\n");
        EntityTypeRules r = EntityTypeRules.fromSection(c);
        assertEquals(7, r.effectiveLimit());
        assertTrue(r.use());
        assertFalse(r.autoClean());
        assertFalse(r.naturalSpawn());
    }
}
