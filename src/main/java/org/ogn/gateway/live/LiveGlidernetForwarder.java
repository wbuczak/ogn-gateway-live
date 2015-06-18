/**
 * Copyright (c) 2014 OGN, All Rights Reserved.
 */

package org.ogn.gateway.live;

import static java.lang.String.format;

import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.forwarder.OgnAircraftBeaconForwarder;
import org.ogn.commons.collections.TimeWindowBuffer;
import org.ogn.commons.collections.TimeWindowBufferListener;
import org.ogn.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
// import java.util.concurrent.locks.ReentrantLock;

// import com.hazelcast.config.Config;
// import com.hazelcast.core.Hazelcast;
// Oimport com.hazelcast.core.HazelcastInstance;

/**
 * live.glidernet.org forwarder plug-in for OGN gateway
 * 
 * @author Seb, re-factoring: wbuczak
 */
public class LiveGlidernetForwarder implements OgnAircraftBeaconForwarder, TimeWindowBufferListener {

    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    public static final String SERVICE_NAME = "live.glidernet.org";
    public static final String VERSION = "1.0.0";

    private static final Logger LOG = LoggerFactory.getLogger(LiveGlidernetForwarder.class);

    private static final int MAX_BUFFER_SIZE = 1500;
    private static final int BUFFER_TIME_WINDOW = 2000;

    public static final String FIX_PREFIX = "fix[]=";
    public static final char DELIMITER = ',';
    public static final String AMPERSAND = "&";

    private TimeWindowBuffer<String> buffer;

    // private Lock lock;

    private MsgSender forwarder;

    // private boolean clusterMode;

    private boolean initialized = false;

    @Override
    public String getName() {
        return SERVICE_NAME + " forwarder";
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "relays OGN aircraft beacons to " + SERVICE_NAME;
    }

    @Override
    public void init() {

        if (initialized)
            return;

        buffer = new TimeWindowBuffer<>(MAX_BUFFER_SIZE, BUFFER_TIME_WINDOW, this, AMPERSAND);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:application-context.xml");
        ctx.refresh();
        String passwd = ctx.getBean(String.class, "passwd");
        // clusterMode = ctx.getBean(Boolean.class, "clusterMode");
        ctx.close();

        // if (clusterMode) {
        // Config config = new Config();
        // HazelcastInstance h = Hazelcast.newHazelcastInstance(config);
        // lock = h.getLock("OgnGatewatLivePluginLock");
        // } else {
        // lock = new ReentrantLock();
        // }

        if (forwarder == null)
            forwarder = new HttpMsgSender(passwd);

        initialized = true;
    }

    void init(MsgSender sender) {
        if (!initialized) {
            this.forwarder = sender;
            init();
        }
    }

    @Override
    public void stop() {
        // lock.unlock();
    }

    /**
     * default constructor
     */
    public LiveGlidernetForwarder() {

    }

    static String beaconToStr(AircraftBeacon beacon, AircraftDescriptor descriptor) {
        StringBuilder bld = new StringBuilder(FIX_PREFIX);

        // if descriptor is provided take cn and reg number from it
        if (descriptor != null && descriptor.isKnown()) {
            bld.append(descriptor.getCN()).append(DELIMITER).append(descriptor.getRegNumber());
        } else {
            bld.append(beacon.getAddress().substring(beacon.getAddress().length() - 2)).append(DELIMITER)
                    .append(beacon.getAddress());
        }

        bld.append(DELIMITER);

        bld.append(format("%.4f", beacon.getLat())).append(DELIMITER);
        bld.append(format("%.4f", beacon.getLon())).append(DELIMITER);
        bld.append(format("%.0f", beacon.getAlt())).append(DELIMITER);
        bld.append(format("%d", beacon.getTimestamp() / 1000)).append(DELIMITER);
        bld.append(format("%d", beacon.getTrack())).append(DELIMITER);
        bld.append(format("%.0f", beacon.getGroundSpeed())).append(DELIMITER);
        bld.append(format("%.1f", beacon.getClimbRate())).append(DELIMITER);
        bld.append(format("%d", beacon.getAircraftType().getCode())).append(DELIMITER);
        bld.append(beacon.getReceiverName()).append(DELIMITER);
        bld.append(beacon.getId());

        return bld.toString();
    }

    @Override
    public void onBeacon(AircraftBeacon beacon, AircraftDescriptor descriptor, String rawBeacon) {
        // if (lock.tryLock()) {

        // forward only beacons which have tracked flag ON
        // if (descriptor.isTracked()) {
        LOG.trace("sending beacon to {}: {} {}", SERVICE_NAME, JsonUtils.toJson(beacon), JsonUtils.toJson(descriptor));
        buffer.add(beaconToStr(beacon, descriptor));
        // }
        // } else {
        // LOG.info("discarding, another gateway instance (in the cluster) is forwarding beacon: {}",
        // beacon.getRawPacket());
        // }
    }

    /**
     * TimeWindowBuffer's callback
     */
    @Override
    public void tick(String msg, int elements) {
        LOG.trace("tick received with {} elements", elements);
        forwarder.send(msg);
    }
}