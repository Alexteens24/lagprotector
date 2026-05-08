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

package net.alexisbinh;

import be.seeseemelk.mockbukkit.MockBukkitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Boots a minimal Bukkit registry so {@link org.bukkit.Material}, {@link org.bukkit.inventory.ItemStack}, etc. work in tests.
 */
@ExtendWith(MockBukkitExtension.class)
public abstract class AbstractMockBukkitTest {}
