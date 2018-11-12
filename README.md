## JMSActiveMQ SpringBoot пример

Как реализовать Spring JMS с Apache, в частности, ActiveMQ?

Со `Spring Boot` это легко.Этот проект имеет 2 модуля, `writer` и `reader`. Оба имеют 
одинаковые зависимости.

With spring-boot starters only have to call spring-boot-starter-activemq, the package contains 
spring JMS and Apache ActiveMQ api.

## Reader module

In Reader module go to configure SpringBoot application for read from topic and from queue.  
Additionally add a writer,that is a JmsTemplate,for read from queue and write in topic operation.  
Look Java Configuration

```java
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
```

Here see 2 listeners, for topic and for queue,and 2 message adapters and a JmsTemplate for send to topic a message.  
See the Queue implementation class MsgListenerQueue, that write in log and send to topic the message with UUID.

```java
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
```

### Writer

Writer module contains a REST controller for send messages to topic and queue,and for send messages in a test file,one message per line.  
Configuration Contains connectionFactory, 2 JmsTemplates and a ThreadPoolExecutor for send all lines of file in other thread.See the configuration

```java
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
	 * ActiveMQ implementation for connection factory. If you want to use other
	 * messaging engine,you have to implement it here. In this
	 * case,ActiveMQConnectionFactory.
	 *
	 * @return ConnectionFactory - JMS interface
	 **/
	@Bean
	public ConnectionFactory connectionFactory() {
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
	public ConnectionFactory cachingConnectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setTargetConnectionFactory(connectionFactory());
		connectionFactory.setSessionCacheSize(10);
		return connectionFactory;
	}


	/**
     * Sender configuration for topic
     * @return JmsTemplate
     * @see JmsTemplate
     */
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


	/**
     * Sender configuration for queue
     * @return JmsTemplate
     * @see JmsTemplate
     */
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

	/**
	 * ThreadPool for long executions
	 * @return ThreadPoolTaskExecutor
	 * @see ThreadPoolTaskExecutor
	 */
	@Bean
	public ThreadPoolTaskExecutor executor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(5);
		ex.setMaxPoolSize(15);
		return ex;
	}

}
```

REST methods for write in ActiveMQ are:
* /sendTopic (POST)
* /sendQueue (POST)
* /sendFromFIle (GET)

It's easy to work with Apache activeMQ in SpringBoot.

Thanks to :  
[Apache ActiveMQ](http://activemq.apache.org/)  
[SpringBoot](https://projects.spring.io/spring-boot/)
