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

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EntityCategories {

    private EntityCategories() {}

    public static boolean isMobCapTarget(@NotNull EntityType type) {
        Class<? extends Entity> clazz = type.getEntityClass();
        if (clazz == null) {
            return false;
        }
        if (!LivingEntity.class.isAssignableFrom(clazz)) {
            return false;
        }
        return !Player.class.isAssignableFrom(clazz);
    }

    public static boolean isMobCapTarget(@NotNull Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player);
    }
}
