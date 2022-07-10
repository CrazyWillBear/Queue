package net.capbear.queue;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.event.EventHandler;

import java.util.Vector;
import java.util.concurrent.TimeUnit;

public final class Queue extends Plugin implements Listener {
    private static final String queueServerName = "hub"; // replace hub as needed
    private static final String destinationServerName = "survival"; // replace survival as needed
    private static final int maxPlayersDestinationServer = 0;
    Vector<ProxiedPlayer> queue;

    @Override
    public void onEnable() {
        // Plugin startup logic
        queue = new Vector<ProxiedPlayer>();

        getProxy().getPluginManager().registerListener(this, this);

        TaskScheduler scheduler = getProxy().getScheduler();
        scheduler.schedule(this, new Runnable() {
            @Override
            public void run() {
                System.out.println("Players on " + destinationServerName + ": " + getProxy().getServerInfo(destinationServerName).getPlayers().size() + "\t|\tQueue Size: " + queue.size());
                for (ProxiedPlayer player : getProxy().getServerInfo(queueServerName).getPlayers()) {
                    player.sendMessage("§3You are currently in position " + (queue.indexOf(player) + 1) + "/" + queue.size());
                }
                if (getProxy().getServerInfo(destinationServerName).getPlayers().size() < maxPlayersDestinationServer && queue.size() > 0) {
                    ServerInfo destination = getProxy().getServerInfo(destinationServerName);
                    queue.get(0).connect(destination);
                    queue.remove(0);
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        // this assumes player is forced to join queue server
        queue.add(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        System.out.println("Player left server " + event.getPlayer().getServer().getInfo().getName());
        if (event.getPlayer().getServer().getInfo().getName().equals(queueServerName)) {
            queue.remove(event.getPlayer());
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
