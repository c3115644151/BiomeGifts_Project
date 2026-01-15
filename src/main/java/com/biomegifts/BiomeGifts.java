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

        // NexusCore Integration and Listener
        registerWithNexusCore();
        
        // Save nexus_recipes.yml if not exists
        if (!new java.io.File(getDataFolder(), "nexus_recipes.yml").exists()) {
            saveResource("nexus_recipes.yml", false);
        }
        
        // Load Nexus Recipes
        try {
            com.nexuscore.recipe.RecipeConfigLoader.load(com.nexuscore.NexusCore.getInstance(), new java.io.File(getDataFolder(), "nexus_recipes.yml"));
            getLogger().info("Loaded Nexus recipes.");
        } catch (Exception e) {
            getLogger().warning("Failed to load Nexus recipes: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPluginEnable(org.bukkit.event.server.PluginEnableEvent event) {
                if (event.getPlugin().getName().equals("NexusCore")) {
                    getLogger().info("NexusCore re-enabled detected. Re-registering modules...");
                    registerWithNexusCore();
                }
            }
        }, this);

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
     * 
     * @param block The block being harvested
     * @return The probability (0.0 to 1.0) of a specialty drop, or 0.0 if not
     *         configured.
     */
    public double getBiomeDropChance(org.bukkit.block.Block block) {
        ConfigManager.CropConfig config = configManager.getCropConfig(block.getType());
        if (config == null)
            return 0.0;

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
     * 
     * @param block The block to calculate for
     * @return A map containing the calculation components and total chance.
     */
    public java.util.Map<String, Object> calculateDropDetails(org.bukkit.block.Block block) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        ConfigManager.CropConfig config = configManager.getCropConfig(block.getType());
        if (config == null)
            return result;

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
                org.bukkit.plugin.Plugin cuisinePlugin = org.bukkit.Bukkit.getPluginManager()
                        .getPlugin("CuisineFarming");
                Class<?> mainClass = cuisinePlugin.getClass();

                // --- Fertility Bonus ---
                Object fertilityManager = mainClass.getMethod("getFertilityManager").invoke(cuisinePlugin);
                // Use getFertilityDropBonus directly (returns the calculated bonus, e.g. 0.10
                // for 10%)
                java.lang.reflect.Method getDropBonusMethod = fertilityManager.getClass()
                        .getMethod("getFertilityDropBonus", org.bukkit.block.Block.class);
                fertilityBonus = (double) getDropBonusMethod.invoke(fertilityManager, block.getRelative(0, -1, 0));

                // --- Gene Multiplier ---
                Object geneticsManager = mainClass.getMethod("getGeneticsManager").invoke(cuisinePlugin);
                java.lang.reflect.Method getGenesMethod = geneticsManager.getClass().getMethod("getGenesFromBlock",
                        org.bukkit.block.Block.class);
                Object geneData = getGenesMethod.invoke(geneticsManager, block);

                if (geneData != null) {
                    Class<?> traitClass = Class.forName("com.example.cuisinefarming.genetics.Trait");
                    Object yieldEnum = traitClass.getField("YIELD").get(null);

                    // getGenePair(Trait) returns GenePair
                    java.lang.reflect.Method getGenePairMethod = geneData.getClass().getMethod("getGenePair",
                            traitClass);
                    Object genePair = getGenePairMethod.invoke(geneData, yieldEnum);

                    // GenePair.getPhenotypeValue() returns double
                    java.lang.reflect.Method getPhenotypeValueMethod = genePair.getClass()
                            .getMethod("getPhenotypeValue");
                    double yieldValue = (double) getPhenotypeValueMethod.invoke(genePair);

                    // Normalize: [-10.0, 10.0] -> [0.5, 2.0]
                    // Map [-10, 10] to [0.5, 2.0]
                    // Formula: y = 0.075 * x + 1.25
                    double multiplier = 0.075 * yieldValue + 1.25;

                    // Clamp to range [0.5, 2.0]
                    if (multiplier < 0.5)
                        multiplier = 0.5;
                    if (multiplier > 2.0)
                        multiplier = 2.0;

                    geneMultiplier = multiplier;
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
                java.lang.reflect.Method getBonusMethod = spiritManager.getClass().getMethod("getSpiritDropBonus",
                        org.bukkit.Location.class);
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

    public void registerWithNexusCore() {
        org.bukkit.plugin.Plugin nexusCore = getServer().getPluginManager().getPlugin("NexusCore");
        if (nexusCore != null && nexusCore.isEnabled()) {
            try {
                Object registry = nexusCore.getClass().getMethod("getRegistry").invoke(nexusCore);
                
                java.lang.reflect.Method registerMethod = null;
                for (java.lang.reflect.Method m : registry.getClass().getMethods()) {
                    if (m.getName().equals("register") && m.getParameterCount() == 6) {
                        registerMethod = m;
                        break;
                    }
                }
                
                if (registerMethod == null) {
                    registerMethod = registry.getClass().getMethod("register", 
                        String.class, String.class, java.util.function.Supplier.class, java.util.function.Supplier.class);
                }

                java.util.function.Supplier<org.bukkit.inventory.ItemStack> iconSupplier = () -> new org.bukkit.inventory.ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING);
                java.util.function.Supplier<java.util.List<org.bukkit.inventory.ItemStack>> itemsSupplier = () -> new java.util.ArrayList<>(itemManager.getAllItems());

                // Star Filter: Check PDC for nexus_has_star
                java.util.function.Function<org.bukkit.inventory.ItemStack, Boolean> starFilter = (item) -> {
                    if (item.hasItemMeta()) {
                        org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                        org.bukkit.NamespacedKey starKey = new org.bukkit.NamespacedKey(this, "nexus_has_star");
                        if (pdc.has(starKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                            return pdc.get(starKey, org.bukkit.persistence.PersistentDataType.INTEGER) == 1;
                        }
                    }
                    return false; // Default to false if no flag found
                };

                if (registerMethod.getParameterCount() == 6) {
                    registerMethod.invoke(registry, "biome-gifts", "BiomeGifts", iconSupplier, itemsSupplier, null, starFilter);
                } else {
                    registerMethod.invoke(registry, "biome-gifts", "BiomeGifts", iconSupplier, itemsSupplier);
                }

                getLogger().info("Registered items with NexusCore Nexus (Using Reflection).");
            } catch (Exception e) {
                getLogger().warning("Failed to register with NexusCore: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
