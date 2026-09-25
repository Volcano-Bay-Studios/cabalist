package xyz.volcanobay.cabalist.util;

import java.util.HashMap;
import java.util.Map;

public class WordToNumberConverter {
    private static final Map<String, Double> numberWords = new HashMap<>();

    static {
        numberWords.put("zero", 0.0);
        numberWords.put("one", 1.0);
        numberWords.put("two", 2.0);
        numberWords.put("three", 3.0);
        numberWords.put("four", 4.0);
        numberWords.put("five", 5.0);
        numberWords.put("six", 6.0);
        numberWords.put("seven", 7.0);
        numberWords.put("eight", 8.0);
        numberWords.put("nine", 9.0);
        numberWords.put("ten", 10.0);
        numberWords.put("twenty", 20.0);
        numberWords.put("thirty", 30.0);
        numberWords.put("forty", 40.0);
        numberWords.put("fifty", 50.0);
        numberWords.put("sixty", 60.0);
        numberWords.put("seventy", 70.0);
        numberWords.put("eighty", 80.0);
        numberWords.put("ninety", 90.0);
        numberWords.put("hundred", 100.0);
        numberWords.put("thousand", 1000.0);
    }

    public static double convertPhraseToDouble(String phrase) {
        String[] tokens = phrase.toLowerCase().split("\\s+");
        double total = 0.0;
        double current = 0.0;

        for (String token : tokens) {
            if (!numberWords.containsKey(token)) continue;
            double val = numberWords.get(token);
            if (val == 100.0) {
                current *= val;
            } else if (val >= 1000.0) {
                current *= val;
                total += current;
                current = 0.0;
            } else {
                current += val;
            }
        }
        return total + current;
    }
}
