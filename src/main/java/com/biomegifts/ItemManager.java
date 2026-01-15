package com.biomegifts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import com.nexuscore.api.NexusKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private final BiomeGifts plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();

    public ItemManager(BiomeGifts plugin) {
        this.plugin = plugin;
        loadItems();
    }

    private void loadItems() {
        plugin.getLogger().info("Loading special items from config...");
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    registerConfigItem(key, itemsSection.getConfigurationSection(key));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load item: " + key);
                    e.printStackTrace();
                }
            }
        }
        
        plugin.getLogger().info("Registered " + customItems.size() + " special items.");
    }

    private void registerConfigItem(String key, ConfigurationSection section) {
        String materialName = section.getString("material");
        Material material = Material.valueOf(materialName);
        String name = section.getString("name");
        int modelData = section.getInt("custom-model-data");
        boolean hasStar = section.getBoolean("star", true);
        List<String> lore = section.getStringList("lore");

        registerItem(key, material, name, modelData, hasStar, lore);
    }

    private void registerItem(String key, Material material, String name, int modelData, boolean hasStar, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).color(NamedTextColor.GOLD)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            
            List<Component> loreComponents = new ArrayList<>();
            for (String line : loreLines) {
                loreComponents.add(Component.text(line).color(NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(loreComponents);
            
            meta.setCustomModelData(modelData);

            // [New] Add ID to PDC for identification by other plugins
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey idKey = new org.bukkit.NamespacedKey(plugin, "id");
            pdc.set(idKey, PersistentDataType.STRING, key);
            // NexusCore Unified ID
            pdc.set(NexusKeys.ITEM_ID, PersistentDataType.STRING, key);

            // [New] Add Star Flag to PDC
            if (hasStar) {
                NamespacedKey starKey = new NamespacedKey(plugin, "nexus_has_star");
                pdc.set(starKey, PersistentDataType.INTEGER, 1);
            }

            item.setItemMeta(meta);
        }
        customItems.put(key, item);
    }

    public ItemStack getItem(String key) {
        return customItems.get(key) != null ? customItems.get(key).clone() : null;
    }

    public List<String> getItemKeys() {
        return new ArrayList<>(customItems.keySet());
    }

    public java.util.Collection<ItemStack> getAllItems() {
        return customItems.values();
    }
}
