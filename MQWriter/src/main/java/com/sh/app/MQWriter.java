package com.sh.app;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.jms.ConnectionFactory;
import java.text.MessageFormat;

@SpringBootApplication
@ComponentScan(basePackages = "com.sh")
public class MQWriter {

	@Value("${jms.broker.url}")
	private String broker;

	@Value("${jms.topic.name}")
	private String topicName;

	@Value("${jms.queue.name}")
	private String queueName;

	private static final Logger LOGGER = LoggerFactory.getLogger(MQWriter.class);

	public static void main(String[] args) {
		SpringApplication.run(MQWriter.class);
	}

	/**
	 * Реализация ActiveMQ для подключения фабрики. Если вы хотите использовать другие
	 * механизм обмена сообщениями, вы должны реализовать его здесь. В этом
	 * случай, ActiveMQConnectionFactory.
	 */
	@Bean
	public ConnectionFactory connectionFactory() {
		LOGGER.debug("<<<<<< Loading connectionFactory");
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(broker);
		connectionFactory.setUserName("system");
		connectionFactory.setPassword("manager");
		LOGGER.debug(MessageFormat.format("{0} loaded sucesfully >>>>>>>", broker));
		return connectionFactory;
	}

	@Bean
	public ConnectionFactory cachingConnectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setTargetConnectionFactory(connectionFactory());
		connectionFactory.setSessionCacheSize(10);
		return connectionFactory;
	}

	@Bean(name = "jmsTemplateTopic")
	public JmsTemplate jmsTemplateTopic() {
		LOGGER.debug("<<<<<< Loading jmsTemplateTopic");
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(connectionFactory());
		template.setDefaultDestinationName(topicName);
		template.setPubSubDomain(true);
		LOGGER.debug("jmsTemplateTopic loaded >>>>>>>");
		return template;
	}

	@Bean(name = "jmsTemplateQueue")
	public JmsTemplate jmsTemplateQueue() {
		LOGGER.debug("<<<<<< Loading jmsTemplateQueue");
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(connectionFactory());
		template.setDefaultDestinationName(queueName);
		template.setPubSubDomain(false);
		LOGGER.debug("jmsTemplateQueue loaded >>>>>>>>");
		return template;
	}

	@Bean
	public ThreadPoolTaskExecutor executor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(5);
		ex.setMaxPoolSize(15);
		return ex;
	}
}