// src/main/java/com/pokerapp/service/spadehub/CheatsheetService.java
package com.spadeboot.service.spadehub;

import com.gurobi.gurobi.*;
import com.spadeboot.api.dto.request.ChipInventoryDto;
import com.spadeboot.api.dto.response.ChipDistributionDto;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Getter
@Service
public class CheatsheetService {

    private List<HeatmapDataPoint> heatmapData = new ArrayList<>();

    @PostConstruct
    public void initializeHeatmapData() {
        this.heatmapData = createHeatmapData();
    }

    private List<HeatmapDataPoint> createHeatmapData() {
        List<HeatmapDataPoint> data = new ArrayList<>();
        data.add(new HeatmapDataPoint("A", "A", 0.85, "\n 0"));
        data.add(new HeatmapDataPoint("A", "K", 0.65, "o\n 5"));
        data.add(new HeatmapDataPoint("A", "Q", 0.64, "o\n 8"));
        data.add(new HeatmapDataPoint("A", "J", 0.64, "o\n 12"));
        data.add(new HeatmapDataPoint("A", "T", 0.63, "o\n 18"));
        data.add(new HeatmapDataPoint("A", "9", 0.61, "o\n 32"));
        data.add(new HeatmapDataPoint("A", "8", 0.6, "o\n 39"));
        data.add(new HeatmapDataPoint("A", "7", 0.59, "o\n 45"));
        data.add(new HeatmapDataPoint("A", "6", 0.58, "o\n 51"));
        data.add(new HeatmapDataPoint("A", "5", 0.58, "o\n 44"));
        data.add(new HeatmapDataPoint("A", "4", 0.57, "o\n 46"));
        data.add(new HeatmapDataPoint("A", "3", 0.56, "o\n 49"));
        data.add(new HeatmapDataPoint("A", "2", 0.55, "o\n 54"));

        data.add(new HeatmapDataPoint("K", "A", 0.76, "s\n 2"));
        data.add(new HeatmapDataPoint("K", "K", 0.82, "\n 1"));
        data.add(new HeatmapDataPoint("K", "Q", 0.61, "o\n 9"));
        data.add(new HeatmapDataPoint("K", "J", 0.61, "o\n 14"));
        data.add(new HeatmapDataPoint("K", "T", 0.59, "o\n 20"));
        data.add(new HeatmapDataPoint("K", "9", 0.58, "o\n 35"));
        data.add(new HeatmapDataPoint("K", "8", 0.56, "o\n 50"));
        data.add(new HeatmapDataPoint("K", "7", 0.55, "o\n 57"));
        data.add(new HeatmapDataPoint("K", "6", 0.54, "o\n 60"));
        data.add(new HeatmapDataPoint("K", "5", 0.53, "o\n 63"));
        data.add(new HeatmapDataPoint("K", "4", 0.52, "o\n 67"));
        data.add(new HeatmapDataPoint("K", "3", 0.51, "o\n 67"));
        data.add(new HeatmapDataPoint("K", "2", 0.51, "o\n 69"));

        data.add(new HeatmapDataPoint("Q", "A", 0.66, "s\n 2"));
        data.add(new HeatmapDataPoint("Q", "K", 0.63, "s\n 3"));
        data.add(new HeatmapDataPoint("Q", "Q", 0.8, "\n 1"));
        data.add(new HeatmapDataPoint("Q", "J", 0.58, "o\n 15"));
        data.add(new HeatmapDataPoint("Q", "T", 0.57, "o\n 22"));
        data.add(new HeatmapDataPoint("Q", "9", 0.55, "o\n 36"));
        data.add(new HeatmapDataPoint("Q", "8", 0.54, "o\n 53"));
        data.add(new HeatmapDataPoint("Q", "7", 0.52, "o\n 66"));
        data.add(new HeatmapDataPoint("Q", "6", 0.51, "o\n 71"));
        data.add(new HeatmapDataPoint("Q", "5", 0.5, "o\n 75"));
        data.add(new HeatmapDataPoint("Q", "4", 0.49, "o\n 76"));
        data.add(new HeatmapDataPoint("Q", "3", 0.48, "o\n 77"));
        data.add(new HeatmapDataPoint("Q", "2", 0.47, "o\n 79"));

        data.add(new HeatmapDataPoint("J", "A", 0.65, "s\n 3"));
        data.add(new HeatmapDataPoint("J", "K", 0.63, "s\n 3"));
        data.add(new HeatmapDataPoint("J", "Q", 0.6, "s\n 5"));
        data.add(new HeatmapDataPoint("J", "J", 0.77, "\n 2"));
        data.add(new HeatmapDataPoint("J", "T", 0.55, "o\n 21"));
        data.add(new HeatmapDataPoint("J", "9", 0.53, "o\n 34"));
        data.add(new HeatmapDataPoint("J", "8", 0.51, "o\n 48"));
        data.add(new HeatmapDataPoint("J", "7", 0.5, "o\n 64"));
        data.add(new HeatmapDataPoint("J", "6", 0.48, "o\n 80"));
        data.add(new HeatmapDataPoint("J", "5", 0.47, "o\n 82"));
        data.add(new HeatmapDataPoint("J", "4", 0.46, "o\n 85"));
        data.add(new HeatmapDataPoint("J", "3", 0.45, "o\n 86"));
        data.add(new HeatmapDataPoint("J", "2", 0.44, "o\n 87"));

        data.add(new HeatmapDataPoint("T", "A", 0.65, "s\n 5"));
        data.add(new HeatmapDataPoint("T", "K", 0.62, "s\n 6"));
        data.add(new HeatmapDataPoint("T", "Q", 0.59, "s\n 6"));
        data.add(new HeatmapDataPoint("T", "J", 0.58, "s\n 6"));
        data.add(new HeatmapDataPoint("T", "T", 0.75, "\n 4"));
        data.add(new HeatmapDataPoint("T", "9", 0.52, "o\n 31"));
        data.add(new HeatmapDataPoint("T", "8", 0.5, "o\n 43"));
        data.add(new HeatmapDataPoint("T", "7", 0.48, "o\n 59"));
        data.add(new HeatmapDataPoint("T", "6", 0.46, "o\n 74"));
        data.add(new HeatmapDataPoint("T", "5", 0.44, "o\n 89"));
        data.add(new HeatmapDataPoint("T", "4", 0.44, "o\n 90"));
        data.add(new HeatmapDataPoint("T", "3", 0.43, "o\n 92"));
        data.add(new HeatmapDataPoint("T", "2", 0.42, "o\n 94"));

        data.add(new HeatmapDataPoint("9", "A", 0.63, "s\n 8"));
        data.add(new HeatmapDataPoint("9", "K", 0.6, "s\n 10"));
        data.add(new HeatmapDataPoint("9", "Q", 0.58, "s\n 10"));
        data.add(new HeatmapDataPoint("9", "J", 0.56, "s\n 11"));
        data.add(new HeatmapDataPoint("9", "T", 0.54, "s\n 10"));
        data.add(new HeatmapDataPoint("9", "9", 0.72, "\n 7"));
        data.add(new HeatmapDataPoint("9", "8", 0.48, "o\n 42"));
        data.add(new HeatmapDataPoint("9", "7", 0.46, "o\n 55"));
        data.add(new HeatmapDataPoint("9", "6", 0.44, "o\n 68"));
        data.add(new HeatmapDataPoint("9", "5", 0.43, "o\n 83"));
        data.add(new HeatmapDataPoint("9", "4", 0.41, "o\n 95"));
        data.add(new HeatmapDataPoint("9", "3", 0.4, "o\n 96"));
        data.add(new HeatmapDataPoint("9", "2", 0.39, "o\n 97"));

        data.add(new HeatmapDataPoint("8", "A", 0.62, "s\n 10"));
        data.add(new HeatmapDataPoint("8", "K", 0.58, "s\n 16"));
        data.add(new HeatmapDataPoint("8", "Q", 0.56, "s\n 19"));
        data.add(new HeatmapDataPoint("8", "J", 0.54, "s\n 17"));
        data.add(new HeatmapDataPoint("8", "T", 0.52, "s\n 16"));
        data.add(new HeatmapDataPoint("8", "9", 0.51, "s\n 17"));
        data.add(new HeatmapDataPoint("8", "8", 0.69, "\n 9"));
        data.add(new HeatmapDataPoint("8", "7", 0.45, "o\n 52"));
        data.add(new HeatmapDataPoint("8", "6", 0.43, "o\n 61"));
        data.add(new HeatmapDataPoint("8", "5", 0.41, "o\n 73"));
        data.add(new HeatmapDataPoint("8", "4", 0.39, "o\n 88"));
        data.add(new HeatmapDataPoint("8", "3", 0.37, "o\n 98"));
        data.add(new HeatmapDataPoint("8", "2", 0.37, "o\n 99"));

        data.add(new HeatmapDataPoint("7", "A", 0.61, "s\n 13"));
        data.add(new HeatmapDataPoint("7", "K", 0.58, "s\n 19"));
        data.add(new HeatmapDataPoint("7", "Q", 0.54, "s\n 26"));
        data.add(new HeatmapDataPoint("7", "J", 0.52, "s\n 27"));
        data.add(new HeatmapDataPoint("7", "T", 0.51, "s\n 25"));
        data.add(new HeatmapDataPoint("7", "9", 0.49, "s\n 24"));
        data.add(new HeatmapDataPoint("7", "8", 0.48, "s\n 21"));
        data.add(new HeatmapDataPoint("7", "7", 0.66, "\n 12"));
        data.add(new HeatmapDataPoint("7", "6", 0.42, "o\n 57"));
        data.add(new HeatmapDataPoint("7", "5", 0.41, "o\n 65"));
        data.add(new HeatmapDataPoint("7", "4", 0.39, "o\n 78"));
        data.add(new HeatmapDataPoint("7", "3", 0.37, "o\n 93"));
        data.add(new HeatmapDataPoint("7", "2", 0.31, "o\n 100"));

        data.add(new HeatmapDataPoint("6", "A", 0.6, "s\n 14"));
        data.add(new HeatmapDataPoint("6", "K", 0.57, "s\n 24"));
        data.add(new HeatmapDataPoint("6", "Q", 0.54, "s\n 28"));
        data.add(new HeatmapDataPoint("6", "J", 0.51, "s\n 33"));
        data.add(new HeatmapDataPoint("6", "T", 0.49, "s\n 31"));
        data.add(new HeatmapDataPoint("6", "9", 0.47, "s\n 29"));
        data.add(new HeatmapDataPoint("6", "8", 0.46, "s\n 27"));
        data.add(new HeatmapDataPoint("6", "7", 0.45, "s\n 25"));
        data.add(new HeatmapDataPoint("6", "6", 0.63, "\n 16"));
        data.add(new HeatmapDataPoint("6", "5", 0.4, "o\n 58"));
        data.add(new HeatmapDataPoint("6", "4", 0.38, "o\n 70"));
        data.add(new HeatmapDataPoint("6", "3", 0.36, "o\n 81"));
        data.add(new HeatmapDataPoint("6", "2", 0.32, "o\n 95"));

        data.add(new HeatmapDataPoint("5", "A", 0.6, "s\n 12"));
        data.add(new HeatmapDataPoint("5", "K", 0.56, "s\n 25"));
        data.add(new HeatmapDataPoint("5", "Q", 0.53, "s\n 29"));
        data.add(new HeatmapDataPoint("5", "J", 0.5, "s\n 35"));
        data.add(new HeatmapDataPoint("5", "T", 0.47, "s\n 40"));
        data.add(new HeatmapDataPoint("5", "9", 0.46, "s\n 38"));
        data.add(new HeatmapDataPoint("5", "8", 0.45, "s\n 33"));
        data.add(new HeatmapDataPoint("5", "7", 0.44, "s\n 28"));
        data.add(new HeatmapDataPoint("5", "6", 0.43, "s\n 27"));
        data.add(new HeatmapDataPoint("5", "5", 0.6, "\n 20"));
        data.add(new HeatmapDataPoint("5", "4", 0.38, "o\n 62"));
        data.add(new HeatmapDataPoint("5", "3", 0.36, "o\n 72"));
        data.add(new HeatmapDataPoint("5", "2", 0.34, "o\n 84"));

        data.add(new HeatmapDataPoint("4", "A", 0.59, "s\n 14"));
        data.add(new HeatmapDataPoint("4", "K", 0.55, "s\n 25"));
        data.add(new HeatmapDataPoint("4", "Q", 0.52, "s\n 29"));
        data.add(new HeatmapDataPoint("4", "J", 0.49, "s\n 37"));
        data.add(new HeatmapDataPoint("4", "T", 0.47, "s\n 40"));
        data.add(new HeatmapDataPoint("4", "9", 0.44, "s\n 47"));
        data.add(new HeatmapDataPoint("4", "8", 0.43, "s\n 40"));
        data.add(new HeatmapDataPoint("4", "7", 0.42, "s\n 37"));
        data.add(new HeatmapDataPoint("4", "6", 0.41, "s\n 29"));
        data.add(new HeatmapDataPoint("4", "5", 0.41, "s\n 28"));
        data.add(new HeatmapDataPoint("4", "4", 0.57, "\n 23"));
        data.add(new HeatmapDataPoint("4", "3", 0.35, "o\n 76"));
        data.add(new HeatmapDataPoint("4", "2", 0.33, "o\n 86"));

        data.add(new HeatmapDataPoint("3", "A", 0.58, "s\n 14"));
        data.add(new HeatmapDataPoint("3", "K", 0.54, "s\n 26"));
        data.add(new HeatmapDataPoint("3", "Q", 0.51, "s\n 30"));
        data.add(new HeatmapDataPoint("3", "J", 0.48, "s\n 37"));
        data.add(new HeatmapDataPoint("3", "T", 0.46, "s\n 41"));
        data.add(new HeatmapDataPoint("3", "9", 0.43, "s\n 47"));
        data.add(new HeatmapDataPoint("3", "8", 0.41, "s\n 53"));
        data.add(new HeatmapDataPoint("3", "7", 0.4, "s\n 45"));
        data.add(new HeatmapDataPoint("3", "6", 0.4, "s\n 38"));
        data.add(new HeatmapDataPoint("3", "5", 0.4, "s\n 32"));
        data.add(new HeatmapDataPoint("3", "4", 0.39, "s\n 36"));
        data.add(new HeatmapDataPoint("3", "3", 0.54, "\n 23"));
        data.add(new HeatmapDataPoint("3", "2", 0.35, "o\n 91"));

        data.add(new HeatmapDataPoint("2", "A", 0.57, "s\n 17"));
        data.add(new HeatmapDataPoint("2", "K", 0.53, "s\n 26"));
        data.add(new HeatmapDataPoint("2", "Q", 0.5, "s\n 31"));
        data.add(new HeatmapDataPoint("2", "J", 0.47, "s\n 38"));
        data.add(new HeatmapDataPoint("2", "T", 0.45, "s\n 41"));
        data.add(new HeatmapDataPoint("2", "9", 0.42, "s\n 49"));
        data.add(new HeatmapDataPoint("2", "8", 0.4, "s\n 54"));
        data.add(new HeatmapDataPoint("2", "7", 0.38, "s\n 56"));
        data.add(new HeatmapDataPoint("2", "6", 0.38, "s\n 49"));
        data.add(new HeatmapDataPoint("2", "5", 0.38, "s\n 39"));
        data.add(new HeatmapDataPoint("2", "4", 0.37, "s\n 41"));
        data.add(new HeatmapDataPoint("2", "3", 0.36, "s\n 46"));
        data.add(new HeatmapDataPoint("2", "2", 0.5, "\n 24"));

        return data;
    }


    public ChipDistributionDto calculateOptimalChipDistribution(ChipInventoryDto dto) {
        try {
            Map<String, Integer> chipValues = dto.getChipValues();
            Map<String, Integer> chipAvailable = dto.getChipAvailable();
            int targetValue = dto.getTargetValue();

            int bestN = 0;
            Map<String, Integer> bestDistribution = new HashMap<>();

            for (int n = 1; n <= 50; n++) {
                GRBEnv env = new GRBEnv(true);
                env.set("logFile", "");
                env.set(GRB.IntParam.OutputFlag, 0); // suppress output
                env.start();

                GRBModel model = new GRBModel(env);

                Map<String, GRBVar> xVars = new HashMap<>();
                for (String chip : chipValues.keySet()) {
                    xVars.put(chip, model.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, chip));
                }

                // Constraint: total value per player = targetValue
                GRBLinExpr totalValue = new GRBLinExpr();
                for (String chip : chipValues.keySet()) {
                    totalValue.addTerm(chipValues.get(chip), xVars.get(chip));
                }
                model.addConstr(totalValue, GRB.EQUAL, targetValue, "target_value");

                // Constraint: total chips used must not exceed availability
                for (String chip : chipValues.keySet()) {
                    GRBLinExpr usedChips = new GRBLinExpr();
                    usedChips.addTerm(n, xVars.get(chip));
                    model.addConstr(usedChips, GRB.LESS_EQUAL, chipAvailable.get(chip), "avail_" + chip);
                }

                // NEW CONSTRAINT: each player should get at least 10 chips in total
                GRBLinExpr totalChipCount = new GRBLinExpr();
                for (String chip : chipValues.keySet()) {
                    totalChipCount.addTerm(1.0, xVars.get(chip));
                }
                model.addConstr(totalChipCount, GRB.GREATER_EQUAL, 10, "min_chips_per_player");

                model.setObjective(new GRBLinExpr(), GRB.MAXIMIZE);
                model.optimize();

                if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                    bestN = n;
                    bestDistribution.clear();
                    for (String chip : xVars.keySet()) {
                        bestDistribution.put(chip, (int) Math.round(xVars.get(chip).get(GRB.DoubleAttr.X)));
                    }
                } else {
                    break;
                }

                model.dispose();
                env.dispose();
            }

            ChipDistributionDto result = new ChipDistributionDto();
            result.setMaxPlayers(bestN);
            result.setDistribution(bestDistribution);
            result.setTargetValuePerPlayer(targetValue);
            return result;

        } catch (GRBException e) {
            throw new RuntimeException("Gurobi optimization failed", e);
        }
    }

    // Data classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapDataPoint {
        private String x;
        private String y;
        private Double heat;
        private String symbol;
    }
}