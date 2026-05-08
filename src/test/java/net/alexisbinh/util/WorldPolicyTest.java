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

import java.util.List;
import java.util.Set;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class WorldPolicyTest {

    private static World worldNamed(String name) {
        World w = Mockito.mock(World.class);
        when(w.getName()).thenReturn(name);
        return w;
    }

    @Test
    void emptyAllowListAllowsNonDenied() {
        WorldPolicy p = new WorldPolicy(List.of(), Set.of());
        assertTrue(p.allows(worldNamed("world")));
    }

    @Test
    void denyListBlocks() {
        WorldPolicy p = new WorldPolicy(List.of(), Set.of("nether"));
        assertFalse(p.allows(worldNamed("nether")));
        assertTrue(p.allows(worldNamed("world")));
    }

    @Test
    void denyOverridesAllow() {
        WorldPolicy p = new WorldPolicy(List.of("world", "nether"), Set.of("nether"));
        assertFalse(p.allows(worldNamed("nether")));
        assertTrue(p.allows(worldNamed("world")));
    }

    @Test
    void allowListRestricts() {
        WorldPolicy p = new WorldPolicy(List.of("creative"), Set.of());
        assertTrue(p.allows(worldNamed("creative")));
        assertFalse(p.allows(worldNamed("world")));
    }

    @Test
    void allowMatchingIsCaseInsensitive() {
        WorldPolicy p = new WorldPolicy(List.of("MY_WORLD"), Set.of());
        assertTrue(p.allows(worldNamed("my_world")));
    }
}
