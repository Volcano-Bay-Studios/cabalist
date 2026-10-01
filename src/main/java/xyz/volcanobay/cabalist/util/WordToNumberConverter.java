package xyz.volcanobay.cabalist.util;

import java.util.HashMap;
import java.util.Map;

/**
 * It uhhh converts words to numbers.
 */
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
        numberWords.put("eleven", 11.0);
        numberWords.put("twelve", 12.0);
        numberWords.put("thirteen", 13.0);
        numberWords.put("fourteen", 14.0);
        numberWords.put("fifteen", 15.0);
        numberWords.put("sixteen", 16.0);
        numberWords.put("seventeen", 17.0);
        numberWords.put("eighteen", 18.0);
        numberWords.put("nineteen", 19.0);
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

    public static boolean isNumberWord(String token) {
        return getValue(token) != null;
    }

    public static boolean isMultiplierWord(String token) {
        Double value = numberWords.get(token);
        return value != null && value >= 100.0;
    }

    private static Double getValue(String token) {
        Double value = numberWords.get(token);
        if (value != null) {
            return value;
        }
        if (isDigits(token)) {
            return Double.parseDouble(token);
        }
        return null;
    }

    private static boolean isDigits(String token) {
        if (token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns NaN if the phrase contains no number words.
     */
    public static double convertPhraseToDouble(String phrase) {
        String[] tokens = phrase.toLowerCase().split("\\s+");
        double total = 0.0;
        double current = 0.0;
        boolean hasCurrent = false;
        boolean hasNumber = false;

        for (String token : tokens) {
            Double value = getValue(token);
            if (value == null) {
                continue;
            }
            hasNumber = true;
            if (isMultiplierWord(token)) {
                if (!hasCurrent) {
                    current = 1.0;
                }
                current *= value;
                hasCurrent = true;
                if (value >= 1000.0) {
                    total += current;
                    current = 0.0;
                    hasCurrent = false;
                }
            } else {
                current += value;
                hasCurrent = true;
            }
        }
        if (!hasNumber) {
            return Double.NaN;
        }
        return total + current;
    }
}
