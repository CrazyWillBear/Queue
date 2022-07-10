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
import net.md_5.bungee.event.EventHandler;

import java.util.Vector;
import java.util.concurrent.TimeUnit;

public final class Queue extends Plugin implements Listener {
    // configurable variables
    private static final String queueServerName = "hub"; // replace with the name of your queue server
    private static final String destinationServerName = "survival"; // replace with the name of your destination server
    private static final int maxPlayersDestinationServer = 100; // replace with the maximum number of players allowed on your destination server

    Vector<ProxiedPlayer> queue; // non-configurable, leave as-is
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
                    ComponentBuilder builder = new ComponentBuilder();
                    builder.append("You are currently in position " + (queue.indexOf(player) + 1) + "/" + (queue.size())).color(ChatColor.AQUA);
                    Title title = ProxyServer.getInstance().createTitle().title(builder.create()).fadeOut(100).fadeIn(0);
                    title.send(player);
                } // send each player a title containing queue info
                if (getProxy().getServerInfo(destinationServerName).getPlayers().size() < maxPlayersDestinationServer && queue.size() > 0) {
                    ServerInfo destination = getProxy().getServerInfo(destinationServerName);
                    queue.get(0).connect(destination);
                    queue.remove(0);
                } // if at front of line and slot is open, send to main server
            }
        }, 5, 5, TimeUnit.SECONDS); // repeat forever every 5 seconds
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
