package com.worldcretornica.lilylistme;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.event.EventListener;
import lilypad.client.connect.api.event.MessageEvent;
import lilypad.client.connect.api.request.impl.MessageRequest;
import lilypad.client.connect.api.result.FutureResultListener;
import lilypad.client.connect.api.result.StatusCode;
import lilypad.client.connect.api.result.impl.MessageResult;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.vanish.VanishPlugin;
import org.kitteh.vanish.event.VanishStatusChangeEvent;

public class LilyListMe extends JavaPlugin implements Listener {

    public ConcurrentHashMap<String, ConcurrentHashMap<SortedPlayer, Boolean>> playerServers;

    private final static String channelname = "wcListMe";

    private Chat chat = null;
    private Permission permission = null;

    private String servername = "";

    private Plugin vanishplugin = null;

    @Override
    public void onEnable() {

        playerServers = new ConcurrentHashMap<>();
        setupChat();
        setupPermission();

        this.getServer().getPluginManager().registerEvents(this, this);
        getConnect().registerEvents(this);

        if (this.getServer().getPluginManager().isPluginEnabled("LillyConnect")) {
            servername = this.getServer().getPluginManager().getPlugin("LillyConnect").getConfig().getString("Item Name");
        } else {
            servername = "Server";
        }

        if (this.getServer().getPluginManager().isPluginEnabled("VanishNoPacket")) {
            vanishplugin = this.getServer().getPluginManager().getPlugin("VanishNoPacket");
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                broadcast(servername, "CLEAR");

                if (playerServers != null) {
                    if(playerServers.get(servername) != null) {
                        playerServers.get(servername).clear();
                    }
                }

                for (Player p : Bukkit.getOnlinePlayers()) {

                    String name = p.getName();
                    String color = getColor(p);
                    int rank = getPlayerRank(p);
                    String server = servername;

                    boolean invisible = false;

                    if (vanishplugin != null) {
                        invisible = ((VanishPlugin) vanishplugin).getManager().isVanished(name);
                    }

                    addPlayer(name, rank, color, server, invisible);
                }

                if (playerServers != null) {
                    if (playerServers.get(servername) != null) {
                        if (playerServers.get(servername).size() > 0) {
                            for (SortedPlayer p : playerServers.get(servername).keySet()) {
                                broadcast(p.name, p.rank, p.color, servername, "LOGIN", p.invisible);
                            }
                        }
                    }
                }
            }
        }, 20 * 60 * 3, 20 * 60 * 10); // start after 3 minute, repeat each 5
                                       // minutes
    }

    @Override
    public void onDisable() {
        playerServers.clear();
        playerServers = null;
        getConnect().unregisterEvents(this);
        permission = null;
        chat = null;
        playerServers = null;
    }

    private void setupChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (chatProvider != null)
            chat = chatProvider.getProvider();
    }

    private void setupPermission() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null)
            permission = permissionProvider.getProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("list")) {
            SortedMap<Integer, SortedSet<String>> playerlist = new TreeMap<>();

            for (ConcurrentHashMap<SortedPlayer, Boolean> servers : playerServers.values()) {
                for (SortedPlayer sp : servers.keySet()) {
                    if (!sp.invisible || sender.hasPermission("vanish.see")) {
                        if (!playerlist.containsKey(sp.rank)) {
                            playerlist.put(sp.rank, new TreeSet<String>());
                        }

                        if(sp.invisible) {
                            playerlist.get(sp.rank).add(ChatColor.WHITE + "?" + sp.color + sp.name);
                        } else {
                            playerlist.get(sp.rank).add(sp.color + sp.name);
                        }
                    }
                }
            }

            String output = ChatColor.YELLOW + "Online players : " + ChatColor.RESET;

            if (playerlist.size() == 0) {
                output += ChatColor.YELLOW + "N/A";
            } else {
                while (playerlist.size() > 0) {
                    for (String player : playerlist.get(playerlist.firstKey())) {
                        output += player + ", ";
                    }
                    playerlist.remove(playerlist.firstKey());
                }

                output = output.substring(0, output.length() - 2);
            }

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', output));

            return true;

        } else if (cmd.equals("who")) {

            if (playerServers.size() == 0) {
                sender.sendMessage(ChatColor.YELLOW + "Online players : N/A");
            } else {

                sender.sendMessage(ChatColor.GREEN + StringUtils.repeat("=", 52));
                String buffer = "";
                
                for (String server : playerServers.keySet()) {

                    String output = "" + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', server)) + ": ";

                    SortedMap<Integer, SortedSet<String>> playerlist = new TreeMap<>();

                    for (SortedPlayer sp : playerServers.get(server).keySet()) {
                        if (!sp.invisible || sender.hasPermission("vanish.see")) {
                            if (!playerlist.containsKey(sp.rank)) {
                                playerlist.put(sp.rank, new TreeSet<String>());
                            }

                            if(sp.invisible) {
                                playerlist.get(sp.rank).add(ChatColor.WHITE + "?" + sp.color + sp.name);
                            } else {
                                playerlist.get(sp.rank).add(sp.color + sp.name);
                            }
                        }
                    }

                    if (playerlist.size() > 0) {
                        if(!buffer.equals(""))
                        {
                            sender.sendMessage(buffer);
                            buffer = "";
                        }
                        
                        while (playerlist.size() > 0) {
                            for (String player : playerlist.get(playerlist.firstKey())) {
                                output += player + ", ";
                            }
                            playerlist.remove(playerlist.firstKey());
                        }

                        output = output.substring(0, output.length() - 2);
                        
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', output));
                        buffer = ChatColor.GREEN + StringUtils.repeat("-", 52);
                    }
                }
                
                sender.sendMessage(ChatColor.GREEN + StringUtils.repeat("=", 52));
            }
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(final PlayerLoginEvent e) {

        Player p = e.getPlayer();
        String name = p.getName();
        String color = getColor(p);
        int rank = getPlayerRank(p);
        String server = servername;

        boolean invisible = false;

        if (vanishplugin != null) {
            invisible = ((VanishPlugin) vanishplugin).getManager().isVanished(name);
        }

        addPlayer(name, rank, color, server, invisible);
        broadcast(name, rank, color, server, "LOGIN", invisible);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerLogout(final PlayerQuitEvent e) {

        Player p = e.getPlayer();
        String name = p.getName();
        String color = getColor(p);
        int rank = getPlayerRank(p);
        String server = servername;

        boolean invisible = false;

        if (vanishplugin != null) {
            invisible = ((VanishPlugin) vanishplugin).getManager().isVanished(name);
        }

        removePlayer(name, rank, color, server, invisible);
        broadcast(name, rank, color, server, "LOGOUT", invisible);
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerStatusChange(final VanishStatusChangeEvent event) {
        
        Player p = event.getPlayer();
        String name = p.getName();
        String color = getColor(p);
        int rank = getPlayerRank(p);
        String server = servername;

        boolean invisible = event.isVanishing();

        addPlayer(name, rank, color, server, invisible);
        broadcast(name, rank, color, server, "LOGIN", invisible);
    }

    @EventListener
    public void onMessage(MessageEvent event) {
        if (event.getChannel().equals(channelname)) {
            try {
                String[] tokens;

                tokens = event.getMessageAsString().split(";");

                String action = tokens[0];
                String name;
                int rank;
                String server;
                String color;
                boolean invisible;

                switch (action) {
                case "CLEAR":
                    if (tokens.length >= 2) {
                        server = tokens[1];

                        if (playerServers != null) {
                            if (playerServers.contains(server)) {
                                playerServers.get(server).clear();
                            }
                        }
                    }
                    break;
                case "LOGIN":
                    if (tokens.length >= 5) {
                        name = tokens[1];
                        try {
                            rank = Integer.parseInt(tokens[2]);
                        } catch (NumberFormatException e) {
                            rank = 9999;
                        }
                        server = tokens[3];
                        color = tokens[4];
                        if (tokens.length >= 6) {
                            invisible = Boolean.parseBoolean(tokens[5]);
                        } else {
                            invisible = false;
                        }

                        addPlayer(name, rank, color, server, invisible);
                    }
                    break;
                case "LOGOUT":
                    if (tokens.length >= 5) {
                        name = tokens[1];
                        try {
                            rank = Integer.parseInt(tokens[2]);
                        } catch (NumberFormatException e) {
                            rank = 9999;
                        }
                        server = tokens[3];
                        color = tokens[4];
                        if (tokens.length >= 6) {
                            invisible = Boolean.parseBoolean(tokens[5]);
                        } else {
                            invisible = false;
                        }

                        removePlayer(name, rank, color, server, invisible);
                    }
                    break;
                }

            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }

    }

    private void addPlayer(String name, int rank, String color, String server, boolean invisible) {
        if (!playerServers.containsKey(server)) {
            playerServers.putIfAbsent(server, new ConcurrentHashMap<SortedPlayer, Boolean>());
        }

        for (Iterator<SortedPlayer> i = playerServers.get(server).keySet().iterator(); i.hasNext();) {
            SortedPlayer next = i.next();
            if (next.name.equals(name)) {
                playerServers.get(server).remove(next);
            }
        }

        SortedPlayer sp = new SortedPlayer(name, rank, color, invisible);

        playerServers.get(server).putIfAbsent(sp, true);
    }

    private void removePlayer(String name, int rank, String color, String server, boolean invisible) {
        if (!playerServers.containsKey(server)) {
            playerServers.putIfAbsent(server, new ConcurrentHashMap<SortedPlayer, Boolean>());
        }

        for (Iterator<SortedPlayer> i = playerServers.get(server).keySet().iterator(); i.hasNext();) {
            SortedPlayer next = i.next();
            if (next.name.equals(name)) {
                playerServers.get(server).remove(next);
            }
        }
    }

    private String getColor(Player p) {
        String group = permission.getPrimaryGroup(p);
        String prefix = chat.getGroupPrefix(p.getWorld(), group);

        int lastcolor = prefix.lastIndexOf("&");
        if (lastcolor >= 0) {
            return "&" + prefix.charAt(lastcolor + 1);
        }

        return "";
    }

    private Connect getConnect() {
        return super.getServer().getServicesManager().getRegistration(Connect.class).getProvider();
    }

    private void broadcast(final String name, final int rank, final String color, final String server, final String msg, final boolean invisible) {
        try {
            Connect connect = getConnect();

            connect.request(new MessageRequest("", channelname, msg + ";" + name.replace(";", "") + ";" + rank + ";" + server.replace(";", "") + ";" + color.replace(";", "") + ";" + invisible)).registerListener(new FutureResultListener<MessageResult>() {
                public void onResult(MessageResult redirectResult) {
                    if (redirectResult.getStatusCode() == StatusCode.SUCCESS) {
                        return;
                    }
                }
            });

        } catch (Exception e) {
        }
    }

    private void broadcast(final String server, final String msg) {
        try {
            Connect connect = getConnect();

            connect.request(new MessageRequest("", channelname, msg + ";" + server.replace(";", ""))).registerListener(new FutureResultListener<MessageResult>() {
                public void onResult(MessageResult redirectResult) {
                    if (redirectResult.getStatusCode() == StatusCode.SUCCESS) {
                        return;
                    }
                }
            });

        } catch (Exception e) {
        }
    }

    private int getPlayerRank(Player player) {

        int rank = 0;

        for (int i = 40; i > 0; i--) {
            if (player.hasPermission("rank." + i)) {
                rank = i;
            }
        }

        if (rank != 0) {
            return rank;
        } else {
            return 9999;
        }
    }
}
