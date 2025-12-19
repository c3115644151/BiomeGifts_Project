# 地域馈赠系统 (BiomeGifts) 操作文档

## 1. 系统概述 (Overview)
**地域馈赠系统** 是一个旨在增强 Minecraft 原版探索与生存体验的插件。它通过将特定的资源产出（矿物掉落、作物生长）与**生物群系 (Biome)** 深度绑定，鼓励玩家走出舒适区，前往世界的各个角落建立据点或进行采集。

系统的核心机制是将群系划分为 **富集区 (Rich)**、**贫瘠区 (Poor)** 和 **普通区 (Normal)**，不同区域对资源的产出效率有显著影响。

---

## 2. 核心架构 (Core Architecture)

| 模块 | 类文件 | 职责 |
| :--- | :--- | :--- |
| **插件核心** | `BiomeGifts.java` | 插件入口，负责各管理器的初始化与生命周期管理。 |
| **配置管理** | `ConfigManager.java` | 加载 `config.yml`，解析群系正则匹配规则，提供资源配置查询服务。 |
| **物品管理** | `ItemManager.java` | 负责自定义物品（如黄金麦穗、热带糖蜜等）的加载与堆叠生成。 |
| **采矿系统** | `MiningListener.java` | 监听矿物破坏事件，根据群系修正稀有物品的掉落概率。 |
| **农业系统** | `CropListener.java` | 监听作物生长与收割，处理生长速度修正（加速/减速）及特产掉落。 |
| **特殊作物** | `SpecialCropManager.java` | 管理特殊作物（如灵契之种）的位置持久化与收割逻辑。 |
| **Terra 集成** | `terra_templates/` | **[关键]** 包含 Terra 世界生成器的配置模板，用于从生成层面控制矿物分布。 |

---

## 3. 详细模块解析 (Detailed Module Analysis)

### 3.1 配置与群系判定 (`ConfigManager.java`)
该模块是系统的“大脑”，负责决定一个方块在当前位置是处于“风水宝地”还是“不毛之地”。

*   **配置结构**:
    *   **Resources (Ores/Crops)**: 每个资源定义了 `base_chance` (基础概率) 和对应的掉落物 `drop_item`。
    *   **Multipliers**: 定义了 `rich_multiplier` (富集倍率) 和 `poor_multiplier` (贫瘠倍率)。
    *   **Biomes**: 使用正则表达式 (`List<Pattern>`) 匹配群系 Key (如 `minecraft:desert`, `terralith:.*jungle.*`)。
*   **判定逻辑**:
    1.  优先匹配 **Rich Biomes** -> 返回 `RICH`。
    2.  其次匹配 **Poor Biomes** -> 返回 `POOR`。
    3.  若都不匹配 -> 返回 `NORMAL`。

### 3.2 采矿系统 (`MiningListener.java`)
*   **触发**: `BlockBreakEvent` (Monitor 优先级)。
*   **逻辑**:
    *   玩家挖掘配置中的矿石（如煤矿、钻石矿）。
    *   根据群系类型计算最终概率：
        *   **Rich**: `Base * Multiplier`
        *   **Poor**: `Base * Multiplier` (通常为 0.5 或更低)
        *   **Normal**: `Base * 0.5` (根据最新设定，普通区掉落率折半)
    *   **结果**: 判定成功后，除了原版掉落外，额外掉落自定义物品（如“余烬碎片”），并播放音效。

### 3.3 农业系统 (`CropListener.java`)
包含两个核心部分：生长干预与收割奖励。

#### 3.3.1 生长干预 (`BlockGrowEvent`)
*   **Poor 区域**: 触发 `poorSpeedPenalty` 概率判定，若命中则 `event.setCancelled(true)`，实现**生长减缓**。
*   **Rich 区域**: 触发 `richSpeedBonus` 概率判定，若命中：
    1.  播放 `HAPPY_VILLAGER` 粒子。
    2.  **跳级生长**: 在原版生长 (+1 age) 的基础上，额外再 +1 age，实现**生长加速**。

#### 3.3.2 收割奖励 (`BlockBreakEvent`)
*   **条件**: 作物必须完全成熟 (`Age == MaxAge`)。
*   **逻辑**: 与采矿类似，根据群系计算概率掉落特产（如“黄金麦穗”）。
*   **特殊机制**: 如果该作物被标记为 `SpecialCrop` (由灵契之种种植)，则**100%掉落**高阶特产，并移除特殊标记。

### 3.4 特殊作物管理 (`SpecialCropManager.java`)
*   **功能**: 允许特定位置的作物拥有特殊属性（如必定掉落奖励）。
*   **持久化**: 使用 `special_crops.yml` 存储坐标 (`World,X,Y,Z`)，确保服务器重启后特殊作物状态不丢失。
*   **应用场景**: 地灵系统每日任务奖励的“灵契之种”，种植后即被记录为特殊作物。

---

## 4. Terra 世界生成集成 (Terra World Generation Integration)

BiomeGifts 系统不仅仅依赖插件代码进行后期的掉落调整，还通过 **Terra** 世界生成器直接在地图生成阶段控制矿物资源的分布密度，从源头上实现“富集”与“贫瘠”。

### 4.1 模板结构 (`terra_templates/`)
该目录包含用于 Terra 插件的配置包（Pack）片段：

*   **`ores_rich.yml` / `ores_poor.yml`**: 
    *   定义了不同群系配置（Biome Configuration）。
    *   `ORES_RICH`: 引用了一系列高产出的矿物特性（如 `DIAMOND_ORE_RICH`, `GOLD_ORE_RICH`）。
    *   `ORES_POOR`: 引用了一系列低产出的矿物特性。

*   **`features/*.yml`**:
    *   定义了具体的矿物生成规则（Feature）。
    *   **示例 (`diamond_ore_rich.yml`)**:
        ```yaml
        anchors:
          # 密度阈值乘数：1.2 (提升20%生成量)
          - &densityThreshold 1/256 * ${...:diamond.averageCountPerChunk} * 1.2 
        ```
    *   **示例 (`diamond_ore_poor.yml`)**:
        ```yaml
        anchors:
          # 密度阈值乘数：0.6 (减少40%生成量)
          - &densityThreshold 1/256 * ${...:diamond.averageCountPerChunk} * 0.6
        ```

### 4.2 工作流 (Workflow)
1.  **世界生成**: Terra 读取 `terra_templates` 中的配置。
    *   在 **Rich Biomes** 生成时，加载 `ORES_RICH` 配置，矿物生成密度增加。
    *   在 **Poor Biomes** 生成时，加载 `ORES_POOR` 配置，矿物生成密度降低。
2.  **游戏内交互**: 
    *   玩家进入该区域，`BiomeGifts` 插件识别当前群系。
    *   挖掘矿物时，插件再次根据群系进行**二次判定**（掉落自定义物品）。

**总结**: Terra 负责**“量”**（矿有多少），BiomeGifts 插件负责**“质”**（掉什么特殊物品）。两者结合构成了完整的地域馈赠体验。

---

## 5. 技术细节备忘 (Technical Notes)

### 5.1 生长跳级实现
在 `BlockGrowEvent` 中，`event.getNewState()` 返回的是即将变更的方块状态（Snapshot）。为了实现跳级（+2 Age），我们采取了以下策略（视具体版本实现微调）：
1.  取消原事件，手动设置方块数据为当前 Age + 2。
2.  或者直接修改 `newState` 的数据并让事件继续（更兼容其他插件）。
*当前实现倾向于：若触发 Bonus，播放特效并尝试修改 newState 或手动设置数据。*

### 5.2 概率计算
统一使用 `ThreadLocalRandom.current().nextDouble()` 进行高效的随机数生成。
*   概率公式: `Random < BaseChance * Multiplier`

### 5.3 联动性
本系统设计为独立运行，但提供了 API 或物品钩子供 **EarthSpirit** (地灵系统) 调用，例如地灵的“嘴馋清单”任务需要提交本系统产出的特产。

---

**文档维护人**: 开发组
**最后更新**: 2025-12-19
