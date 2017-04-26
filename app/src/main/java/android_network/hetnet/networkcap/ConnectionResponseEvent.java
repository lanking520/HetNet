package android_network.hetnet.networkcap;

import java.util.Date;

import android_network.hetnet.data.NetworkEvaluation;

/**
 * Created by lanking on 26/04/2017.
 */

public class ConnectionResponseEvent {
    private String eventOriginator;
    private NetworkEvaluation evaluation;
    private Date timeOfEvent;
    public ConnectionResponseEvent(String eventOriginator, NetworkEvaluation eval, Date timeOfEvent) {
        this.eventOriginator = eventOriginator;
        this.evaluation = eval;
        this.timeOfEvent = timeOfEvent;
    }

    public String getEventOriginator() {
        return eventOriginator;
    }

    public void setEventOriginator(String eventOriginator) {
        this.eventOriginator = eventOriginator;
    }

    public NetworkEvaluation getEvaluation() {
        return evaluation;
    }

    public void setLocation(NetworkEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    public Date getTimeOfEvent() {
        return timeOfEvent;
    }

    public void setTimeOfEvent(Date timeOfEvent) {
        this.timeOfEvent = timeOfEvent;
    }
}
