package com.biomegifts;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class CropListener implements Listener {
    private final BiomeGifts plugin;

    public CropListener(BiomeGifts plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCropGrow(BlockGrowEvent event) {
        // If CuisineFarming is present, it handles all growth logic (efficiency, speed, etc.)
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("CuisineFarming")) {
            return;
        }

        Block block = event.getBlock();
        ConfigManager.CropConfig config = plugin.getConfigManager().getCropConfig(block.getType());
        
        if (config == null) return;
        
        String biomeKey = block.getWorld().getBiome(block.getLocation()).getKey().toString();
        ConfigManager.BiomeType biomeType = config.getBiomeType(biomeKey);
        
        if (biomeType == ConfigManager.BiomeType.POOR) {
            // Poor Soil Penalty: Slower than vanilla (handled here by cancelling vanilla events)
            if (ThreadLocalRandom.current().nextDouble() < config.poorSpeedPenalty) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onCropHarvest(BlockBreakEvent event) {
        // Unified Crop Drop Logic (BiomeGifts + CuisineFarming + EarthSpirit)
        // Handled centrally here in BiomeGifts.

        Block block = event.getBlock();
        ConfigManager.CropConfig config = plugin.getConfigManager().getCropConfig(block.getType());
        
        if (config == null) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        
        // Only drop if fully grown
        if (ageable.getAge() != ageable.getMaximumAge()) return;

        // Check for Bone Meal (Artificial Growth) - Set by CuisineFarming
        if (block.hasMetadata("bonemealed")) {
            plugin.getLogger().info("[Debug] Bone Meal harvest detected. Skipping specialty drops.");
            return;
        }

        // Special Crop Logic (Spirit Seed)
        if (plugin.getSpecialCropManager().isSpecialCrop(block.getLocation())) {
            plugin.getSpecialCropManager().removeCrop(block.getLocation());
            
            // Guaranteed Golden Wheat
            ItemStack drop = plugin.getItemManager().getItem("GOLDEN_WHEAT");
            if (drop != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
                // Effects
                block.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.5);
                block.getWorld().playSound(block.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 2.0f);
                plugin.getLogger().info("[Debug] Special Crop Harvested!");
            }
            return; // Skip normal calculation
        }
        
        // Unified Drop Logic Formula (Updated 2025-12-21)
        // Probability = BaseChance(Biome) * GeneMultiplier * (1.0 + FertilityBonus + SpiritBonus)
        
        java.util.Map<String, Object> details = plugin.calculateDropDetails(block);
        
        if (!details.containsKey("totalChance")) return;
        
        double totalChance = (double) details.get("totalChance");
        double baseChance = (double) details.getOrDefault("baseChance", 0.0);
        double geneMultiplier = (double) details.getOrDefault("geneMultiplier", 1.0);
        double fertilityBonus = (double) details.getOrDefault("fertilityBonus", 0.0);
        double spiritBonus = (double) details.getOrDefault("spiritBonus", 0.0);
        
        plugin.getLogger().info(String.format("[Debug] Harvesting %s. Breakdown: Base=%.2f%%, Gene=x%.2f, Bonuses=(Fert=%.2f + Spirit=%.2f) -> Total=%.2f%%", 
            block.getType(), baseChance * 100, geneMultiplier, fertilityBonus, spiritBonus, totalChance * 100));
        
        if (ThreadLocalRandom.current().nextDouble() < totalChance) {
             ItemStack drop = plugin.getItemManager().getItem(config.dropItem);
             if (drop != null) {
                 block.getWorld().dropItemNaturally(block.getLocation(), drop);
                 // Sound
                 block.getWorld().playSound(block.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);
                 plugin.getLogger().info("[Debug] SUCCESS: Dropped " + config.dropItem);
             }
        } else {
             plugin.getLogger().info("[Debug] FAIL: Roll failed for " + block.getType());
        }
    }
}
