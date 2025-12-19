package com.biomegifts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private final BiomeGifts plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();

    public ItemManager(BiomeGifts plugin) {
        this.plugin = plugin;
        registerItems();
    }

    private void registerItems() {
        plugin.getLogger().info("Registering special items...");
        // 煤炭特产
        registerItem("LIGNITE", Material.COAL, "高能褐煤", 10001, "§7沼泽的沉积更久，产出的煤含碳量极高。");
        // ... (其他保持不变)
        // 铁矿特产
        registerItem("RICH_SLAG", Material.RAW_IRON, "富铁", 10006, "§7高原地区裸露的铁矿，受天体辐射变异。");
        
        // 金矿特产
        registerItem("GOLD_DUST", Material.RAW_GOLD, "纯度金沙", 10007, "§7河流冲刷出的高纯度金。");
        
        // 红石特产
        registerItem("CHARGED_DUST", Material.REDSTONE, "充能尘埃", 10008, "§7干热环境让红石带有不稳定的高能电荷。");
        
        // 钻石特产
        registerItem("ICE_SHARD", Material.DIAMOND, "冰川碎钻", 10003, "§b极寒高压下形成的特殊钻石，带有冰属性。");
        
        // 青金石特产
        registerItem("TIDE_ESSENCE", Material.LAPIS_LAZULI, "海潮沉淀", 10009, "§9吸收了深海魔力的矿石伴生物。");
        
        // 作物特产
        registerItem("GOLDEN_WHEAT", Material.WHEAT, "黄金麦穗", 10005, "§6只有温带的阳光才能晒出的饱满谷物。");
        registerItem("WATER_GEL", Material.SLIME_BALL, "储水凝胶", 10010, "§a沙漠植物为了生存进化出的精华。"); // 暂用粘液球
        registerItem("TROPICAL_NECTAR", Material.HONEY_BOTTLE, "热带糖蜜", 10011, "§e湿热环境下积累的高糖分蜜露。");
        registerItem("FROST_BERRY", Material.SWEET_BERRIES, "霜糖果实", 10012, "§b经霜之后变得异常甜美的果实。");
        
        // 特殊种子
        registerItem("SPIRIT_WHEAT_SEEDS", Material.WHEAT_SEEDS, "灵契之种", 10013, "§6地灵赠予的特殊种子，成熟后必结出黄金麦穗。");

        plugin.getLogger().info("Registered " + customItems.size() + " special items.");
    }

    private void registerItem(String key, Material material, String name, int modelData, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GOLD));
            meta.setCustomModelData(modelData);
            meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        customItems.put(key, item);
    }

    public ItemStack getItem(String key) {
        return customItems.get(key) != null ? customItems.get(key).clone() : null;
    }
}
