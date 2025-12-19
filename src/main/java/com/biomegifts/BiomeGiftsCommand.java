package com.biomegifts;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BiomeGiftsCommand implements CommandExecutor, TabCompleter {

    private final BiomeGifts plugin;
    private final List<String> itemKeys;

    public BiomeGiftsCommand(BiomeGifts plugin) {
        this.plugin = plugin;
        this.itemKeys = plugin.getItemManager().getItemKeys();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此指令。");
            return true;
        }

        if (!sender.hasPermission("biomegifts.admin")) {
            sender.sendMessage("§c你没有权限使用此指令。");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c用法: /getgift <物品ID>");
            return true;
        }

        String key = args[0].toUpperCase();

        Player player = (Player) sender;

        if (key.equals("ALL")) {
            int count = 0;
            for (String itemKey : plugin.getItemManager().getItemKeys()) {
                ItemStack item = plugin.getItemManager().getItem(itemKey);
                if (item != null) {
                    player.getInventory().addItem(item);
                    count++;
                }
            }
            sender.sendMessage("§a已获取所有地域馈赠物品 (共 " + count + " 种)。");
            return true;
        }

        ItemStack item = plugin.getItemManager().getItem(key);

        if (item == null) {
            sender.sendMessage("§c未找到物品: " + key);
            return true;
        }

        player.getInventory().addItem(item);
        sender.sendMessage("§a已获取物品: " + key);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(itemKeys);
            completions.add("ALL");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
