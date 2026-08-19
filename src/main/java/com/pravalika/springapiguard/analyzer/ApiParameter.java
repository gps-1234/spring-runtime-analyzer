package com.pravalika.springapiguard.analyzer;

public final class ApiParameter {

    public enum Location {
        PATH,
        QUERY,
        HEADER,
        BODY
    }

    private final String name;
    private final String type;
    private final Location location;
    private final boolean required;

    public ApiParameter(
            String name,
            String type,
            Location location,
            boolean required
    ) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isRequired() {
        return required;
    }

    public String key() {
        return location + ":" + name;
    }
}