package edu.rit.se.crashavoidance.network;

import android.util.Log;

import java.util.List;

import edu.rit.se.wifibuddy.DnsSdService;

/**
 * Created by osvaldo on 09/07/17.
 */

public class NetworkUtil {
    private static NetworkUtil singleton;
    private DeviceType myType;
    private List<DnsSdService> deviceList;

    private NetworkUtil() { }

    private NetworkUtil(DeviceType myType) {
        this.myType = myType;
    }

    public boolean canDiscoverTo(String discoveredDeviceType) {
        if(myType == DeviceType.EMITTER) {
            // ACCESS_POINT, because it has to send its information to a free ACCESS_POINT
            return discoveredDeviceType.equals(DeviceType.ACCESS_POINT.toString());
        } else if(myType == DeviceType.ACCESS_POINT_WREQ || myType == DeviceType.ACCESS_POINT_WRES) {
            // RANGE_EXTENDER, because it has to redirect its information to the RANGE_EXTENDER
            // !!! but we don't need to see the RANGE_EXTENDER, it has to see the ACCESS_POINT
            //return discoveredDeviceType.equals(DeviceType.RANGE_EXTENDER.toString());
            return false;
        } else if(myType == DeviceType.QUERIER) {
            // ACCESS_POINT, because the target ACCESS_POINT must not be involved in a searching process
            return discoveredDeviceType.equals(DeviceType.ACCESS_POINT.toString());
        } else if(myType == DeviceType.QUERIER_ASK) {
            // ACCESS_POINT_WRES, because the target ACCESS_POINT must gives us a response
            return discoveredDeviceType.equals(DeviceType.ACCESS_POINT_WRES.toString());
        } else if(myType == DeviceType.RANGE_EXTENDER) {
            // ACCESS_POINT_WREQ && ACCESS_POINT_WRES && !ACCESS_POINT
            // because the target must have interesting information (a request or response)
            // or must be a clean ACCESS_POINT to send it a request ONLY
            String str = DeviceType.ACCESS_POINT.toString();
            return discoveredDeviceType.startsWith(str) && !discoveredDeviceType.endsWith(str);
        } else if(myType == DeviceType.RANGE_EXTENDER_WREQ || myType == DeviceType.RANGE_EXTENDER_WRES) {
            return discoveredDeviceType.equals(DeviceType.ACCESS_POINT.toString());
        }
        return false;
    }

    public static NetworkUtil getInstance(DeviceType deviceType) {
        if(singleton == null) {
            synchronized(NetworkUtil.class) {
                if(singleton == null) {
                    singleton = new NetworkUtil();
                    Log.d("DEBUG", "getInstance(): inside");
                }
            }
        }
        Log.d("DEBUG", "getInstance(): ");
        if(deviceType != null) {
            singleton.myType = deviceType;
        }
        return singleton;
    }
}
