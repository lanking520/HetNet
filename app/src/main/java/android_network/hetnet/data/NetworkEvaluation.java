package android_network.hetnet.data;

import java.io.Serializable;

/**
 * Created by lanking on 26/04/2017.
 */

public class NetworkEvaluation implements Serializable{
    private String bandwidth;
    private double latency;
    public String toString(){
        String finale = "";
        finale += "Current Bandwidth: " + bandwidth;
        finale += "\nCurrent Latency: "+String.valueOf(finale);
        return finale;
    }

    public void setBandwidth(String bandwidth) {
        this.bandwidth = bandwidth;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public String getBandwidth() {
        return bandwidth;
    }

    public double getLatency() {
        return latency;
    }
}
