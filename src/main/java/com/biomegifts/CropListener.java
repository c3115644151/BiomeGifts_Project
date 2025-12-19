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
        Block block = event.getBlock();
        ConfigManager.CropConfig config = plugin.getConfigManager().getCropConfig(block.getType());
        
        if (config == null) return;
        
        String biomeKey = block.getWorld().getBiome(block.getLocation()).getKey().toString();
        ConfigManager.BiomeType biomeType = config.getBiomeType(biomeKey);
        
        if (biomeType == ConfigManager.BiomeType.POOR) {
            // 贫瘠区：减速
            if (ThreadLocalRandom.current().nextDouble() < config.poorSpeedPenalty) {
                event.setCancelled(true);
            }
        } else if (biomeType == ConfigManager.BiomeType.RICH) {
            // 富集区：加速
            if (ThreadLocalRandom.current().nextDouble() < config.richSpeedBonus) {
                // 播放快乐粒子
                block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.3, 0.2, 0.3);
                
                // 跳级生长逻辑
                if (event.getNewState().getBlockData() instanceof Ageable ageable) {
                    int currentAge = ageable.getAge();
                    int maxAge = ageable.getMaximumAge();
                    
                    // 如果还能长，就多长一岁
                    // 注意：event.getNewState() 已经是 +1 后的状态了
                    // 比如原版是 0 -> 1。我们想让它变成 2。
                    // 但不能直接改 event.getNewState()，因为那是 snapshot。
                    // 我们可以取消原事件，然后手动设为 +2？
                    // 或者更优雅：不取消事件，但在事件结束后（下一 tick）再 +1。
                    
                    if (currentAge < maxAge) { // 这里的 age 是“即将变成”的 age
                         // 这里有个小逻辑陷阱：event.getNewState() 获取的是“即将变成的状态”。
                         // 假设原版逻辑是从 age 2 变成 age 3。
                         // ageable.getAge() 返回的是 3。
                         
                         // 如果我们想跳级，我们希望它变成 4。
                         if (currentAge + 1 <= maxAge) {
                             // 修改 newState 中的数据
                             ageable.setAge(currentAge + 1);
                             // 将修改后的 BlockData 写回 newState
                             // BlockState.setBlockData(ageable)
                             // event.getNewState() 返回的是 BlockState，我们可以直接对它操作吗？
                             // Bukkit API: event.getNewState() returns a BlockState. 
                             // modifying this BlockState DOES affect the outcome IF we don't need to call update().
                             // 因为这个 State 会被系统应用。
                             
                             // 让我们尝试直接修改 State 的 Data
                             // 但是 BlockState 是接口，我们需要 cast
                             // 实际上 event.getNewState() 的 BlockData 是我们要改的。
                             // 但是 BlockState 没有 setBlockData 的简便方法来直接影响事件结果，
                             // 除非该 State 对象就是最终会被 apply 的对象。
                             // 在较新版本 Paper 中，通常修改 event.getNewState() 的属性是有效的。
                             
                             // 更保险的方法：取消事件，手动 setBlockData。
                             event.setCancelled(true);
                             Ageable currentBlockData = (Ageable) block.getBlockData();
                             int newAge = Math.min(maxAge, currentBlockData.getAge() + 2); // 原本+1，我们额外+1
                             currentBlockData.setAge(newAge);
                             block.setBlockData(currentBlockData);
                             
                             // 播放骨粉音效
                             block.getWorld().playSound(block.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.5f, 1.0f);
                         }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onCropHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        ConfigManager.CropConfig config = plugin.getConfigManager().getCropConfig(block.getType());
        
        if (config == null) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        
        // 只有成熟才掉落特产
        if (ageable.getAge() != ageable.getMaximumAge()) return;

        // 检查是否是特殊作物 (灵契之种)
        if (plugin.getSpecialCropManager().isSpecialCrop(block.getLocation())) {
            plugin.getSpecialCropManager().removeCrop(block.getLocation());
            
            // 必掉黄金麦穗
            ItemStack drop = plugin.getItemManager().getItem("GOLDEN_WHEAT");
            if (drop != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
                // 华丽的音效和特效
                block.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.5);
                block.getWorld().playSound(block.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 2.0f);
                plugin.getLogger().info("[Debug] Special Crop Harvested!");
            }
            return; // 特殊作物处理完毕，跳过常规概率计算
        }
        
        String biomeKey = block.getWorld().getBiome(block.getLocation()).getKey().toString();
        ConfigManager.BiomeType biomeType = config.getBiomeType(biomeKey);
        
        double chance = config.baseChance;
        
        // 草案：“作物特产只能在特定群系掉落”
        // 但用户最新指示：“普通区也要处理...特产掉落同等0.5”
        // 这意味着 Normal 区域也要掉落，只是概率可能低一些，或者和 Poor 一样？
        // 假设：Rich > Normal > Poor
        // 如果用户说“特产掉落同等0.5”，可能指 Normal = 0.5 * Base? 或者 Normal = Base, Poor = 0.5 * Base?
        // 根据上下文 "贫瘠区概率可能减半"，那么 Normal 应该是 Base。
        
        if (biomeType == ConfigManager.BiomeType.RICH) {
            chance *= config.richMultiplier;
        } else if (biomeType == ConfigManager.BiomeType.POOR) {
            chance *= config.poorMultiplier; // 0.5
            // 如果不想让贫瘠区掉落，可以在 config 里把 poor_multiplier 设为 0
        } else {
            // Normal area logic: 0.5x drop rate
            chance *= 0.5;
        }
        
        // Debug Output
        plugin.getLogger().info(String.format("[Debug] Harvesting %s in %s (%s). Chance: %.2f%%", 
            block.getType(), biomeKey, biomeType, chance * 100));
        
        if (ThreadLocalRandom.current().nextDouble() < chance) {
             ItemStack drop = plugin.getItemManager().getItem(config.dropItem);
             if (drop != null) {
                 block.getWorld().dropItemNaturally(block.getLocation(), drop);
                 // 收获音效
                 block.getWorld().playSound(block.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);
                 plugin.getLogger().info("[Debug] SUCCESS: Dropped " + config.dropItem);
             }
        } else {
             plugin.getLogger().info("[Debug] FAIL: Roll failed for " + block.getType());
        }
    }
}
