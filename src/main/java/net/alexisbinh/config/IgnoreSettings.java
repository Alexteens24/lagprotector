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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional protections so cull does not strip player-valued entities ({@code Ignore.*}).
 */
public record IgnoreSettings(
        @NotNull Set<String> worldsLower,
        boolean namedAnimals,
        boolean tamedAnimals,
        boolean babyAnimals,
        boolean fullArmorStands,
        boolean fullItemFrames,
        boolean entityWithPickedItem,
        boolean withSaddle
) {

    public static @NotNull IgnoreSettings load(@NotNull FileConfiguration c) {
        ConfigurationSection s = ConfigUtil.sectionCI(c, "Ignore");
        if (s == null) {
            return defaults();
        }
        List<String> worlds = s.getStringList("Worlds");
        if (worlds.isEmpty()) {
            worlds = s.getStringList("worlds");
        }
        Set<String> wl = new HashSet<>();
        for (String w : worlds) {
            wl.add(w.toLowerCase(Locale.ROOT));
        }
        return new IgnoreSettings(
                Collections.unmodifiableSet(wl),
                s.getBoolean("NamedAnimals", true),
                s.getBoolean("TamedAnimals", true),
                s.getBoolean("BabyAnimals", true),
                s.getBoolean("FullArmorStands", true),
                s.getBoolean("FullItemFrames", true),
                s.getBoolean("EntityWithPickedItem", true),
                s.getBoolean("WithSadle", true) || s.getBoolean("WithSaddle", true)
        );
    }

    public static @NotNull IgnoreSettings defaults() {
        return new IgnoreSettings(Set.of(), true, true, true, true, true, true, true);
    }

    public boolean ignoresWorld(@NotNull String worldName) {
        return worldsLower.contains(worldName.toLowerCase(Locale.ROOT));
    }

    /**
     * @return true if this entity must not be removed by cull.
     */
    public boolean skipCull(@NotNull Entity entity) {
        if (entity instanceof LivingEntity living) {
            if (tamedAnimals && living instanceof Tameable t && t.isTamed()) {
                return true;
            }
            if (namedAnimals && living.customName() != null) {
                return true;
            }
            if (babyAnimals && living instanceof Ageable a && !a.isAdult()) {
                return true;
            }
            if (withSaddle && living instanceof org.bukkit.entity.AbstractHorse horse) {
                ItemStack saddle = horse.getInventory().getSaddle();
                if (saddle != null && saddle.getType().isItem()) {
                    return true;
                }
            }
            if (entityWithPickedItem) {
                EntityEquipment eq = living.getEquipment();
                if (eq != null) {
                    ItemStack h = eq.getItemInMainHand();
                    if (h != null && h.getType().isItem() && h.getAmount() > 0) {
                        return true;
                    }
                }
            }
        }
        if (fullArmorStands && entity instanceof ArmorStand stand && armorStandHasItems(stand)) {
            return true;
        }
        if (fullItemFrames && entity instanceof ItemFrame frame) {
            ItemStack it = frame.getItem();
            if (it != null && it.getType().isItem() && it.getAmount() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean armorStandHasItems(@NotNull ArmorStand stand) {
        EntityEquipment eq = stand.getEquipment();
        if (eq == null) {
            return false;
        }
        return anyItem(eq.getHelmet()) || anyItem(eq.getChestplate()) || anyItem(eq.getLeggings()) || anyItem(eq.getBoots())
                || anyItem(eq.getItemInMainHand()) || anyItem(eq.getItemInOffHand());
    }

    private static boolean anyItem(@Nullable ItemStack stack) {
        return stack != null && stack.getType().isItem() && stack.getAmount() > 0;
    }
}
