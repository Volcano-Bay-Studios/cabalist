package xyz.volcanobay.cabalist.system.casting;

import java.util.List;

// The edits a caster makes to what they're writing. Both sides apply them the same way so the caster can predict them.
public final class CastingEdit {
    public static final int TYPE = 0;
    public static final int BACKSPACE = 1;
    public static final int SPACE = 2;

    public static String apply(int edit, String text, List<String> words, String current) {
        switch (edit) {
            case TYPE -> {
                return current + text;
            }
            case BACKSPACE -> {
                if (!current.isEmpty()) {
                    return current.substring(0, current.length() - 1);
                }
                return words.isEmpty() ? "" : words.remove(words.size() - 1);
            }
            case SPACE -> {
                if (isInQuote(current)) {
                    return current + " ";
                }
                if (!current.isEmpty()) {
                    words.add(current);
                }
                return "";
            }
            default -> {
                return current;
            }
        }
    }

    public static boolean isInQuote(String current) {
        char open = 0;
        for (int i = 0; i < current.length(); i++) {
            char character = current.charAt(i);
            if (open == 0 && (character == '"' || character == '\'' && i == 0)) {
                open = character;
            } else if (character == open) {
                open = 0;
            }
        }
        return open != 0;
    }

    public static boolean isTypeable(char character) {
        return character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '"' || character == '\'' || character == '_';
    }
}
