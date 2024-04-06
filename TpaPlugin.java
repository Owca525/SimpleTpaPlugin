package org.owcadev.tpaplugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;

import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;

import java.util.*;

public class TpaPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, UUID> teleportRequests = new HashMap<>();
    private final Map<UUID, UUID> multiply = new HashMap<>();
    private FileConfiguration config;

    protected int timer;
    @Override
    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        timer = config.getInt("timer");
    }

    @Override
    public void onDisable() {}

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use these commands!");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("tpa")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.GOLD + "Usage: /tpa <player>");
                return true;
            }

            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found or is offline!");
                return true;
            }

            if (target.getName().equals(player.getName())) {
                player.sendMessage(ChatColor.RED + "You can't teleport to yourself");
                return true;
            }

            multiply.put(player.getUniqueId(), player.getUniqueId());

            teleportRequests.put(target.getUniqueId(), player.getUniqueId());

            TextComponent accept = new TextComponent("/tpaccept");
            accept.setColor(net.md_5.bungee.api.ChatColor.DARK_GREEN);
            accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + player.getName()));
            accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Accept this")));

            TextComponent deny = new TextComponent("/tpadeny");
            deny.setColor(net.md_5.bungee.api.ChatColor.DARK_GREEN);
            deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny" + player.getName()));
            deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Don't accept this")));

            TextComponent finalMessage = new TextComponent(ChatColor.GOLD + "You have " + timer + " seconds to accept teleport player: " + player.getName() + ". Use ");
            finalMessage.addExtra(accept);
            finalMessage.addExtra(ChatColor.GOLD + " or ");
            finalMessage.addExtra(deny);
            finalMessage.addExtra(ChatColor.GOLD + " to respond.");

            getServer().getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    multiply.remove(Objects.requireNonNull(player.getPlayer()).getUniqueId());
                }
            }, timer * 20L);

            target.spigot().sendMessage(finalMessage);
            player.sendMessage(ChatColor.GOLD + "Teleport request sent to " + target.getName() + "!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpaccept")) {
            if (!teleportRequests.containsKey(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You don't have any pending teleport requests!");
                return true;
            }

            Player accepted = getServer().getPlayer(args[0]);

            Player requester = getServer().getPlayer(player.getUniqueId());
            if (requester == null) {
                player.sendMessage(ChatColor.RED + "The player who requested teleportation is no longer online!");
                return true;
            }

            if (multiply.containsKey(accepted.getPlayer().getUniqueId())) {
                accepted.teleport(requester.getLocation());
                multiply.remove(accepted.getPlayer().getUniqueId());
                accepted.sendMessage(ChatColor.GREEN + requester.getName() + " accepted your teleport request!");
            }

            if (multiply.isEmpty()) {
                teleportRequests.remove(player.getUniqueId());
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpadeny")) {
            if (!teleportRequests.containsKey(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You don't have any pending teleport requests!");
                return true;
            }

            Player deny = getServer().getPlayer(args[0]);

            Player requester = getServer().getPlayer(teleportRequests.get(player.getUniqueId()));
            if (requester == null) {
                player.sendMessage(ChatColor.RED + "The player who requested teleportation is no longer online!");
                return true;
            }

            if (multiply.containsKey(deny.getPlayer().getUniqueId())) {
                deny.sendMessage(ChatColor.RED + requester.getName() + " denied your teleport request!");
                multiply.remove(deny.getPlayer().getUniqueId());
            }

            if (multiply.isEmpty()) {
                teleportRequests.remove(player.getUniqueId());
            }
            return true;
        }
        return false;
    }
}