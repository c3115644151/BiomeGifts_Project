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
        ItemStack item = plugin.getItemManager().getItem(key);

        if (item == null) {
            sender.sendMessage("§c未找到物品: " + key);
            return true;
        }

        Player player = (Player) sender;
        player.getInventory().addItem(item);
        sender.sendMessage("§a已获取物品: " + key);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return itemKeys.stream()
                    .filter(s -> s.startsWith(args[0].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
