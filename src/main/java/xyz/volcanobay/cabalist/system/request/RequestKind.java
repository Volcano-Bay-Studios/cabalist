package xyz.volcanobay.cabalist.system.request;

import java.util.Locale;

public enum RequestKind {
    JOIN("join", "invite"),
    LEAVE("leave", "expel"),
    AMEND("amend", "amend");

    private final String selfKey;
    private final String otherKey;

    RequestKind(String selfKey, String otherKey) {
        this.selfKey = selfKey;
        this.otherKey = otherKey;
    }

    public String getStyleName() {
        return name().toLowerCase(Locale.ROOT);
    }

    // Joining or leaving on someone else's behalf reads as inviting or expelling them.
    public String getTranslationKey(boolean isForSelf) {
        return "request.cabalist." + (isForSelf ? selfKey : otherKey);
    }
}
