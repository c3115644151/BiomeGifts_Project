package com.biomegifts;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class MiningListener implements Listener {
    private final BiomeGifts plugin;

    public MiningListener(BiomeGifts plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void onOreBreak(BlockBreakEvent event) {
        // Debug entry - UNCOMMENTED AND ENHANCED
        plugin.getLogger().info("[Debug] Event: " + event.getEventName() + 
                              " Block: " + event.getBlock().getType() + 
                              " Cancelled: " + event.isCancelled());

        Player player = event.getPlayer();
        
        Block block = event.getBlock();
        ConfigManager.ResourceConfig config = plugin.getConfigManager().getOreConfig(block.getType());
        
        if (config == null) {
             return;
        }
        
        // 获取当前群系对该矿物的适应性
        String biomeKey = block.getWorld().getBiome(block.getLocation()).getKey().toString();
        ConfigManager.BiomeType biomeType = config.getBiomeType(biomeKey);
        
        double chance = config.baseChance;
        if (biomeType == ConfigManager.BiomeType.RICH) {
            chance *= config.richMultiplier;
        } else if (biomeType == ConfigManager.BiomeType.POOR) {
            chance *= config.poorMultiplier;
        } else {
            // Normal area logic: 0.5x drop rate as requested
            // "普通区就是没设定的那些群系...特产掉落同等0.5"
            // So Normal multiplier = 0.5?
            // Or does "同等0.5" mean same as Poor (which is 0.5)?
            // Let's assume Normal = 0.5 * Base.
            // Wait, previous code had Poor = 0.5.
            // So Normal and Poor are both 0.5? 
            // Rich = 1.5.
            // Let's stick to the user's latest instruction: "Normal area... specialty drop equivalent 0.5"
            chance *= 0.5;
        }
        
        // Debug Output - Force enable for diagnosis
        plugin.getLogger().info(String.format("[Debug] Mining %s in %s (%s). Base: %.2f, Final Chance: %.2f%%", 
            block.getType(), biomeKey, biomeType, config.baseChance, chance * 100));
        
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            ItemStack drop = plugin.getItemManager().getItem(config.dropItem);
            if (drop != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
                plugin.getLogger().info("[Debug] SUCCESS: Dropped special item: " + config.dropItem);
                
                // 音效反馈
                player.playSound(block.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            } else {
                plugin.getLogger().warning("Special item not found in ItemManager: " + config.dropItem);
            }
        } else {
            plugin.getLogger().info("[Debug] FAIL: Roll failed for " + block.getType());
        }
    }
}
