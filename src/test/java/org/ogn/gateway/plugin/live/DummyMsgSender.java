/**
 * Copyright (c) 2015 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.live;

import static org.ogn.gateway.plugin.live.LiveGlidernetForwarder.AMPERSAND;

import java.util.ArrayList;
import java.util.List;

import org.ogn.gateway.plugin.live.AbstractMessageSender;
import org.ogn.gateway.plugin.live.HttpMsgSender;

/**
 * for tests only
 * 
 * @author wbuczak
 */
public class DummyMsgSender extends AbstractMessageSender {

	List<String> buffor = new ArrayList<>();

	private final String password;

	public DummyMsgSender(final String passwd) {
		this.password = passwd;
	}

	@Override
	protected synchronized void sendMessage(String msg) {
		StringBuilder bld = new StringBuilder(HttpMsgSender.pass(password));
		bld.append(AMPERSAND).append(msg);

		String msgToSend = bld.toString();

		buffor.add(msgToSend);
	}

	public synchronized int size() {
		return buffor.size();
	}

	public synchronized String[] getAll() {
		return buffor.toArray(new String[0]);
	}
}
