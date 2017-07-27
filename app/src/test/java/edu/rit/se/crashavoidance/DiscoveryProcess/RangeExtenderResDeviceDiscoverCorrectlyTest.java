package edu.rit.se.crashavoidance.DiscoveryProcess;

import org.junit.Assert;
import org.junit.Test;

import edu.rit.se.crashavoidance.network.DeviceType;
import edu.rit.se.crashavoidance.network.NetworkUtil;

/**
 * Created by osvaldo on 09/07/17.
 */

public class RangeExtenderResDeviceDiscoverCorrectlyTest {

    NetworkUtil networkUtil;

    public RangeExtenderResDeviceDiscoverCorrectlyTest() {
        networkUtil = NetworkUtil.getInstance(DeviceType.RANGE_EXTENDER_WRES);
    }

    @Test
    public void discoverEmitter() throws Exception {
        boolean result = networkUtil.canDiscoverTo(DeviceType.EMITTER.toString());
        Assert.assertEquals(false, result);
    }

    @Test
    public void discoverAccessPoint() throws Exception {
        boolean result = networkUtil.canDiscoverTo(DeviceType.ACCESS_POINT.toString());
        boolean result1 = networkUtil.canDiscoverTo(DeviceType.ACCESS_POINT_WRES.toString());
        boolean result2 = networkUtil.canDiscoverTo(DeviceType.ACCESS_POINT_WREQ.toString());

        Assert.assertEquals(true, result);
        Assert.assertEquals(false, result1);
        Assert.assertEquals(false, result2);
    }

    @Test
    public void discoverQuerier() throws Exception {
        boolean result = networkUtil.canDiscoverTo(DeviceType.QUERIER.toString());
        boolean result2 = networkUtil.canDiscoverTo(DeviceType.QUERIER_ASK.toString());

        Assert.assertEquals(false, result);
        Assert.assertEquals(false, result2);
    }

    @Test
    public void discoverRangeExtender() throws Exception {
        boolean result = networkUtil.canDiscoverTo(DeviceType.RANGE_EXTENDER.toString());
        boolean result1 = networkUtil.canDiscoverTo(DeviceType.RANGE_EXTENDER_WREQ.toString());
        boolean result2 = networkUtil.canDiscoverTo(DeviceType.RANGE_EXTENDER_WRES.toString());

        Assert.assertEquals(false, result);
        Assert.assertEquals(false, result1);
        Assert.assertEquals(false, result2);
    }
}
