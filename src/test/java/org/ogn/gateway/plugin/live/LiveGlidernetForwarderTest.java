/**
 * Copyright (c) 2014 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.live;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.AircraftType;
import org.ogn.commons.beacon.impl.aprs.AprsLineParser;

@RunWith(EasyMockRunner.class)
public class LiveGlidernetForwarderTest {

	@Mock
	AircraftBeacon		beacon;

	@Mock
	AircraftDescriptor	descr;

	// @Test
	public void testClustredForwarder() throws Exception {

		System.setProperty("live.glidernet.org.cluster.enabled", "true");

		final List<String> aprsSentences = Files.readAllLines(
				Paths.get(this.getClass().getResource("test-beacons.txt").toURI()), Charset.defaultCharset());

		final List<AircraftBeacon> beacons = new ArrayList<>();
		for (final String sentence : aprsSentences) {
			beacons.add((AircraftBeacon) AprsLineParser.get().parse(sentence));
		}

		final DummyMsgSender forwarder = new DummyMsgSender("anonymous");
		final LiveGlidernetForwarder cf = new LiveGlidernetForwarder();
		cf.init(forwarder);
		assertNotNull(cf);

		final LiveGlidernetForwarder cf2 = new LiveGlidernetForwarder();
		cf2.init(forwarder);
		assertNotNull(cf2);

		Thread.sleep(1000);

		final int ITERATIONS = 10;
		for (int i = 0; i < ITERATIONS; i++) {
			for (final AircraftBeacon b : beacons) {
				// System.out.println("adding beacon");
				cf.onBeacon(b, null);
				cf2.onBeacon(b, null);
			}
			Thread.sleep(100);
		}

		Thread.sleep(2000);

		final String[] elements = forwarder.getAll();

		assertTrue(elements.length > 0);

		// make sure there was as many updates received as there were beacon
		// entries in the file
		int count = 0;
		String pass = null;
		for (final String s : elements) {
			final String[] tokens = s.split("\\&");
			pass = tokens[0];
			// check the pass
			assertNotNull(pass);
			final String[] ptokens = pass.split("=");
			assertEquals(2, ptokens.length);
			assertEquals("p", ptokens[0]);
			assertEquals("anonymous", ptokens[1]);

			count += tokens.length - 1; // the first token is the password (do
										// not count it)
		}

		assertEquals(beacons.size() * ITERATIONS, count);

		Thread.sleep(6000);
	}

	@Test
	public void testBeaconToStr1() {

		final long timestamp = 1418939437063L;

		expect(beacon.getAddress()).andReturn("DD03434").times(3);
		expect(beacon.getAircraftType()).andReturn(AircraftType.GLIDER);
		expect(beacon.getAlt()).andReturn(1500.0f);
		expect(beacon.getLat()).andReturn((double) 52.882f);
		expect(beacon.getLon()).andReturn((double) 8.959f);
		expect(beacon.getClimbRate()).andReturn(1.2f);
		expect(beacon.getGroundSpeed()).andReturn(120.0f);
		expect(beacon.getId()).andReturn("DD03434");
		expect(beacon.getReceiverName()).andReturn("TestRec");
		expect(beacon.getTimestamp()).andReturn(timestamp);
		expect(beacon.getTrack()).andReturn(30);

		replay(beacon);

		final String str =
				LiveGlidernetForwarder.beaconToStr(beacon, Optional.ofNullable((AircraftDescriptor) null)).toString();

		assertEquals("fix[]=34,DD03434,52.8820,8.9590,1500,1418939437,30,120,1.2,1,TestRec,DD03434", str);
	}

	@Test
	public void testBeaconToStr2() {

		final long timestamp = 1418939437063L;

		expect(beacon.getAddress()).andReturn("DD03434").times(3);
		expect(beacon.getAircraftType()).andReturn(AircraftType.GLIDER);
		expect(beacon.getAlt()).andReturn(1500.0f);
		expect(beacon.getLat()).andReturn((double) 52.882f);
		expect(beacon.getLon()).andReturn((double) 8.959f);
		expect(beacon.getClimbRate()).andReturn(1.2f);
		expect(beacon.getGroundSpeed()).andReturn(120.0f);
		expect(beacon.getId()).andReturn("DD03434");
		expect(beacon.getReceiverName()).andReturn("TestRec");
		expect(beacon.getTimestamp()).andReturn(timestamp);
		expect(beacon.getTrack()).andReturn(30);

		expect(descr.getCN()).andReturn("3M");
		expect(descr.getRegNumber()).andReturn("HA-4295");

		replay(beacon, descr);

		final String str = LiveGlidernetForwarder.beaconToStr(beacon, Optional.of(descr)).toString();

		assertEquals("fix[]=3M,HA-4295,52.8820,8.9590,1500,1418939437,30,120,1.2,1,TestRec,DD03434", str);
	}

}