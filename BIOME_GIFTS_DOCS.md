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
包含两个核心部分：生长数据提供与收割奖励。

#### 3.3.1 生长干预 (联动机制)
*   **旧机制**：直接监听 `BlockGrowEvent` 并根据群系取消事件（减速）或跳级生长（加速）。
*   **新机制 (CuisineFarming 联动)**：
    *   插件会检测服务器是否安装了 **CuisineFarming (耕食传说)** 插件。
    *   **若存在 CuisineFarming**：`CropListener` 即使监听到生长事件，也**不再主动干预**（不做任何取消或加速操作）。它转而作为**数据提供者**，将当前群系的 `richSpeedBonus`（正值）或 `poorSpeedPenalty`（负值）通过配置供 CuisineFarming 读取。
    *   **计算公式**：CuisineFarming 会将本插件提供的加成/惩罚值直接累加到其统一的效率公式中（例如：贫瘠惩罚 0.3 会被视为 -0.3 效率）。
    *   **若不存在 CuisineFarming**：保留原有的独立逻辑（如下所述），作为后备方案运行。

#### 3.3.2 独立模式下的生长逻辑 (Legacy)
（仅当 CuisineFarming 未安装时生效）
*   **Poor 区域**: 触发 `poorSpeedPenalty` 概率判定，若命中则 `event.setCancelled(true)`，实现**生长减缓**。
*   **Rich 区域**: 触发 `richSpeedBonus` 概率判定，若命中：
    1.  播放 `HAPPY_VILLAGER` 粒子。
    2.  **跳级生长**: 在原版生长 (+1 age) 的基础上，额外再 +1 age，实现**生长加速**。

#### 3.3.3 收割奖励 (`BlockBreakEvent`)
*   **条件**: 作物必须完全成熟 (`Age == MaxAge`)。
*   **统一掉落机制 (Unified Drop System)**: 
    *   本插件作为**核心执行者**，接管了所有相关插件的特产掉落计算。
    *   **公式**: `TotalChance = BaseChance (Biome) + FertilityBonus (Cuisine) + SpiritBonus (Spirit)`
    *   这意味着不再有独立的“肥力掉落”或“地灵掉落”，所有加成统一汇总为单次判定，避免了多次独立判定导致的概率分布不均或“一次掉三份”的问题。
*   **特殊机制**: 如果该作物被标记为 `SpecialCrop` (由灵契之种种植)，则**100%掉落**高阶特产，并移除特殊标记。

### 3.4 特殊作物管理 (`SpecialCropManager.java`)
*   **功能**: 允许特定位置的作物拥有特殊属性（如必定掉落奖励）。
*   **持久化**: 使用 `special_crops.yml` 存储坐标 (`World,X,Y,Z`)，确保服务器重启后特殊作物状态不丢失。
*   **应用场景**: 地灵系统每日任务奖励的“灵契之种”，种植后即被记录为特殊作物。

### 3.5 指令系统 (`BiomeGiftsCommand.java`)
*   **指令**: `/getgift <物品ID>`
*   **权限**: `biomegifts.admin`
*   **功能**: 管理员可以直接获取指定的特产物品。
*   **特性**:
    *   支持 Tab 键自动补全所有可用的物品 ID（如 `GOLDEN_WHEAT`, `COPPER_CRYSTAL` 等）。
    *   支持输入 `ALL` (如 `/getgift ALL`) 一键获取所有特产物品，方便测试。

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
1.  **世界生成**: Terra 读取对应其插件目录中的配置。
    *   在 **Rich Biomes** 生成时，加载 `ORES_RICH` 配置，矿物生成密度增加。
    *   在 **Poor Biomes** 生成时，加载 `ORES_POOR` 配置，矿物生成密度降低。
2.  **游戏内交互**: 
    *   玩家进入该区域，`BiomeGifts` 插件识别当前群系。
    *   挖掘矿物时，插件再次根据群系进行**二次判定**（掉落自定义物品）。

**总结**: Terra 负责**“量”**（矿有多少），BiomeGifts 插件负责**“质”**（掉什么特殊物品）。两者结合构成了完整的地域馈赠体验。

---

## 5. 技术细节备忘 (Technical Notes)

### 5.1 生长跳级实现
*   **联动模式**: 本插件仅提供 `richSpeedBonus` 或 `poorSpeedPenalty` 数值，具体的加速（补课）或减速（拦截）逻辑完全由 **CuisineFarming** 接管。
*   **独立模式**: 
    *   在 `BlockGrowEvent` 中，`event.getNewState()` 返回的是即将变更的方块状态（Snapshot）。
    *   跳级实现：手动设置方块数据为当前 Age + 2，并播放粒子效果。

### 5.2 概率计算
统一使用 `ThreadLocalRandom.current().nextDouble()` 进行高效的随机数生成。
*   概率公式: `Random < BaseChance * Multiplier`

### 5.3 联动性
*   **CuisineFarming (耕食传说)**: 深度集成。CuisineFarming 通过反射读取本插件的 `ConfigManager`，获取当前位置的群系加成/减罚值，统一纳入效率计算。
*   **EarthSpirit (地灵系统)**: 提供物品钩子。地灵的“嘴馋清单”任务需要提交本系统产出的特产。

---

## 6. 附录：特产物品清单 (Appendix: Item List)

### 矿业特产 (Ores)
| ID | 名称 | 原料 | 掉落来源 | 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `LIGNITE` | 褐煤 | 煤炭 | 沼泽/雨林 | 质地疏松的煤炭，燃烧效率一般。 |
| `RICH_SLAG` | 富铁 | 铁粒 | 热带草原/石林 | 冶炼失败的产物，但含有极高的铁元素。 |
| `GOLD_DUST` | 砂金 | 金粒 | 恶地/河流 | 河床冲刷出的细小金粒。 |
| `CHARGED_DUST` | 充能尘埃 | 红石 | 沙漠 | 吸收了静电的红石粉，能量极高。 |
| `ICE_SHARD` | 永冻冰晶 | 钻石 | 雪山/冰刺 | 在极寒之地与钻石伴生的冰晶。 |
| `TIDE_ESSENCE` | 洋流精粹 | 青金石 | 海洋/深海 | 凝结了海潮之力的蓝色宝石。 |
| `COPPER_CRYSTAL` | 孔雀石晶体 | 粗铜 | 溶洞/河流 | 铜在特定环境下结晶出的伴生宝石。 |
| `JADE_SHARD` | 高山翠玉 | 绿宝石 | 高山/山地 | 比绿宝石更纯净，带有东方韵味的玉石原石。 |
| `ECHO_SHARD` | 共鸣晶簇 | 紫水晶碎片 | 繁茂洞穴/溶洞 | 发着微光的紫色尖刺状晶体。 |
| `SOUL_SHARD` | 灵魂玻片 | 石英 | 下界荒原 | 苍白半透明的薄片，封印着扭曲的灵魂面孔。 |
| `VULCAN_SCALE` | 火神之鳞 | 远古残骸 | 玄武岩三角洲 | 暗金色带有熔岩裂纹的鳞片状金属。 |

### 农业特产 (Crops)
| ID | 名称 | 原料 | 掉落来源 | 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `GOLDEN_WHEAT` | 黄金麦穗 | 小麦 | 平原/森林 | 只有温带的阳光才能晒出的饱满谷物。 |
| `WATER_GEL` | 储水凝胶 | 粘液球 | 沙漠/恶地 (仙人掌) | 沙漠植物为了生存进化出的精华。 |
| `TROPICAL_NECTAR` | 热带糖蜜 | 蜂蜜瓶 | 雨林/沼泽 (西瓜) | 湿热环境下积累的高糖分蜜露。 |
| `FROST_BERRY` | 霜糖果实 | 甜浆果 | 针叶林/雪原 | 经霜之后变得异常甜美的果实。 |
| `TERRA_POTATO` | 大地之心薯 | 马铃薯 | 平原/山地 | 吸收了大地精华的完美块茎。 |
| `RUBY_CARROT` | 红玉胡萝卜 | 胡萝卜 | 森林/平原 | 通体晶莹剔透，富含维生素。 |
| `CRIMSON_ESSENCE` | 深红血精 | 甜菜根 | 针叶林/雪原 | 饱满的深红色球体，提纯后的生命精华。 |
| `GLOW_PUMPKIN` | 辉光南瓜瓤 | 南瓜种子 | 平原/森林 | 吸收了日照的精华，自身发光。 |
| `CRYSTAL_CANE` | 水晶糖柱 | 甘蔗 | 河流/沼泽 | 像玻璃棒一样透明，糖分饱和度200%。 |
| `DARK_COCOA` | 醇黑可可脂 | 可可豆 | 丛林 | 顶级巧克力的原材料。 |
| `JADE_SHOOT` | 翠玉竹笋 | 竹子 | 竹林/丛林 | 质感完全是玉石的短小竹笋。 |
| `IODINE_CRYSTAL` | 深海碘晶 | 干海带 | 海洋 | 来自深海的馈赠，炼金术的重要材料。 |
| `DOOM_SPORE` | 厄运孢子簇 | 地狱疣 | 下界 | 炼制邪恶药水的核心。 |
| `CAVE_PEARL` | 洞穴夜明珠 | 发光浆果 | 繁茂洞穴 | 繁茂洞穴的照明之源。 |
| `VOID_CORE` | 虚空回响核 | 紫颂果 | 末地 | 形状破碎的几何体，蕴含虚空能量。 |
| `SPIRIT_WHEAT_SEEDS`| 灵契之种 | 小麦种子 | 特殊任务 | 地灵赠予的特殊种子，成熟后必结出黄金麦穗。 |

---

**文档维护人**: 开发组
**最后更新**: 2025-12-20
