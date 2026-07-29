package com.mc4.airun.activities.events;

import java.util.Arrays;

public record RawActivity(
        String sourceName,
        byte[] content
) {

    public RawActivity {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Activity source name must not be blank");
        }
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
