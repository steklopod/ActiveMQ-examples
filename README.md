В этом руководстве описывается процесс публикации и подписки на сообщения с помощью брокера JMS.

### Что мы создадим?
Вы создадите приложение, которое использует спринговый `JmsTemplate` для публикации
одного сообщения и подписки на него с помощью аннотированного метода `@JmsListener` управляемого Бина.

### Создание получателя сообщения
Spring предоставляет средства для публикации сообщений в любом `POJO`.

В этом руководстве вы рассмотрите, как отправить сообщение по брокеру сообщений JMS. Чтобы
начать, давайте создадим очень простой POJO, который воплощает в себе детали сообщения
электронной почты. Обратите внимание, мы не отправляем сообщение по электронной почте. **Мы
просто посылаем детали из одного места в другое О ТОМ, ЧТО отправить в сообщении**.

`src/main/java/ru/Email.java`

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Email {

    private String to;
    private String body;

    @Override
    public String toString() {
        return String.format("Email{to=%s, body=%s}", getTo(), getBody());
    }
}
```

Это POJO довольно прост, содержит два поля, `to` и `body`, а также предполагаемый набор геттеров и сеттеров.

Здесь можно определить получателя сообщения:

`src/main/java/ru/Receiver.java`

```java
@Component
public class Receiver {

    @JmsListener(destination = "mailbox", containerFactory = "myFactory")
    public void receiveMessage(Email email) {
        System.out.println("Received <" + email + ">");
    }
}
```

`Receiver` (получатель) также известен как POJO, управляемый сообщением. Как вы можете видеть
в приведенном выше коде, нет необходимости реализовывать какой-либо конкретный интерфейс или
иметь какое-либо конкретное имя метода. Кроме того, метод может иметь очень гибкую сигнатуру.
Обратите внимание, в частности, что этот класс не имеет импорта в API JMS.

Аннотация `@JmsListener` определяет имя назначения, которое должен слушать этот метод, и
ссылку на `JmsListenerContainerFactory`, чтобы использовать для создания базового контейнера
прослушивателя сообщений. Строго говоря, этот последний атрибут не нужен, если вам не нужно
настроить способ сборки контейнера, поскольку `Spring Boot` регистрирует фабрику по умолчанию,
если это необходимо. [Более подробно это описано в справочной документации](https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#jms-annotated-method-signature).

### Отправка и получение сообщений JMS с Spring
Затем подключите отправителя и получателя.

`src/main/java/ru/Application.java`

`@EnableJms` запускает обнаружение методов с аннотацией `@JmsListener`, создание контейнера
получатель сообщения `под капотом`.

Для ясности мы также определили бин `myFactory`, на который ссылаются в аннотации `JmsListener`
 получателя. Поскольку мы используем инфраструктуру `DefaultJmsListenerContainerFactoryconfigurer`,
  предоставляемую Spring Boot, этот `JmsMessageListenerContainer` будет идентичен тому,
  который создается загрузчиком по умолчанию.

```java
    @Bean
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
                                                    DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        // Это обеспечивает всю загрузку по умолчанию к этой фабрике, включая конвертер сообщений
        configurer.configure(factory, connectionFactory);
        return factory;
    }

```
`MessageConverter` по умолчанию может преобразовывать только базовые типы (такие как `String`,
`Map`, `Serializable`), и наша электронная почта не сериализуема специально. Мы хотим
использовать `Jackson` и сериализовать содержимое в `json` в текстовом формате (т.е. как `TextMessage`).
`Spring Boot` обнаружит наличие `MessageConverter` и свяжет его как с `jmstemplate` по умолчанию,
так и с любым `JmsListenerContainerFactory`, созданным `DefaultJmsListenerContainerFactoryconfigurer`.

```java
    @Bean // Сериализация содержимого сообщения в json с помощью TextMessage
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

```

`JmsTemplate` делает это очень простым для отправки сообщений в пункт назначения JMS. В методе
`main runner` после запуска вы можете просто использовать `jmsTemplate` для отправки электронного
письма. Поскольку наш пользовательский `MessageConverter` был автоматически связан с ним,
документ json будет создан только в `TextMessage`.

Два бина, которые вы не видите - `JmsTemplate` и `ConnectionFactory`. Они создаются автоматически
с помощью Spring Boot. В этом случае брокер `ActiveMQ` запускается встроенно (embedded).

По умолчанию Spring Boot создает `JmsTemplate`, настроенный для передачи в очереди, если
`pubSubDomain` имеет значение `false`. `JmsMessageListenerContainer` также настроен то же самое.
Для переопределения установите `spring.jms.isPubSubDomain=true` через настройки свойств загрузки
(либо внутри приложения.свойства или переменной среды). Затем убедитесь, что принимающий
контейнер имеет те же настройки.

Замечание: `Jmstemplate` может получить сообщения сразу через свой `receive` метод, но это
работает только одновременно, значить блокирующе. Поэтому рекомендуется использовать контейнер
прослушивателя, например `DefaultMessageListenerContainer`, с фабрикой подключений на основе
кэша, чтобы использовать сообщения асинхронно и с максимальной эффективностью подключения.

Когда он работает, похоронен среди всех журналов, вы должны увидеть эти сообщения:

....
Sending an email message.
Received <Email{to=info@example.com, body=ru}>
....
