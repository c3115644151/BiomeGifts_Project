package com.biomegifts;

import org.bukkit.plugin.java.JavaPlugin;

public class BiomeGifts extends JavaPlugin {

    private static BiomeGifts instance;
    private ConfigManager configManager;
    private ItemManager itemManager;
    private SpecialCropManager specialCropManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load Configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        // Initialize Managers
        itemManager = new ItemManager(this);
        specialCropManager = new SpecialCropManager(this);
        
        // Register Listeners
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new CropListener(this), this);

        // Register Commands
        getCommand("getgift").setExecutor(new BiomeGiftsCommand(this));
        
        getLogger().info("BiomeGifts has been enabled!");
    }

    @Override
    public void onDisable() {
        if (specialCropManager != null) {
            specialCropManager.save();
        }
        getLogger().info("BiomeGifts has been disabled!");
    }

    public static BiomeGifts getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public SpecialCropManager getSpecialCropManager() {
        return specialCropManager;
    }

    /**
     * Calculates the base drop chance for a block based on biome configuration.
     * Used by external plugins (e.g., CuisineFarming) to unify drop logic.
     * @param block The block being harvested
     * @return The probability (0.0 to 1.0) of a specialty drop, or 0.0 if not configured.
     */
    public double getBiomeDropChance(org.bukkit.block.Block block) {
        ConfigManager.CropConfig config = configManager.getCropConfig(block.getType());
        if (config == null) return 0.0;

        String biomeKey = block.getWorld().getBiome(block.getLocation()).getKey().toString();
        ConfigManager.BiomeType biomeType = config.getBiomeType(biomeKey);

        double chance = config.baseChance;
        if (biomeType == ConfigManager.BiomeType.RICH) {
            chance *= config.richMultiplier;
        } else if (biomeType == ConfigManager.BiomeType.POOR) {
            chance *= config.poorMultiplier;
        } else {
            // Normal area: 0.5x multiplier
            chance *= 0.5;
        }
        return chance;
    }

    /**
     * Calculates the full drop chance breakdown for a block.
     * Includes Biome Base, CuisineFarming Genes/Fertility, and EarthSpirit Bonuses.
     * @param block The block to calculate for
     * @return A map containing the calculation components and total chance.
     */
    public java.util.Map<String, Object> calculateDropDetails(org.bukkit.block.Block block) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        ConfigManager.CropConfig config = configManager.getCropConfig(block.getType());
        if (config == null) return result;
        
        result.put("dropItem", config.dropItem);

        // 1. Base Chance (Biome)
        double baseChance = getBiomeDropChance(block);
        result.put("baseChance", baseChance);
        
        if (baseChance <= 0.0) {
            result.put("totalChance", 0.0);
            return result;
        }

        double geneMultiplier = 1.0;
        double fertilityBonus = 0.0;
        double spiritBonus = 0.0;

        // 2. Gene Multiplier & Fertility (CuisineFarming Integration)
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("CuisineFarming")) {
            try {
                org.bukkit.plugin.Plugin cuisinePlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("CuisineFarming");
                Class<?> mainClass = cuisinePlugin.getClass();

                // --- Fertility Bonus ---
                 Object fertilityManager = mainClass.getMethod("getFertilityManager").invoke(cuisinePlugin);
                 // Use getFertilityDropBonus directly (returns the calculated bonus, e.g. 0.10 for 10%)
                 java.lang.reflect.Method getDropBonusMethod = fertilityManager.getClass().getMethod("getFertilityDropBonus", org.bukkit.block.Block.class);
                 fertilityBonus = (double) getDropBonusMethod.invoke(fertilityManager, block.getRelative(0, -1, 0));

                 // --- Gene Multiplier ---
                Object geneticsManager = mainClass.getMethod("getGeneticsManager").invoke(cuisinePlugin);
                java.lang.reflect.Method getGenesMethod = geneticsManager.getClass().getMethod("getGenesFromBlock", org.bukkit.block.Block.class);
                Object geneData = getGenesMethod.invoke(geneticsManager, block);

                if (geneData != null) {
                    Class<?> geneTypeClass = Class.forName("com.example.cuisinefarming.genetics.GeneType");
                    Object yieldEnum = geneTypeClass.getField("YIELD").get(null);
                    java.lang.reflect.Method getGeneMethod = geneData.getClass().getMethod("getGene", geneTypeClass);
                    double yieldValue = (double) getGeneMethod.invoke(geneData, yieldEnum);
                    
                    double bonusPercent = -0.25 + (yieldValue / 5.0) * 1.25;
                    geneMultiplier = 1.0 + bonusPercent;
                    if (geneMultiplier < 0.0) geneMultiplier = 0.0;
                }
            } catch (Exception e) {
                getLogger().warning("[BiomeGifts] Failed to integrate with CuisineFarming: " + e.getMessage());
            }
        }

        // 3. Spirit Bonus (EarthSpirit Integration)
         if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("EarthSpirit")) {
             try {
                 org.bukkit.plugin.Plugin spiritPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EarthSpirit");
                 // Use getManager() instead of getSpiritManager()
                 Object spiritManager = spiritPlugin.getClass().getMethod("getManager").invoke(spiritPlugin);
                 // Use getSpiritDropBonus() instead of getSpiritBonus()
                 java.lang.reflect.Method getBonusMethod = spiritManager.getClass().getMethod("getSpiritDropBonus", org.bukkit.Location.class);
                 spiritBonus = (double) getBonusMethod.invoke(spiritManager, block.getLocation());
             } catch (Exception e) {
                 getLogger().warning("[BiomeGifts] Failed to integrate with EarthSpirit: " + e.getMessage());
             }
         }
        
        // Final Calculation
        double totalChance = baseChance * geneMultiplier * (1.0 + fertilityBonus + spiritBonus);
        
        result.put("geneMultiplier", geneMultiplier);
        result.put("fertilityBonus", fertilityBonus);
        result.put("spiritBonus", spiritBonus);
        result.put("totalChance", totalChance);
        
        return result;
    }
}
