package com.spadeboot.api.dto.request;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ChipInventoryDto {
    private int chip1;
    private int chip5;
    private int chip10;
    private int chip25;
    private int chip100;
    private int chip500;
    private int targetValue = 1000; // Default target value

    public Map<String, Integer> getChipValues() {
        Map<String, Integer> chipValues = new HashMap<>();
        chipValues.put("chip1", 1);
        chipValues.put("chip5", 5);
        chipValues.put("chip10", 10);
        chipValues.put("chip25", 25);
        chipValues.put("chip100", 100);
        chipValues.put("chip500", 500);
        return chipValues;
    }

    public Map<String, Integer> getChipAvailable() {
        Map<String, Integer> chipAvailable = new HashMap<>();
        chipAvailable.put("chip1", chip1);
        chipAvailable.put("chip5", chip5);
        chipAvailable.put("chip10", chip10);
        chipAvailable.put("chip25", chip25);
        chipAvailable.put("chip100", chip100);
        chipAvailable.put("chip500", chip500);
        return chipAvailable;
    }
}
