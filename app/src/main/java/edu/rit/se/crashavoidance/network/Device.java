package edu.rit.se.crashavoidance.network;

/**
 * Created by osvaldo on 18/05/17.
 */

public class Device {
    public String deviceId;
    public String location = "";
    public String timestamp = "";
    public String mac = "";

    public Device() {}

    public Device(String name) {
        deviceId = name;
    }

    public Device (String deviceId, String location, String timestamp) {
        this.deviceId = deviceId;
        this.location = location;
        this.timestamp = timestamp;
    }
}
