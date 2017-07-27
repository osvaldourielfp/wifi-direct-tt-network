package edu.rit.se.crashavoidance.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by osvaldo on 18/05/17.
 */

public enum DeviceType {
    RANGE_EXTENDER(1),
    ACCESS_POINT(2),
    ACCESS_POINT_WREQ(5),
    ACCESS_POINT_WRES(6),
    QUERIER(3),
    QUERIER_ASK(7),
    RANGE_EXTENDER_WREQ(8),
    RANGE_EXTENDER_WRES(9),
    EMITTER(4);

    public int id;

    DeviceType(int id) {
        this.id = id;
    }

    public int getCode() { return id; }

    public static DeviceType get(int id) {
        return lookup.get(id);
    }

    private static final Map<Integer,DeviceType> lookup
            = new HashMap<Integer,DeviceType>();

    static {
        for(DeviceType s : EnumSet.allOf(DeviceType.class))
            lookup.put(s.getCode(), s);
    }
}
