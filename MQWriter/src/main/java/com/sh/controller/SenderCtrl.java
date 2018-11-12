package com.sh.controller;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
public class SenderCtrl {
	@Autowired
	@Qualifier("jmsTemplateTopic")
	private JmsTemplate jmsTemplateTopic;
	
	@Autowired
	@Qualifier("jmsTemplateQueue")
	private JmsTemplate jmsTemplateQueue;
	
	@Autowired
	private ThreadPoolTaskExecutor executor;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SenderCtrl.class);
	
	/**
	 * REST method for send a message to topic
	 * @param msg - String
	 * @return String - result
	 */
	@RequestMapping(value="/sendTopic",method=RequestMethod.POST)
	public String sendTopic(@RequestBody String msg) {
		try {
			jmsTemplateTopic.send(session -> session.createTextMessage(msg));
			return "MESSAGE WAS SENT";
		} catch (JmsException e) {
			LOGGER.debug("Error: ",e);
			return e.getMessage();
		}
	}
	/**
	 * REST method for send a message to queue
	 * @param msg - String
	 * @return String - result
	 */
	@RequestMapping(value="/sendQueue",method=RequestMethod.POST)
	public String sendQueue(@RequestBody String msg) {
		try {
			jmsTemplateQueue.send(session->session.createTextMessage(msg));
			return "MESSAGE WAS SENT";
		} catch (JmsException e) {
			LOGGER.debug("Error: ",e);
			return e.getMessage();
		}
	}
	
	/**
	 * Send all messages from text file,one per line
	 * @return String - result
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/sendFromFIle",method=RequestMethod.GET)
	public String sendFromFile() {
		try {
			InputStream iStr = getClass().getClassLoader().getResourceAsStream("messages.txt");
			List<String> lsRes =  IOUtils.readLines(iStr);
			executor.execute(createThread(lsRes));
			return "PROCESS LAUNCHED";
		}
		catch (IOException e) {
			LOGGER.debug("Error: ",e);
			return e.getMessage();
		}
	}
	
	/**
	 * Create a thread for iterate and send messages to Queue from list
	 * @param ls - List&lt;String&gt; 
	 * @return Thread - created thread
	 */
	private Thread createThread(List<String> ls) {
		Runnable hilo = () -> {
			for (String s : ls) {
				jmsTemplateQueue.send(session -> session.createTextMessage(s));
			}
		};
		return new Thread(hilo);
	}

}
