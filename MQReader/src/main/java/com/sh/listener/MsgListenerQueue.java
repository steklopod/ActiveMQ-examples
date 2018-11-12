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
	 * Sender class for topic
	 */
	@Autowired
	@Qualifier("jmsTemplateTopic")
	private JmsTemplate jmsTemplateTopic;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MsgListenerQueue.class);
	
	/**
	 * Method that read the Queue when exists messages.
	 * This method is a listener
	 * @param msg - String message
	 */
	public void onMessage(String msg) 
	{	
		LOGGER.debug(msg);
		jmsTemplateTopic.send(session->session.createTextMessage(UUID.randomUUID()+" "+ msg));
	}

}
