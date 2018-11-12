package com.sh.app;

import com.sh.listener.MsgListenerQueue;
import com.sh.listener.MsgListenerTopic;
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
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import javax.jms.ConnectionFactory;
import java.text.MessageFormat;


@SpringBootApplication
@ComponentScan(basePackages="com.sh")
public class MQReaderApp 
{
	
	@Value("${jms.broker.url}")
	private String broker;
    
	@Value("${jms.topic.name}")
    private String topicName;
	
	@Value("${jms.queue.name}")
    private String queueName;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MQReaderApp.class);

	public static void main(String[] args) 
	{
		SpringApplication.run(MQReaderApp.class, args);
	}
	
	/**
	 * ActiveMQ implementation for connection factory.
	 * If you want to use other messaging engine,you have to implement it here.
	 * In this case,ActiveMQConnectionFactory.
	 * @return ConnectionFactory - JMS interface
	 **/
	@Bean
    public ConnectionFactory connectionFactory(){
 		LOGGER.debug("<<<<<< Loading connectionFactory");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(broker);
        LOGGER.debug(MessageFormat.format("{0} loaded sucesfully >>>>>>>", broker));
        return connectionFactory;
    }
	
    /**
     * Catching connection factory for better performance if big load
     * @return ConnectionFactory - cachingConnection
     **/
    @Bean
    public ConnectionFactory cachingConnectionFactory(){
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setTargetConnectionFactory(connectionFactory());
        connectionFactory.setSessionCacheSize(10);
        return connectionFactory;
    }
    
    /**
     * Message listener adapter configuration for topic reception.
     * MsgListenerTopic class implements in method onMessage
     * @param topic - MsgListenerTopic
     * @see MsgListenerTopic
     * @return MessageListenerAdapter
     * @see MessageListenerAdapter
     **/
    @Bean(name = "adapterTopic")
    public MessageListenerAdapter adapterTopic(MsgListenerTopic topic)
    {
    	MessageListenerAdapter listener = new MessageListenerAdapter(topic);
    	listener.setDefaultListenerMethod("onMessage");
    	listener.setMessageConverter(new SimpleMessageConverter());
    	return listener;
    	
    }
    
    /**
     * Message listener adapter configuration for queue reception.
     * MsgListenerQueue class implements in method onMessage
     * @param queue - MsgListenerQueue
     * @see MsgListenerQueue
     * @return MessageListenerAdapter
     * @see MessageListenerAdapter
     **/
    @Bean(name = "adapterQueue")
    public MessageListenerAdapter adapterQueue(MsgListenerQueue queue)
    {
    	MessageListenerAdapter listener =  new MessageListenerAdapter(queue); 
    	listener.setDefaultListenerMethod("onMessage");
    	listener.setMessageConverter(new SimpleMessageConverter());
    	return listener;
    	
    }
    
    /**
     * Topic listener container.
     * This method configure a listener for a topic
     * @param adapterTopic -  MessageListenerAdapter
     * @see MessageListenerAdapter
     * @see SimpleMessageListenerContainer
     **/
    @Bean(name = "jmsTopic")
    public SimpleMessageListenerContainer getTopic(MessageListenerAdapter adapterTopic){
    	LOGGER.debug("<<<<<< Loading Listener topic");
    	SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    	// settings for listener: connectonFactory,Topic name,MessageListener and PubSubDomain (true if is a topic)
        container.setConnectionFactory(connectionFactory());
        container.setDestinationName(topicName);
        container.setMessageListener(adapterTopic);
        container.setPubSubDomain(true);
        LOGGER.debug("Listener topic loaded >>>>>>>>>");
        
        return container;
    }
    
    /**
     * Queue listener container.
     * This method configure a listener for a queue
     * @param adapterQueue -  MessageListenerAdapter
     * @see MessageListenerAdapter
     * @see SimpleMessageListenerContainer
     **/
    @Bean(name = "jmsQueue")
    public SimpleMessageListenerContainer getQueue(MessageListenerAdapter adapterQueue){
    	LOGGER.debug("<<<<<< Loading Listener Queue");
    	SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    	// settings for listener: connectonFactory,Topic name,MessageListener and PubSubDomain (false if is a queue)
        container.setConnectionFactory(connectionFactory());
        container.setDestinationName(queueName);
        container.setMessageListener(adapterQueue);
        container.setPubSubDomain(false);
        LOGGER.debug("Listener Queue loaded >>>>>>>");
        
        return container;
    }
    
    
   
    /**
     * Sender configuration for topic 
     * @return JmsTemplate
     * @see JmsTemplate
     */
    @Bean(name = "jmsTemplateTopic")
    public JmsTemplate jmsTemplateTopic(){
    	LOGGER.debug("<<<<<< Loading jmsTemplateTopic");
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory());
        template.setDefaultDestinationName(topicName);
        template.setPubSubDomain(true);
        LOGGER.debug("jmsTemplateTopic loaded >>>>>>>");
        
        return template;
    }
}
