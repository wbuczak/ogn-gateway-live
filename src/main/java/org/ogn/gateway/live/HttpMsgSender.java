/**
 * Copyright (c) 2015 OGN, All Rights Reserved.
 */

package org.ogn.gateway.live;

import static org.ogn.gateway.live.LiveGlidernetForwarder.AMPERSAND;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMsgSender extends AbstractMessageSender {

	private static final String SERVICE_URL = "http://live.glidernet.org/livep.php";

	private static final Logger LOG = LoggerFactory.getLogger(HttpMsgSender.class);

	private static int HTTP_CONNECT_TIMEOUT = 1000;
	private static int HTTP_READ_TIMEOUT = 1000;

	private static String PASSWD_PARAM = "p";

	private final String password;

	public static String pass(String password) {
		return String.format("%s=%s", PASSWD_PARAM, password);
	}

	public HttpMsgSender(final String passwd) {
		this.password = passwd;
	}

	@Override
	protected void sendMessage(String msg) {

		if (msg.length() == 0)
			return;

		StringBuilder bld = new StringBuilder(pass(password));
		bld.append(AMPERSAND).append(msg);

		String msgToSend = bld.toString();

		LOG.debug("Sending HTTP POST message to {} - {}", SERVICE_URL, msgToSend);

		DataOutputStream wr = null;
		try {
			URL serverAddress = new URL(SERVICE_URL);
			HttpURLConnection connection = (HttpURLConnection) serverAddress.openConnection();

			connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT); // Set timeout
																// to 1s
			connection.setReadTimeout(HTTP_READ_TIMEOUT);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.connect();
			wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(msgToSend);
		} catch (java.net.SocketTimeoutException e) {
			LOG.warn("connection timeout!", e);
		} catch (Exception e) {
			LOG.error("exception caught", e);
		} finally {
			if (wr != null)
				try {
					wr.flush();
					wr.close();
				} catch (IOException e) {
					LOG.warn("exception", e);
				}
		}

	}
}