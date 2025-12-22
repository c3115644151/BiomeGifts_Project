import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GenerateDropRateData {

    // Config Constants
    // User specified: Rich Biome Base is 10%. Others are 0%.
    private static final double RICH_BASE_CHANCE = 0.10;

    static class DataPoint {
        String scenario;
        String xLabel; // Generic label for X-axis or Category
        double valueX; // Numeric value for X-axis (if applicable)
        double totalChance;

        public DataPoint(String scenario, String xLabel, double valueX, double totalChance) {
            this.scenario = scenario;
            this.xLabel = xLabel;
            this.valueX = valueX;
            this.totalChance = totalChance;
        }
        
        public String toCSV() {
            return String.format(Locale.US, "%s,%s,%.2f,%.4f", 
                scenario, xLabel, valueX, totalChance);
        }
    }

    public static void main(String[] args) {
        List<DataPoint> data = new ArrayList<>();

        // Scenario 1: Gene Impact (Rich Biome)
        // Var: Gene Yield (2.0 - 9.0)
        // Fixed: Fert=100 (Bonus=0), Spirit=0
        for (double g = 2.0; g <= 9.0; g += 0.5) {
            double chance = calculate(g, 100, 0);
            data.add(new DataPoint("Gene_Impact", "Gene_" + g, g, chance));
        }

        // Scenario 2: Fertility Impact (Rich Biome, Max Gene 9.0)
        // Var: Fertility (100-150)
        // Fixed: Gene=9.0, Spirit=0
        for (double f = 100; f <= 150; f += 2.5) {
            double chance = calculate(9.0, f, 0);
            data.add(new DataPoint("Fert_Impact", "Fert_" + f, f, chance));
        }

        // Scenario 3: Waterfall Breakdown (Maxed Stats)
        // Steps to build up to the max probability
        
        // 1. Base (Rich Biome)
        double base = RICH_BASE_CHANCE;
        data.add(new DataPoint("Waterfall", "1_Base_Config", 0, base));
        
        // 2. Gene Effect (Max 9.0 -> 2.0x)
        // New Chance = Base * GeneMultiplier
        double withGene = calculate(9.0, 100, 0);
        data.add(new DataPoint("Waterfall", "2_Gene_Max", 9, withGene));
        
        // 3. Fertility Effect (Max 150 -> +0.1)
        // New Chance = (Base * Gene) + FertBonus
        double withFert = calculate(9.0, 150, 0);
        data.add(new DataPoint("Waterfall", "3_Fert_Max", 150, withFert));
        
        // 4. Spirit Effect (Max -> +0.1)
        // New Chance = (Base * Gene) + FertBonus + SpiritBonus
        double finalChance = calculate(9.0, 150, 0.1);
        data.add(new DataPoint("Waterfall", "4_Spirit_Max", 0.1, finalChance));

        // Output to CSV
        try (PrintWriter writer = new PrintWriter(new FileWriter("drop_rate_data.csv"))) {
            writer.println("Scenario,Label,ValueX,TotalChance");
            for (DataPoint dp : data) {
                writer.println(dp.toCSV());
            }
            System.out.println("Data generated successfully: drop_rate_data.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculate(double geneYield, double fertility, double spiritBonus) {
        // Only calculating for RICH biome as requested (others are 0)
        double base = RICH_BASE_CHANCE;
        
        // Gene Multiplier
        // Range: 2.0 (Wild) ~ 9.0 (Max)
        // Logic: (Yield * 1.25 - 4.25) / 7.0
        // 2.0 -> -0.25
        // 9.0 -> +1.0
        double bonusPercent = (geneYield * 1.25 - 4.25) / 7.0;
        double geneMultiplier = 1.0 + bonusPercent;
        if (geneMultiplier < 0.0) geneMultiplier = 0.0;
        
        // Fertility Bonus
        // >100 only. (Max 150 -> +0.10)
        // Formula: (Fert - 100) * 0.002
        double fertilityBonus = 0.0;
        if (fertility > 100) {
            fertilityBonus = (fertility - 100.0) * 0.002;
        }
        
        // Total
        double total = (base * geneMultiplier) + fertilityBonus + spiritBonus;
        return total;
    }
}
