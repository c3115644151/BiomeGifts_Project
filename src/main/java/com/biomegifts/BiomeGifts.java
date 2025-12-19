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
}
