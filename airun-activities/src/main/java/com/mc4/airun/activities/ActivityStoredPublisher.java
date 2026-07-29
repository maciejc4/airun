package com.mc4.airun.activities;

import com.mc4.airun.activities.events.ActivityStored;

public interface ActivityStoredPublisher {

    void publish(ActivityStored event);
}
