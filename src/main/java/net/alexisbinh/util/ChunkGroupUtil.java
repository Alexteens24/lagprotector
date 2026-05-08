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

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class ChunkGroupUtil {

    private ChunkGroupUtil() {}

    /**
     * Chunks whose Chebyshev distance from ({@code chunkX}, {@code chunkZ}) is at most {@code chebyshevRadius}
     * (0 = single chunk, 1 = 3×3, 2 = 5×5). YAML uses {@code configuredRadius = chebyshevRadius + 1}.
     */
    public static @NotNull List<Chunk> chunksInGroup(@NotNull World world, int chunkX, int chunkZ, int radius) {
        int r = Math.max(0, Math.min(radius, 3));
        List<Chunk> out = new ArrayList<>((2 * r + 1) * (2 * r + 1));
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                out.add(world.getChunkAt(chunkX + dx, chunkZ + dz));
            }
        }
        return out;
    }
}
