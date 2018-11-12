package com.sh.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MsgListenerQueue {
	
	/**
	 * Класс отправителя для топика
	 */
	@Autowired
	@Qualifier("jmsTemplateTopic")
	private JmsTemplate jmsTemplateTopic;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MsgListenerQueue.class);
	
	/**
	 * Метод, который читает очередь, когда существует сообщение.
	 * Этот метод является слушателем
	 */
	public void onMessage(String msg) {
		LOGGER.debug(msg);
		jmsTemplateTopic.send(session->session.createTextMessage(UUID.randomUUID()+" "+ msg));
	}
}
