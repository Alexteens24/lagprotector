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

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.Locale;
import net.alexisbinh.config.PluginConfig;
import net.alexisbinh.config.PluginMessages;
import net.alexisbinh.cull.ChunkCullCoordinator;
import net.alexisbinh.listener.EntitySpawnLimiter;
import net.alexisbinh.listener.MechanicsBlockListener;
import net.alexisbinh.listener.MechanicsPistonListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LagProtectorPlugin extends JavaPlugin {

    private static final int PLUGIN_METRICS_ID = 31208;

    private PluginConfig pluginConfig;
    private final PluginMessages pluginMessages = new PluginMessages();
    private ChunkCullCoordinator cullCoordinator;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginMessages.reload(this);
        this.pluginConfig = new PluginConfig();
        this.pluginConfig.reload(this);

        new Metrics(this, PLUGIN_METRICS_ID);

        this.cullCoordinator = new ChunkCullCoordinator(this, pluginConfig);
        this.cullCoordinator.start();

        getServer().getPluginManager().registerEvents(new EntitySpawnLimiter(this), this);
        getServer().getPluginManager().registerEvents(new MechanicsBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new MechanicsPistonListener(this), this);

        registerCommand("lagprotector", new LagProtectorPaperCommand());
    }

    @Override
    public void onDisable() {
        if (cullCoordinator != null) {
            cullCoordinator.stop();
        }
    }

    private final class LagProtectorPaperCommand implements BasicCommand {

        @Override
        public void execute(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {
            CommandSender sender = stack.getSender();
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("lagprotector.reload")) {
                    sender.sendMessage(pluginMessages.commandNoPermission());
                    return;
                }
                reloadPluginConfig();
                sender.sendMessage(pluginMessages.commandReloadSuccess());
                return;
            }
            sender.sendMessage(pluginMessages.commandUsage());
        }

        @Override
        public @Nullable List<String> suggest(@NotNull CommandSourceStack source, @NotNull String @NotNull [] args) {
            if (args.length == 0 || args.length == 1 && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                return List.of("reload");
            }
            return List.of();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        pluginMessages.reload(this);
        pluginConfig.reload(this);
        cullCoordinator.restart();
    }

    public @NotNull PluginConfig configState() {
        return pluginConfig;
    }

    public @NotNull PluginMessages messages() {
        return pluginMessages;
    }
}
