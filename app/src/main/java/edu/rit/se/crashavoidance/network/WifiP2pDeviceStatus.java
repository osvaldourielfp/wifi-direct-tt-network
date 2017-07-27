package edu.rit.se.crashavoidance.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by osvaldo on 19/05/17.
 */

public enum WifiP2pDeviceStatus {
    CONNECTED(0),
    INVITED(1),
    FAILED(2),
    AVAILABLE(3),
    UNAVAILABLE(4);

    public int id;

    WifiP2pDeviceStatus(int id) {
        this.id = id;
    }

    public int getCode() { return id; }

    public static WifiP2pDeviceStatus get(int id) {
        return lookup.get(id);
    }

    private static final Map<Integer,WifiP2pDeviceStatus> lookup
            = new HashMap<Integer,WifiP2pDeviceStatus>();

    static {
        for(WifiP2pDeviceStatus s : EnumSet.allOf(WifiP2pDeviceStatus.class))
            lookup.put(s.getCode(), s);
    }
}
