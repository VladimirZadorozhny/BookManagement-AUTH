package org.mystudying.bookmanagementauth.domain;

public enum ReminderType {
    THREE_DAYS_LEFT("3_DAYS_LEFT"),
    DUE_TODAY("DUE_TODAY"),
    OVERDUE("OVERDUE");

    private final String type;

    ReminderType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
