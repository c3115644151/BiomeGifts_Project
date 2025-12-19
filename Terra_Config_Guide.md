# Terra 矿物生成配置指南

本指南协助您使用 Terra 插件实现“富集区”和“贫瘠区”的矿物生成率控制 (120% / 80%)。

## 1. 原理说明
Terra 通过 `features` 定义矿物生成，通过 `biomes` 引用这些 features。
要实现不同生成率，我们需要：
1. 创建新的矿物 Feature (如 `DIAMOND_ORE_RICH`)，在其中修改密度阈值。
2. 创建新的矿物列表 (如 `ORES_RICH`)，包含这些新的 Feature。
3. 在群系配置 (如 `plains.yml`) 中，引用 `ORES_RICH` 而非 `ORES_DEFAULT`。

## 2. 模板文件
已为您创建 `terra_templates` 文件夹，包含以下示例：

### 2.1 矿物 Feature (`terra_templates/features/`)
- `diamond_ore_rich.yml`: 钻石矿富集版 (1.2倍生成率)
- `diamond_ore_poor.yml`: 钻石矿贫瘠版 (0.8倍生成率)

**修改原理**:
在 `anchors` 部分，我们修改了 `densityThreshold` 的计算公式：
```yaml
# 富集区 (1.2倍)
- &densityThreshold 1/256 * ${features/deposits/distribution.yml:diamond.averageCountPerChunk} * 1.2
```

### 2.2 矿物列表 (`terra_templates/`)
- `ores_rich.yml`: 包含 `DIAMOND_ORE_RICH` 的列表。
- `ores_poor.yml`: 包含 `DIAMOND_ORE_POOR` 的列表。

## 3. 操作步骤

### 第一步：复制文件
将 `terra_templates` 中的文件复制到 Terra 配置包 (`packs/homeworld`) 的对应位置：
1. `features/diamond_ore_rich.yml` -> `packs/homeworld/features/deposits/ores/diamond_ore_rich.yml`
2. `features/diamond_ore_poor.yml` -> `packs/homeworld/features/deposits/ores/diamond_ore_poor.yml`
3. `ores_rich.yml` -> `packs/homeworld/biomes/abstract/features/ores/ores_rich.yml`
4. `ores_poor.yml` -> `packs/homeworld/biomes/abstract/features/ores/ores_poor.yml`

*(注意：您需要为其他矿物（煤、铁等）重复创建类似的 Rich/Poor Feature 文件)*

### 第二步：修改群系配置
打开您想要设为“富集区”的群系配置文件 (例如 `packs/homeworld/biomes/land/temperate/semi-arid/plains.yml`)。

找到引用矿物的地方 (通常继承自 `base.yml` 或直接引用 `ORES_DEFAULT`)。
修改为引用 `ORES_RICH`。

示例 (`plains.yml`):
```yaml
extends:
  - ...
  - ABSTRACT_BIOME
  - ORES_RICH  # 替换原来的 ORES_DEFAULT
```

### 第三步：重载 Terra
在服务器运行 `terra reload` 或重启服务器。

---

## 关于 BiomeGifts 插件
BiomeGifts 插件负责 **挖掘掉落 (BlockBreakEvent)** 和 **作物生长控制**。
- 无论 Terra 生成了多少矿物，BiomeGifts 都会根据 `config.yml` 中的概率掉落特产。
- 目前已修复小麦掉落率为 0 的问题 (配置项名称错误修正)。
- 已消除控制台关于非矿物方块的误报日志。
