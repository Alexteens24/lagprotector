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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkGroupUtilTest {

    @Test
    void radiusZeroSingleChunk() {
        World world = Mockito.mock(World.class);
        Chunk only = Mockito.mock(Chunk.class);
        when(world.getChunkAt(10, -3)).thenReturn(only);

        List<Chunk> chunks = ChunkGroupUtil.chunksInGroup(world, 10, -3, 0);
        assertEquals(1, chunks.size());
        assertEquals(only, chunks.getFirst());
        verify(world).getChunkAt(10, -3);
    }

    @Test
    void radiusOneIsThreeByThree() {
        World world = Mockito.mock(World.class);
        when(world.getChunkAt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenAnswer(inv -> {
            int x = inv.getArgument(0);
            int z = inv.getArgument(1);
            Chunk ch = Mockito.mock(Chunk.class);
            when(ch.getX()).thenReturn(x);
            when(ch.getZ()).thenReturn(z);
            return ch;
        });

        List<Chunk> chunks = ChunkGroupUtil.chunksInGroup(world, 5, 5, 1);
        assertEquals(9, chunks.size());
        Set<String> keys = new HashSet<>();
        for (Chunk c : chunks) {
            keys.add(c.getX() + "," + c.getZ());
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                assertEquals(true, keys.contains((5 + dx) + "," + (5 + dz)));
            }
        }
    }

    @Test
    void radiusClampedToMaxThree() {
        World world = Mockito.mock(World.class);
        when(world.getChunkAt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenAnswer(inv -> Mockito.mock(Chunk.class));

        List<Chunk> huge = ChunkGroupUtil.chunksInGroup(world, 0, 0, 99);
        List<Chunk> max = ChunkGroupUtil.chunksInGroup(world, 0, 0, 3);
        assertEquals(max.size(), huge.size());
        assertEquals(7 * 7, max.size());
    }
}
