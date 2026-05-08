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
import org.bukkit.entity.Cow;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class IgnoreSettingsTest extends AbstractMockBukkitTest {

    @Test
    void defaultsSkipNoNamedEntity() {
        IgnoreSettings ig = IgnoreSettings.defaults();
        Zombie z = Mockito.mock(Zombie.class);
        when(z.customName()).thenReturn(null);
        when(z.isAdult()).thenReturn(true);
        assertFalse(ig.skipCull(z));
    }

    @Test
    void namedLivingSkippedWhenEnabled() {
        var c = TestFixtures.loadYaml("Ignore:\n  NamedAnimals: true\n");
        IgnoreSettings ig = IgnoreSettings.load(c);
        Zombie z = Mockito.mock(Zombie.class);
        when(z.customName()).thenReturn(net.kyori.adventure.text.Component.text("Bob"));
        assertTrue(ig.skipCull(z));
    }

    @Test
    void loadWithSadleTypoEnablesSaddleIgnore() {
        var c = TestFixtures.loadYaml("Ignore:\n  WithSadle: true\n");
        IgnoreSettings ig = IgnoreSettings.load(c);
        assertTrue(ig.withSaddle());
    }

    @Test
    void ignoresWorldCaseInsensitive() {
        var c = TestFixtures.loadYaml("Ignore:\n  Worlds:\n    - MyWorld\n");
        IgnoreSettings ig = IgnoreSettings.load(c);
        assertTrue(ig.ignoresWorld("myworld"));
    }

    @Test
    void itemFrameWithItemSkipped() {
        IgnoreSettings ig = IgnoreSettings.load(TestFixtures.loadYaml("Ignore:\n  FullItemFrames: true\n"));
        ItemFrame frame = Mockito.mock(ItemFrame.class);
        ItemStack stack = new ItemStack(Material.STICK);
        when(frame.getItem()).thenReturn(stack);
        assertTrue(ig.skipCull(frame));
    }

    @Test
    void itemFrameEmptyNotSkippedByFrameRule() {
        IgnoreSettings ig = IgnoreSettings.load(TestFixtures.loadYaml("Ignore:\n  FullItemFrames: true\n"));
        ItemFrame frame = Mockito.mock(ItemFrame.class);
        when(frame.getItem()).thenReturn(null);
        assertFalse(ig.skipCull(frame));
    }

    @Test
    void pickedItemInMainHandSkips() {
        IgnoreSettings ig = IgnoreSettings.load(TestFixtures.loadYaml("Ignore:\n  EntityWithPickedItem: true\n"));
        Cow cow = Mockito.mock(Cow.class);
        EntityEquipment eq = Mockito.mock(EntityEquipment.class);
        when(cow.isAdult()).thenReturn(true);
        when(cow.getEquipment()).thenReturn(eq);
        when(eq.getItemInMainHand()).thenReturn(new ItemStack(Material.DIAMOND));
        assertTrue(ig.skipCull(cow));
    }

    @Test
    void pickedItemDisabledDoesNotSkip() {
        IgnoreSettings ig = IgnoreSettings.load(TestFixtures.loadYaml("Ignore:\n  EntityWithPickedItem: false\n"));
        Cow cow = Mockito.mock(Cow.class);
        EntityEquipment eq = Mockito.mock(EntityEquipment.class);
        when(cow.isAdult()).thenReturn(true);
        when(cow.getEquipment()).thenReturn(eq);
        when(eq.getItemInMainHand()).thenReturn(new ItemStack(Material.DIAMOND));
        assertFalse(ig.skipCull(cow));
    }
}
