package android_network.hetnet.networkcap;

import java.util.Date;

import android_network.hetnet.common.trigger_events.TriggerEvent;

/**
 * Created by lanking on 26/04/2017.
 */

public class ConnectionTriggerEvent extends TriggerEvent {
    public ConnectionTriggerEvent(String eventOriginator, String eventName, Date timeOfEvent) {
        super(eventOriginator, eventName, timeOfEvent);
    }
}
