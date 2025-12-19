package com.biomegifts;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class SpecialCropManager {
    private final BiomeGifts plugin;
    private final File file;
    private final Set<Location> specialCrops = new HashSet<>();

    public SpecialCropManager(BiomeGifts plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "special_crops.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getStringList("locations")) {
            try {
                specialCrops.add(stringToLoc(key));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load special crop location: " + key);
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("locations", specialCrops.stream().map(this::locToString).toList());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save special crops!", e);
        }
    }

    public void addCrop(Location loc) {
        specialCrops.add(loc);
        save();
    }

    public boolean isSpecialCrop(Location loc) {
        return specialCrops.contains(loc);
    }

    public void removeCrop(Location loc) {
        specialCrops.remove(loc);
        save();
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        String[] parts = str.split(",");
        return new Location(
            plugin.getServer().getWorld(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3])
        );
    }
}
