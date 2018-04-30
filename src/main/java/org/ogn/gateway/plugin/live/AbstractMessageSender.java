/**
 * Copyright (c) 2015 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.live;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessageSender implements MsgSender {

	protected BlockingQueue<String>	messages;

	private ExecutorService			executor;

	private static final Logger		LOG	= LoggerFactory.getLogger(AbstractMessageSender.class);

	private class PollerTask implements Runnable {
		private Logger PLOG = LoggerFactory.getLogger(PollerTask.class);

		@Override
		public void run() {
			PLOG.trace("starting...");
			while (!Thread.interrupted()) {
				try {
					sendMessage(messages.take());
				} catch (InterruptedException e) {
					PLOG.trace("interrupted exception caught. Was the poller task interrupted on purpose?");
					Thread.currentThread().interrupt();
					continue;
				}
			} // while
			PLOG.trace("exiting..");
		}
	}

	protected AbstractMessageSender() {
		executor = Executors.newSingleThreadExecutor();
		messages = new LinkedBlockingQueue<>();
		executor.submit(new PollerTask());
	}

	@Override
	public final void send(String msg) {
		if (!messages.offer(msg)) {
			LOG.warn("could not insert message to the message queue");
		}
	}

	protected void stop() {
		executor.shutdown();
	}

	protected abstract void sendMessage(String msg);
}