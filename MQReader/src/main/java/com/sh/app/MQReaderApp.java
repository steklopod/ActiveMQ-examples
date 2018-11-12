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
public class MQReaderApp {

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
     * Реализация ActiveMQ для подключения фабрики.
     * Если вы хотите использовать другой механизм обмена сообщениями, вы должны его
     * реализовать здесь.
     * В этом случае ActiveMQConnectionFactory.
     *
     * @return ConnectionFactory - интерфейс JMS
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
     * Захват фабрики соединений для лучшей производительности при большой нагрузке
     **/
    @Bean
    public ConnectionFactory cachingConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setTargetConnectionFactory(connectionFactory());
        connectionFactory.setSessionCacheSize(10);
        return connectionFactory;
    }
    
    /**
     * Конфигурация адаптера прослушивателя сообщений для приема тем.
     * Класс MsgListenerTopic реализуется в методе onMessage
     **/
    @Bean(name = "adapterTopic")
    public MessageListenerAdapter adapterTopic(MsgListenerTopic topic) {
    	MessageListenerAdapter listener = new MessageListenerAdapter(topic);
    	listener.setDefaultListenerMethod("onMessage");
    	listener.setMessageConverter(new SimpleMessageConverter());
    	return listener;
    }

    /***
     * Конфигурация адаптера прослушивателя сообщений для приема очереди.
     * Класс MsgListenerQueue реализуется в методе onMessage
     **/
    @Bean(name = "adapterQueue")
    public MessageListenerAdapter adapterQueue(MsgListenerQueue queue) {
    	MessageListenerAdapter listener =  new MessageListenerAdapter(queue); 
    	listener.setDefaultListenerMethod("onMessage");
    	listener.setMessageConverter(new SimpleMessageConverter());
    	return listener;
    }
    
    /**
     * Контейнер слушателя темы.
     * Этот метод настраивает прослушиватель для темы
     **/
    @Bean(name = "jmsTopic")
    public SimpleMessageListenerContainer getTopic(MessageListenerAdapter adapterTopic){
    	LOGGER.debug("<<<<<< Loading Listener topic");
    	SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setDestinationName(topicName);
        container.setMessageListener(adapterTopic);
        container.setPubSubDomain(true);
        LOGGER.debug("Listener topic loaded >>>>>>>>>");
        return container;
    }
    
    /**
     * Контейнер для прослушивания очереди.
     * Этот метод настраивает слушателя для очереди
     **/
    @Bean(name = "jmsQueue")
    public SimpleMessageListenerContainer getQueue(MessageListenerAdapter adapterQueue){
    	LOGGER.debug("<<<<<< Loading Listener Queue");
    	SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setDestinationName(queueName);
        container.setMessageListener(adapterQueue);
        container.setPubSubDomain(false);
        LOGGER.debug("Listener Queue loaded >>>>>>>");
        return container;
    }

    /**
     * Конфигурация отправителя для темы
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
