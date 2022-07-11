// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package net.capbear.queue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public final class Queue extends Plugin implements Listener {
    // init variables
    String queueServerName;
    String destinationServerName;
    int maxPlayersDestinationServer;
    Vector<ProxiedPlayer> queue; // non-configurable, leave as-is
    @Override
    public void onEnable() {
        // Plugin startup logic
        queue = new Vector<ProxiedPlayer>();

        getProxy().getPluginManager().registerListener(this, this);

        TaskScheduler scheduler = getProxy().getScheduler();

        Configuration configuration = new Configuration();

        if (!getDataFolder().exists()) { getDataFolder().mkdir(); }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configuration.set("queue-server", "hub");
            configuration.set("dest-server", "survival");
            configuration.set("max-players", 100);

            try { ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, new File(getDataFolder(), "config.yml")); }
            catch (IOException e) { throw new RuntimeException(e); }
        }

        try { configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml")); }
        catch (IOException e) { }

        String queueServerName = (String) configuration.get("queue-server");
        String destinationServerName = (String) configuration.get("dest-server");
        int maxPlayersDestinationServer = (int) configuration.get("max-players");
        scheduler.schedule(this, new Runnable() {
            @Override
            public void run() {
                System.out.println("Players on " + destinationServerName + ": " + getProxy().getServerInfo(destinationServerName).getPlayers().size() + "\t|\tQueue Size: " + queue.size());
                if (queue.size() > 0) {
                    if (queue.get(0).getServer().getInfo() == getProxy().getServerInfo(destinationServerName)) {
                        queue.remove(0);
                    }
                } // nested if statement to avoid IndexOutofRange error
                for (ProxiedPlayer player : getProxy().getServerInfo(queueServerName).getPlayers()) {
                    ComponentBuilder builder = new ComponentBuilder();
                    builder.append("You are currently in position " + (queue.indexOf(player) + 1) + "/" + (queue.size())).color(ChatColor.AQUA);
                    Title title = ProxyServer.getInstance().createTitle().title(builder.create()).fadeOut(25).fadeIn(0);
                    title.send(player);
                } // send each player a title containing queue info
                if (getProxy().getServerInfo(destinationServerName).getPlayers().size() < maxPlayersDestinationServer && queue.size() > 0) {
                    ServerInfo destination = getProxy().getServerInfo(destinationServerName);
                    queue.get(0).connect(destination);
                } // if at front of line and slot is open, send to main server
            }
        }, 1, 1, TimeUnit.SECONDS); // repeat forever every 5 seconds
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        // this assumes player is forced to join queue server
        queue.add(event.getPlayer());
    } // add player to queue upon login

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event.getPlayer().getServer().getInfo().getName().equals(queueServerName)) {
            queue.remove(event.getPlayer());
        }
    } // remove player from queue if they disconnect from queue server

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
