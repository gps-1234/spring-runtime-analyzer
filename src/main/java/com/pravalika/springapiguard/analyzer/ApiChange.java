package com.pravalika.springapiguard.analyzer;

public final class ApiChange {

    public enum Type {
        ADDED,
        REMOVED,
        PATH_CHANGED,
        METHOD_CHANGED,
        REQUEST_CHANGED,
        RESPONSE_CHANGED
    }

    private final Type type;
    private final ApiEndpoint before;
    private final ApiEndpoint after;
    private final String message;
    private final boolean breaking;

    public ApiChange(
            Type type,
            ApiEndpoint before,
            ApiEndpoint after,
            String message,
            boolean breaking
    ) {
        this.type = type;
        this.before = before;
        this.after = after;
        this.message = message;
        this.breaking = breaking;
    }

    public Type getType() {
        return type;
    }

    public ApiEndpoint getBefore() {
        return before;
    }

    public ApiEndpoint getAfter() {
        return after;
    }

    public String getMessage() {
        return message;
    }

    public boolean isBreaking() {
        return breaking;
    }
}