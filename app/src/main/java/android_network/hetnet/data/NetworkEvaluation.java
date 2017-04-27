package android_network.hetnet.data;

import java.io.Serializable;

/**
 * Created by lanking on 26/04/2017.
 */

public class NetworkEvaluation implements Serializable{
    private String bandwidth;
    private double latency;
    private String MAC_ADDR;

    public String toString(){
        String finale = "";
        finale += "Current Bandwidth: " + bandwidth;
        finale += "\nCurrent Latency: "+String.valueOf(latency);
        finale += "\nCurrent Mac Address: "+String.valueOf(MAC_ADDR);
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

    public String getMAC_ADDR() {
        return MAC_ADDR;
    }

    public void setMAC_ADDR(String MAC_ADDR) {
        this.MAC_ADDR = MAC_ADDR;
    }
}
