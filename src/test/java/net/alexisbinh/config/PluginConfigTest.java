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

import net.alexisbinh.AbstractMockBukkitTest;
import net.alexisbinh.TestFixtures;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class PluginConfigTest extends AbstractMockBukkitTest {

    /**
     * Non-empty {@code BlockTypes.List} avoids {@link MechanicsMaterials} static init, which iterates
     * {@link Material#values()} and is not supported under MockBukkit.
     */
    private static final String BLOCK_TYPES_STUB = """
            BlockTypes:
              Limit: true
              List:
                HOPPER: 16
            """;

    @Test
    void entitySpawnCheckCanBeDisabled() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 10
                EntitySpawn:
                  Check: false
                """;
        assertFalse(reload(yaml).mobs().entitySpawnCheck());
    }

    @Test
    void fallbackNonLivingLimitAppliesToItemFrame() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 10
                  FallbackNonLivingLimit: 4
                EntitySpawn:
                  Check: true
                EntityTypes:
                  PLAYER:
                    Use: true
                    Limit: -1
                """;
        PluginConfig cfg = reload(yaml);
        assertEquals(4, cfg.mobs().limit(EntityType.ITEM_FRAME));
    }

    @Test
    void blockTypesPistonMoveFlagLoaded() {
        String yaml = """
                BlockTypes:
                  Limit: true
                  CheckPistonMove: true
                  List:
                    HOPPER: 8
                Mobs:
                  DefaultPerType: 10
                EntitySpawn:
                  Check: true
                """;
        assertTrue(reload(yaml).mechanics().checkPistonMove());
    }

    @Test
    void naturalSpawnFalseOnExplicitMobRule() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 50
                EntityTypes:
                  ZOMBIE:
                    Use: true
                    Limit: 10
                    AutoClean: true
                    NaturalSpawn: false
                EntitySpawn:
                  Check: true
                """;
        assertFalse(reload(yaml).mobs().naturalSpawn(EntityType.ZOMBIE));
    }

    @Test
    void defaultPerTypeAppliesToMobWithoutEntityTypesRow() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 11
                EntitySpawn:
                  Check: true
                """;
        PluginConfig cfg = reload(yaml);
        assertEquals(11, cfg.mobs().limit(EntityType.ZOMBIE));
        assertTrue(cfg.mobs().autoClean(EntityType.ZOMBIE));
    }

    @Test
    void explicitEntityTypeOverridesDefault() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 50
                EntityTypes:
                  ZOMBIE:
                    Use: true
                    Limit: 3
                    AutoClean: true
                    NaturalSpawn: true
                EntitySpawn:
                  Check: true
                """;
        PluginConfig cfg = reload(yaml);
        assertEquals(3, cfg.mobs().limit(EntityType.ZOMBIE));
    }

    @Test
    void spawnReasonFalseBlocks() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 10
                SpawnReasons:
                  BREEDING: false
                EntitySpawn:
                  Check: true
                """;
        PluginConfig cfg = reload(yaml);
        assertFalse(cfg.mobs().spawnReasonAllowed(SpawnReason.BREEDING));
        assertTrue(cfg.mobs().spawnReasonAllowed(SpawnReason.NATURAL));
    }

    @Test
    void excludedChunkRecognized() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 10
                ExcludedChunks:
                  - "world_nether:4:-7"
                EntitySpawn:
                  Check: true
                """;
        PluginConfig cfg = reload(yaml);
        World nether = Mockito.mock(World.class);
        when(nether.getName()).thenReturn("world_nether");
        assertTrue(cfg.mobs().isChunkExcluded(nether, 4, -7));
        assertFalse(cfg.mobs().isChunkExcluded(nether, 4, -6));
    }

    @Test
    void autocleanMasterSwitchOffDisablesMobCull() {
        String yaml = BLOCK_TYPES_STUB + """
                AutoClean:
                  Enabled: false
                  Mobs:
                    Use: true
                    IntervalSeconds: 10
                Mobs:
                  DefaultPerType: 10
                EntitySpawn:
                  Check: true
                """;
        PluginConfig cfg = reload(yaml);
        assertFalse(cfg.mobs().cull().enabled());
    }

    @Test
    void groupChunksRadiusMapsToChebyshevRadius() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  DefaultPerType: 10
                EntitySpawn:
                  Check: true
                  GroupChunks: true
                  GroupChunksRadius: 3
                """;
        PluginConfig cfg = reload(yaml);
        assertTrue(cfg.mobs().groupChunks());
        assertEquals(2, cfg.mobs().groupChunkRadius());
    }

    @Test
    void mechanicsHopperLimitFromBlockTypesList() {
        String yaml = """
                Mobs:
                  WorldsAllow: []
                BlockTypes:
                  Limit: true
                  List:
                    HOPPER: 9
                EntityTypes:
                  PLAYER:
                    Use: true
                    Limit: -1
                """;
        PluginConfig cfg = reload(yaml);
        assertEquals(9, cfg.mechanics().limit(Material.HOPPER));
        assertTrue(cfg.mechanics().enabled());
    }

    @Test
    void mechanicsTracksHopperMinecartFromEntityTypes() {
        String yaml = BLOCK_TYPES_STUB + """
                Mobs:
                  WorldsAllow: []
                EntityTypes:
                  PLAYER:
                    Use: true
                    Limit: -1
                  HOPPER_MINECART:
                    Use: true
                    Limit: 2
                    AutoClean: true
                    NaturalSpawn: true
                """;
        PluginConfig cfg = reload(yaml);
        assertTrue(cfg.mechanics().tracksEntityType(EntityType.HOPPER_MINECART));
        assertEquals(2, cfg.mechanics().mechanicsEntityLimit(EntityType.HOPPER_MINECART));
    }

    @Test
    void logSettingsDefaultsWhenMissing() {
        PluginConfig cfg = reload(BLOCK_TYPES_STUB + "Mobs:\n  DefaultPerType: 10\nEntitySpawn:\n  Check: true\n");
        assertFalse(cfg.log().enabled());
        assertEquals(3, cfg.log().minRemoved());
    }

    @Test
    void debugFlagLoaded() {
        PluginConfig cfg = reload(BLOCK_TYPES_STUB + "debug: true\nMobs:\n  DefaultPerType: 1\nEntitySpawn:\n  Check: true\n");
        assertTrue(cfg.debug());
    }

    private static PluginConfig reload(String yaml) {
        PluginConfig cfg = new PluginConfig();
        cfg.reload(TestFixtures.pluginWithConfig(TestFixtures.loadYaml(yaml)));
        return cfg;
    }
}
