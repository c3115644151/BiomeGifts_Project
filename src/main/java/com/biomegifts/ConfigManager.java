package com.biomegifts;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.regex.Pattern;

public class ConfigManager {
    private final BiomeGifts plugin;
    
    // 缓存资源配置：Material -> ResourceConfig
    private final Map<Material, ResourceConfig> oreConfigs = new HashMap<>();
    private final Map<Material, CropConfig> cropConfigs = new HashMap<>();

    public ConfigManager(BiomeGifts plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        oreConfigs.clear();
        cropConfigs.clear();
        
        // Load Ores
        if (config.contains("ores")) {
            ConfigurationSection oresSection = config.getConfigurationSection("ores");
            for (String key : oresSection.getKeys(false)) {
                ConfigurationSection section = oresSection.getConfigurationSection(key);
                ResourceConfig rc = new ResourceConfig(section);
                
                // Register for main material
                Material mainMat = Material.getMaterial(key);
                if (mainMat != null) oreConfigs.put(mainMat, rc);
                
                // Register aliases
                for (String alias : section.getStringList("aliases")) {
                    Material aliasMat = Material.getMaterial(alias);
                    if (aliasMat != null) oreConfigs.put(aliasMat, rc);
                }
            }
        }
        
        // Load Crops
        if (config.contains("crops")) {
            ConfigurationSection cropsSection = config.getConfigurationSection("crops");
            for (String key : cropsSection.getKeys(false)) {
                ConfigurationSection section = cropsSection.getConfigurationSection(key);
                CropConfig cc = new CropConfig(section);
                
                Material mainMat = Material.getMaterial(key);
                if (mainMat != null) cropConfigs.put(mainMat, cc);
                
                for (String alias : section.getStringList("aliases")) {
                    Material aliasMat = Material.getMaterial(alias);
                    if (aliasMat != null) cropConfigs.put(aliasMat, cc);
                }
            }
        }
    }

    public ResourceConfig getOreConfig(Material material) {
        return oreConfigs.get(material);
    }
    
    public CropConfig getCropConfig(Material material) {
        return cropConfigs.get(material);
    }

    // Inner classes for structured config data
    public static class ResourceConfig {
        public String dropItem;
        public double baseChance;
        public List<Pattern> richBiomes = new ArrayList<>();
        public double richMultiplier;
        public List<Pattern> poorBiomes = new ArrayList<>();
        public double poorMultiplier;

        public ResourceConfig(ConfigurationSection section) {
            this.dropItem = section.getString("drop_item");
            this.baseChance = section.getDouble("base_chance");
            this.richMultiplier = section.getDouble("rich_multiplier", 1.0);
            this.poorMultiplier = section.getDouble("poor_multiplier", 1.0);
            
            for (String regex : section.getStringList("rich_biomes")) {
                richBiomes.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            }
            for (String regex : section.getStringList("poor_biomes")) {
                poorBiomes.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            }
        }
        
        public BiomeType getBiomeType(String biomeKey) {
            for (Pattern p : richBiomes) {
                if (p.matcher(biomeKey).find()) {
                    return BiomeType.RICH;
                }
            }
            for (Pattern p : poorBiomes) {
                if (p.matcher(biomeKey).find()) {
                    return BiomeType.POOR;
                }
            }
            return BiomeType.NORMAL;
        }
    }
    
    public static class CropConfig extends ResourceConfig {
        public double richSpeedBonus;
        public double poorSpeedPenalty;
        
        public CropConfig(ConfigurationSection section) {
            super(section);
            this.richSpeedBonus = section.getDouble("rich_speed_bonus", 0.0);
            this.poorSpeedPenalty = section.getDouble("poor_speed_penalty", 0.0);
        }
    }
    
    public enum BiomeType {
        RICH, POOR, NORMAL
    }
}
