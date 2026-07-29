package com.mc4.airun.ingestion.events;

public interface RawActivityPublisher {

    void publish(RawActivity event);
}
