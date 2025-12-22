import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import os
import sys

# Set up Chinese font
font_names = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS']
if os.name == 'nt':
    simhei_path = r'C:\Windows\Fonts\simhei.ttf'
    if os.path.exists(simhei_path):
        fm.fontManager.addfont(simhei_path)
        plt.rcParams['font.sans-serif'] = ['SimHei']
        plt.rcParams['font.family'] = ['sans-serif']
    else:
        plt.rcParams['font.sans-serif'] = font_names
else:
    plt.rcParams['font.sans-serif'] = font_names

plt.rcParams['axes.unicode_minus'] = False

# Read Data
try:
    df = pd.read_csv('drop_rate_data.csv')
except FileNotFoundError:
    print("Error: drop_rate_data.csv not found. Run GenerateDropRateData.java first.")
    sys.exit(1)

# Create Figure
fig = plt.figure(figsize=(18, 10))
fig.suptitle('BiomeGifts 特产掉落率模型全解 (修正版)', fontsize=20, fontweight='bold', y=0.98)
gs = fig.add_gridspec(2, 2, height_ratios=[1, 1])

# --- Plot 1: Gene Impact (Rich Biome Only) ---
ax1 = fig.add_subplot(gs[0, 0])
df_gene = df[df['Scenario'] == 'Gene_Impact']

# Only one line now
ax1.plot(df_gene['ValueX'], df_gene['TotalChance'], 
         marker='o', linewidth=3, color='#E74C3C', label='富集区 (Rich Biome)')

# Annotate min/max
min_pt = df_gene.iloc[0] # 0 star
max_pt = df_gene.iloc[-1] # 5 star

ax1.text(min_pt['ValueX'], min_pt['TotalChance'] + 0.005, f"0星: {min_pt['TotalChance']:.1%}\n(0.75x)", 
         ha='left', va='bottom', fontsize=10, fontweight='bold')
ax1.text(max_pt['ValueX'], max_pt['TotalChance'] - 0.01, f"5星: {max_pt['TotalChance']:.1%}\n(2.0x)", 
         ha='right', va='top', fontsize=10, fontweight='bold')

ax1.set_title('图表 A: 基因星级对掉落率的影响 (仅富集区生效)', fontsize=14)
ax1.set_xlabel('基因产量 (Gene Yield 0-5星)', fontsize=12)
ax1.set_ylabel('掉落概率', fontsize=12)
ax1.grid(True, linestyle='--', alpha=0.6)
ax1.set_ylim(0, 0.25) 
ax1.yaxis.set_major_formatter(plt.FuncFormatter(lambda y, _: '{:.0%}'.format(y)))
ax1.legend()


# --- Plot 2: Fertility Sensitivity (Rich, 5-Star) ---
ax2 = fig.add_subplot(gs[0, 1])
df_fert = df[df['Scenario'] == 'Fert_Impact']

ax2.plot(df_fert['ValueX'], df_fert['TotalChance'], 
         color='#2ECC71', marker='s', linewidth=2, label='肥力加成 (100-150)')

ax2.set_title('图表 B: 土地肥力对满基因作物的额外增幅', fontsize=14)
ax2.set_xlabel('土地肥力 (Fertility)', fontsize=12)
ax2.set_ylabel('总掉落率', fontsize=12)
ax2.grid(True, linestyle='--', alpha=0.6)
ax2.yaxis.set_major_formatter(plt.FuncFormatter(lambda y, _: '{:.1%}'.format(y)))

# Annotate
start = df_fert.iloc[0]
end = df_fert.iloc[-1]
ax2.annotate(f"肥力>100起效\n{start['TotalChance']:.1%}", 
             xy=(start['ValueX'], start['TotalChance']), 
             xytext=(start['ValueX'], start['TotalChance']-0.005),
             arrowprops=dict(arrowstyle='->', color='black'))
ax2.annotate(f"肥力150上限\n{end['TotalChance']:.1%}", 
             xy=(end['ValueX'], end['TotalChance']), 
             xytext=(end['ValueX']-10, end['TotalChance']-0.005),
             arrowprops=dict(arrowstyle='->', color='black'))


# --- Plot 3: Waterfall Breakdown ---
ax3 = fig.add_subplot(gs[1, :]) 
df_stack = df[df['Scenario'] == 'Waterfall']

labels = ['基础概率 (10%)', '+ 5星基因 (x2.0)', '+ 肥力满 (x1.1)', '+ 地灵满 (x1.1*)'] 
# Note: Actually it's (1 + 0.1 + 0.1) = 1.2 total bonus multiplier, so steps are additive in bonus space but result is multiplicative.
# My generated data handles the math correctly.

values = df_stack['TotalChance'].tolist()

# Calculate increments
increments = [values[0]]
for i in range(1, len(values)):
    increments.append(values[i] - values[i-1])

colors = ['#3498DB', '#9B59B6', '#2ECC71', '#E67E22']

for i in range(len(values)):
    if i == 0:
        ax3.bar(i, values[i], color=colors[i], width=0.5)
        ax3.text(i, values[i] + 0.002, f"{values[i]:.1%}", ha='center', fontweight='bold')
    else:
        ax3.bar(i, increments[i], bottom=values[i-1], color=colors[i], width=0.5)
        # Connector
        ax3.plot([i-1.25, i+0.25], [values[i-1], values[i-1]], 'k--', alpha=0.3)
        # Label total
        ax3.text(i, values[i] + 0.002, f"{values[i]:.1%}", ha='center', fontweight='bold')
        # Label increment
        ax3.text(i, values[i-1] + increments[i]/2, f"+{increments[i]:.1%}", 
                 ha='center', color='white', fontweight='bold', fontsize=10)

ax3.set_xticks(range(len(labels)))
ax3.set_xticklabels(labels, fontsize=11)
ax3.set_title('图表 C: 极限掉落率构成 (24% 上限)', fontsize=14)
ax3.set_ylabel('总掉落率', fontsize=12)
ax3.set_ylim(0, 0.30)
ax3.yaxis.set_major_formatter(plt.FuncFormatter(lambda y, _: '{:.0%}'.format(y)))
ax3.grid(axis='y', linestyle='--', alpha=0.5)

# Text Box for Formula
formula_text = (
    "核心公式 (乘法模型):\n"
    "P = Base(Rich) × GeneMult × (1 + FertBonus + SpiritBonus)\n\n"
    "• Base(Rich): 10% (固定配置)\n"
    "• Gene(5星): 2.0x (1.0 + 100%)\n"
    "• Fert(150): +0.1 (系数加成)\n"
    "• Spirit: +0.1 (系数加成)\n\n"
    "计算过程: 0.10 × 2.0 × (1.0 + 0.1 + 0.1)\n"
    "        = 0.20 × 1.2\n"
    "        = 24%"
)
ax3.text(0.02, 0.90, formula_text, transform=ax3.transAxes, va='top', 
         bbox=dict(boxstyle='round', facecolor='#ECF0F1', alpha=0.9), fontsize=12)

plt.tight_layout()
plt.savefig('biome_gifts_drop_rate_v2.png')
print("Chart saved to biome_gifts_drop_rate_v2.png")
