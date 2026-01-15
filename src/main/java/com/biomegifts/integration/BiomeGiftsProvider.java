package com.biomegifts.integration;

import com.biomegifts.BiomeGifts;
import com.nexuscore.api.NexusItemProvider;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BiomeGiftsProvider implements NexusItemProvider {

    private final BiomeGifts plugin;

    public BiomeGiftsProvider(BiomeGifts plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getModuleId() {
        return "biome-gifts";
    }

    @Override
    public String getDisplayName() {
        return "BiomeGifts";
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.TOTEM_OF_UNDYING);
    }

    @Override
    public List<ItemStack> getItems() {
        return new ArrayList<>(plugin.getItemManager().getAllItems());
    }
}
