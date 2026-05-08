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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Simple token-bucket style throttle for debug / cancel spam.
 */
public final class ThrottledLog {

    private final JavaPlugin plugin;
    private final AtomicLong windowStartMs = new AtomicLong(0L);
    private final AtomicInteger countInWindow = new AtomicInteger(0);
    private final int maxPerWindow;
    private final long windowMs;

    public ThrottledLog(@NotNull JavaPlugin plugin, int maxPerWindow, long windowMs) {
        this.plugin = plugin;
        this.maxPerWindow = Math.max(1, maxPerWindow);
        this.windowMs = Math.max(1L, windowMs);
    }

    public void debug(@NotNull String message) {
        if (!plugin.getConfig().getBoolean("debug")) {
            return;
        }
        long now = System.currentTimeMillis();
        long start = windowStartMs.get();
        if (now - start >= windowMs) {
            if (windowStartMs.compareAndSet(start, now)) {
                countInWindow.set(0);
            }
        }
        int n = countInWindow.incrementAndGet();
        if (n <= maxPerWindow) {
            plugin.getLogger().info("[debug] " + message);
        } else if (n == maxPerWindow + 1) {
            plugin.getLogger().info("[debug] ... further similar messages throttled");
        }
    }
}
