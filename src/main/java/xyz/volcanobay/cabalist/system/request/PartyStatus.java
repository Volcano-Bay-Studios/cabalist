package xyz.volcanobay.cabalist.system.request;

import java.util.Locale;

public enum PartyStatus {
    UNANSWERED,
    YES,
    NO,
    BURNED,
    DISPELLED;

    public boolean isRefusal() {
        return this == NO || this == BURNED || this == DISPELLED;
    }

    public String getStyleName() {
        return "party/" + name().toLowerCase(Locale.ROOT);
    }
}
