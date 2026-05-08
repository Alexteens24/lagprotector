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

import org.bukkit.entity.EntityType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityCategoriesTest {

    @Test
    void zombieIsMobCapTarget() {
        assertTrue(EntityCategories.isMobCapTarget(EntityType.ZOMBIE));
    }

    @Test
    void playerNotMobCapTargetByType() {
        assertFalse(EntityCategories.isMobCapTarget(EntityType.PLAYER));
    }

    @Test
    void itemFrameNotMobCapTarget() {
        assertFalse(EntityCategories.isMobCapTarget(EntityType.ITEM_FRAME));
    }

    @Test
    void armorStandCountsAsMobCapTarget() {
        assertTrue(EntityCategories.isMobCapTarget(EntityType.ARMOR_STAND));
    }

    @Test
    void entityInstanceZombie() {
        Zombie z = Mockito.mock(Zombie.class);
        Mockito.when(z.getType()).thenReturn(EntityType.ZOMBIE);
        assertTrue(EntityCategories.isMobCapTarget(z));
    }

    @Test
    void entityInstancePlayer() {
        Player p = Mockito.mock(Player.class);
        Mockito.when(p.getType()).thenReturn(EntityType.PLAYER);
        assertFalse(EntityCategories.isMobCapTarget(p));
    }

    @Test
    void entityInstanceArmorStand() {
        ArmorStand as = Mockito.mock(ArmorStand.class);
        Mockito.when(as.getType()).thenReturn(EntityType.ARMOR_STAND);
        assertTrue(EntityCategories.isMobCapTarget(as));
    }
}
