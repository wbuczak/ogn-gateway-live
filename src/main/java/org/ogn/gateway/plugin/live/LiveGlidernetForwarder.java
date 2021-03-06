/**
 * Copyright (c) 2014 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.live;

import static java.lang.String.format;

import java.util.Optional;

import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.forwarder.OgnAircraftBeaconForwarder;
import org.ogn.commons.collections.TimeWindowBuffer;
import org.ogn.commons.collections.TimeWindowBufferListener;
import org.ogn.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * live.glidernet.org forwarder plug-in for OGN gateway
 * 
 * @author Seb, re-factoring: wbuczak
 */
public class LiveGlidernetForwarder implements OgnAircraftBeaconForwarder, TimeWindowBufferListener {

	public static final String			SERVICE_NAME		= "live.glidernet.org";
	public static final String			VERSION				= "1.0.0";

	private static final Logger			LOG					= LoggerFactory.getLogger(LiveGlidernetForwarder.class);

	private static final int			MAX_BUFFER_SIZE		= 1500;
	private static final int			BUFFER_TIME_WINDOW	= 2000;

	public static final String			FIX_PREFIX			= "fix[]=";
	public static final char			DELIMITER			= ',';
	public static final String			AMPERSAND			= "&";

	private TimeWindowBuffer<String>	buffer;

	private MsgSender					forwarder;

	private volatile boolean			initialized			= false;

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

		if (!initialized) {

			buffer = new TimeWindowBuffer<>(MAX_BUFFER_SIZE, BUFFER_TIME_WINDOW, this, AMPERSAND);

			ClassPathXmlApplicationContext ctx =
					new ClassPathXmlApplicationContext("classpath:application-context.xml");
			ctx.refresh();
			String passwd = ctx.getBean(String.class, "passwd");
			ctx.close();

			if (forwarder == null)
				forwarder = new HttpMsgSender(passwd);

			initialized = true;
		}
	}

	void init(MsgSender sender) {
		if (!initialized) {
			this.forwarder = sender;
			init();
		}
	}

	@Override
	public void stop() {
	}

	/**
	 * default constructor
	 */
	public LiveGlidernetForwarder() {

	}

	static String beaconToStr(AircraftBeacon beacon, Optional<AircraftDescriptor> descriptor) {
		StringBuilder bld = new StringBuilder(FIX_PREFIX);

		// if descriptor is provided take cn and reg number from it
		if (descriptor.isPresent()) {
			bld.append(descriptor.get().getCN()).append(DELIMITER).append(descriptor.get().getRegNumber());
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
	public void onBeacon(AircraftBeacon beacon, Optional<AircraftDescriptor> descriptor) {

		LOG.trace("sending beacon to {}: {} {}", SERVICE_NAME, JsonUtils.toJson(beacon),
				JsonUtils.toJson(descriptor.orElse(null)));
		buffer.add(beaconToStr(beacon, descriptor));
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