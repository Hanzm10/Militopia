package com.militopia.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * implementation of Counting Sort specifically tailored for 
 * sorting economy breakdown maps (String keys, Integer values).
 */
public class CountingSort {

    /**
     * Sorts a map of (String, Integer) by value in ascending order.
     * 
     * @param map The input map to sort (e.g., income breakdown).
     * @return A new LinkedHashMap with entries sorted by value (smallest first).
     */
    public static LinkedHashMap<String, Integer> sortEconomyMap(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return new LinkedHashMap<>(map != null ? map : new java.util.HashMap<String, Integer>());
        }

        // 1. Find min and max to determine the range for the counting array
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int value : map.values()) {
            if (value < min) min = value;
            if (value > max) max = value;
        }

        int range = max - min + 1;
        
        // 2. Create frequency "buckets" (lists of keys) for each value in the range.
        // We use an ArrayList of Lists to avoid generic array creation issues.
        List<List<String>> buckets = new ArrayList<List<String>>(range);
        for (int i = 0; i < range; i++) {
            buckets.add(new ArrayList<String>());
        }

        // 3. Distribute keys into buckets based on their values
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            buckets.get(entry.getValue() - min).add(entry.getKey());
        }

        // 4. Reconstruct the map by iterating through the buckets in order
        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < range; i++) {
            List<String> keysInBucket = buckets.get(i);
            for (String key : keysInBucket) {
                sortedMap.put(key, i + min);
            }
        }

        return sortedMap;
    }
}
