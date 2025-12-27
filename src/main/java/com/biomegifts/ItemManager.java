package com.biomegifts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
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
        registerItem("WATER_GEL", Material.SLIME_BALL, "储水凝胶", 10010, "§a沙漠植物为了生存进化出的精华。");
        registerItem("TROPICAL_NECTAR", Material.HONEY_BOTTLE, "热带糖蜜", 10011, "§e湿热环境下积累的高糖分蜜露。");
        registerItem("FROST_BERRY", Material.SWEET_BERRIES, "霜糖果实", 10012, "§b经霜之后变得异常甜美的果实。");

        // 新增矿业特产 (Ores+)
        registerItem("COPPER_CRYSTAL", Material.RAW_COPPER, "孔雀石晶体", 10014, "§2铜在特定环境下结晶出的伴生宝石，有一种氧化后的美感。");
        registerItem("JADE_SHARD", Material.EMERALD, "高山翠玉", 10015, "§a比绿宝石更纯净，带有东方韵味的玉石原石。");
        registerItem("ECHO_SHARD", Material.AMETHYST_SHARD, "共鸣晶簇", 10016, "§d发着微光的紫色尖刺状晶体，周围有一圈声波状的粒子效果。");
        registerItem("SOUL_SHARD", Material.QUARTZ, "灵魂玻片", 10017, "§f苍白半透明的薄片，里面仿佛封印着扭曲的灵魂面孔。");
        registerItem("VULCAN_SCALE", Material.NETHERITE_SCRAP, "火神之鳞", 10018, "§6暗金色带有熔岩裂纹的鳞片状金属，看起来非常坚硬且烫手。");

        // 新增农业特产 (Crops+)
        registerItem("TERRA_POTATO", Material.POTATO, "大地之心薯", 10019, "§6吸收了大地精华的完美块茎，看起来就淀粉满满，非常抗饿。");
        registerItem("RUBY_CARROT", Material.CARROT, "红玉胡萝卜", 10020, "§c通体晶莹剔透，富含维生素到了结晶的地步，据说吃了眼睛会发光。");
        registerItem("CRIMSON_ESSENCE", Material.BEETROOT, "深红血精", 10021, "§4饱满的深红色球体，这是提纯后的生命精华。");
        registerItem("GLOW_PUMPKIN", Material.PUMPKIN_SEEDS, "辉光南瓜瓤", 10022, "§6吸收了日照的精华，哪怕不插火把，它自己都在发光。");
        registerItem("CRYSTAL_CANE", Material.SUGAR_CANE, "水晶糖柱", 10023, "§f像玻璃棒一样透明，糖分饱和度200%的极品甘蔗。");
        registerItem("DARK_COCOA", Material.COCOA_BEANS, "醇黑可可脂", 10024, "§8顶级巧克力的原材料，看着就觉得苦中带甜。");
        registerItem("JADE_SHOOT", Material.BAMBOO, "翠玉竹笋", 10025, "§a质感完全是玉石的短小竹笋，熊猫看了都舍不得吃的宝物。");
        registerItem("IODINE_CRYSTAL", Material.DRIED_KELP, "深海碘晶", 10026, "§1来自深海的馈赠，炼金术的重要材料。");
        registerItem("DOOM_SPORE", Material.NETHER_WART, "厄运孢子簇", 10027, "§4炼制邪恶药水的核心，看着就掉SAN值。");
        registerItem("CAVE_PEARL", Material.GLOW_BERRIES, "洞穴夜明珠", 10028, "§e繁茂洞穴的照明之源，可以挂在身上当灯泡。");
        registerItem("VOID_CORE", Material.CHORUS_FRUIT, "虚空回响核", 10029, "§5形状破碎的几何体，吃下去可能会瞬移到另一个维度。");

        // 特殊种子
        registerItem("SPIRIT_WHEAT_SEEDS", Material.WHEAT_SEEDS, "灵契之种", 10013, "§6地灵赠予的特殊种子，成熟后必结出黄金麦穗。");

        // 新增畜牧特产 (PastureSong)
        // 奶牛
        registerItem("FROST_FAT_MILK", Material.MILK_BUCKET, "霜脂牛乳", 10030, "§b产自寒冷地区的特浓牛奶，表面漂浮着一层厚厚的奶脂。");
        registerItem("CONDENSED_CURD", Material.MILK_BUCKET, "浓缩乳酪", 10031, "§e炎热气候下水分蒸发形成的浓缩奶酪，口感极其厚重。");
        registerItem("ANTIDOTE_MILK", Material.MILK_BUCKET, "解毒清乳", 10032, "§a雨林牛食用草药后产出的具有解毒功效的清乳。");
        
        // 绵羊
        registerItem("CRYO_FLEECE", Material.WHITE_WOOL, "极地暖绒", 10033, "§b每一根纤维都像是冰晶拉丝而成，摸上去冰凉刺骨。");
        registerItem("STATIC_WOOL", Material.WHITE_WOOL, "静电硬毛", 10034, "§e干燥环境下充满了静电的硬质羊毛，靠近会有酥麻感。");
        registerItem("SILKY_YARN", Material.WHITE_WOOL, "丝滑柔纱", 10035, "§f如同丝绸般顺滑的高级羊毛，泛着珍珠般的光泽。");
        
        // 鸡
        registerItem("ICE_QUILL", Material.FEATHER, "冰晶硬羽", 10036, "§b坚硬如冰锥的羽毛，极其锋利。");
        registerItem("EMBER_FEATHER", Material.FEATHER, "余烬飞羽", 10037, "§c根部似乎还在燃烧的赤红羽毛，带有余温。");
        registerItem("GOLDEN_SHELL_EGG", Material.EGG, "金壳蛋", 10038, "§6蛋壳呈现纯金色，拿在手里沉甸甸的。");
        
        // 猪
        registerItem("BLACK_TRUFFLE", Material.BROWN_MUSHROOM, "黑松露", 10039, "§8散发着浓郁异香的黑色块菌，价比黄金。");
        registerItem("SWAMP_ROOT", Material.BEETROOT, "沼泽药根", 10040, "§2生长在泥沼深处的扭曲根茎，蕴含着神秘的药性。");
        registerItem("SAND_TUBER", Material.POTATO, "沙棘块茎", 10041, "§e为了在沙漠生存而储存了大量水分的块茎。");

        // 基础特产 (Basic Specialties - Any Biome)
        registerItem("RICH_MILK", Material.MILK_BUCKET, "醇香牛乳", 10042, "§f口感醇厚，比普通牛奶更有营养。");
        registerItem("SOFT_WOOL", Material.WHITE_WOOL, "柔软羊毛", 10043, "§f比普通羊毛更加柔软舒适。");
        registerItem("FINE_FEATHER", Material.FEATHER, "优质羽毛", 10044, "§f挑选出的完整羽毛，适合制作工艺品。");
        registerItem("WHITE_TRUFFLE", Material.BROWN_MUSHROOM, "白松露", 10045, "§f虽然不如黑松露珍贵，但也是不错的食材。");


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

    public List<String> getItemKeys() {
        return new ArrayList<>(customItems.keySet());
    }
}
