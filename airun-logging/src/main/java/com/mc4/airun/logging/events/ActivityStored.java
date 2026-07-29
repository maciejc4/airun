package com.mc4.airun.logging.events;

public record ActivityStored(String sourceName) {

    public ActivityStored {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Activity source name must not be blank");
        }
    }
}
