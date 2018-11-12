package com.sh.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MsgListenerTopic {

	private static final Logger LOGGER = LoggerFactory.getLogger(MsgListenerTopic.class);
	
	/**
	 * Метод, который читает тему, когда существуют сообщения.
	 * Этот метод является слушателем* @param msg - String message
	 */
	public void onMessage(String msg) {
		LOGGER.debug(msg);
	}
	
}
