# kafkastudy

## Introduction
Reference: [Introducing Apache Kafka by Adam Mautner](https://imarcats.wordpress.com/2019/02/13/introducing-apache-kafka/)

## Installation
Reference: [Installing Kafka using Docker by Adam Mautner](https://imarcats.wordpress.com/2019/02/13/installing-kafka-using-docker/)

I have slightly modified the `docker-compose.yml`. See in the folder: `kafkastudy\docker-compose\kafkastudy\`.

```
c:\np\github\kafkastudy\docker-compose\kafkastudy>docker ps
CONTAINER ID        IMAGE                       COMMAND                  CREATED             STATUS              PORTS                                                NAMES
080fe10f0c63        wurstmeister/kafka:latest   "start-kafka.sh"         19 minutes ago      Up 19 minutes       0.0.0.0:9092->9092/tcp                               kafkastudy_kafka
cdbc8cb55805        wurstmeister/zookeeper      "/bin/sh -c '/usr/sb…"   19 minutes ago      Up 19 minutes       22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   kafkastudy_zookeper
```

Test the installation from the command line:

`docker exec -it kafkastudy_kafka /bin/bash`

Within the kafka container:
- Optional: install mc in the kafka container: `apk add mc`
- `kafka-topics.sh --list --zookeeper zookeeper:2181`
- `kafka-console-producer.sh --broker-list kafka:9092 --topic test`
- `kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic test --from-beginning`

## EventLogService

Run the eventlog-service app.

Send some message from the kafka shell
```
c:\np\github\kafkastudy\docker-compose\kafkastudy>ks
bash-4.4# kafka-console-producer.sh --broker-list kafka:9092 --topic eventlog
>hello
>szia
>
```

Application log:
```
                       _   _                                       _          
   _____   _____ _ __ | |_| | ___   __ _       ___  ___ _ ____   _(_) ___ ___ 
  / _ \ \ / / _ \ '_ \| __| |/ _ \ / _` |_____/ __|/ _ \ '__\ \ / / |/ __/ _ \
 |  __/\ V /  __/ | | | |_| | (_) | (_| |_____\__ \  __/ |   \ V /| | (_|  __/
  \___| \_/ \___|_| |_|\__|_|\___/ \__, |     |___/\___|_|    \_/ |_|\___\___|
                                   |___/                                      
                                        project

AdoptOpenJDK 11.0.9.1+1
Spring-Boot: 2.4.5
: 

Author: Peter Nagy <nagy.peter.home@gmail.com>

2021-05-15 06:57:13.106 INFO  --- [main           ] p.e.EventLogServiceApplication  55 : Starting EventLogServiceApplication on NOT-042 with PID 15616 (C:\np\github\kafkastudy\eventlog-service\bin\main started by NagyPeter in C:\np\github\kafkastudy\eventlog-service) 
2021-05-15 06:57:13.110 DEBUG --- [main           ] p.e.EventLogServiceApplication  56 : Running with Spring Boot v2.3.2.RELEASE, Spring v5.2.8.RELEASE 
2021-05-15 06:57:13.110 INFO  --- [main           ] p.e.EventLogServiceApplication 651 : No active profile set, falling back to default profiles: default 
2021-05-15 06:57:13.168 DEBUG SPR [main           ] .ConfigFileApplicationListener 222 : Loaded config file 'file:/C:/np/github/kafkastudy/eventlog-service/bin/main/config/application.properties' (classpath:/config/application.properties) 
2021-05-15 06:57:13.395 WARN  SPR [kground-preinit] .j.Jackson2ObjectMapperBuilder 127 : For Jackson Kotlin classes support please add "com.fasterxml.jackson.module:jackson-module-kotlin" to the classpath 
2021-05-15 06:57:15.211 INFO  SPR [main           ] o.s.b.w.e.t.TomcatWebServer    108 : Tomcat initialized with port(s): 8400 (https) 
2021-05-15 06:57:17.735 INFO  SPR [main           ] o.s.b.w.e.t.TomcatWebServer    220 : Tomcat started on port(s): 8400 (https) with context path '' 
2021-05-15 06:57:18.152 INFO  --- [main           ] p.e.EventLogServiceApplication  61 : Started EventLogServiceApplication in 5.541 seconds (JVM running for 6.102) 
2021-05-15 06:57:32.698 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  19 : Received Kafka message: hello 
2021-05-15 06:57:36.258 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  19 : Received Kafka message: szia 
```

Interesting, that default settings seem to mimic delivery semantics 'exactly ones'. If I stop the application for a while, only new messages will be delivered the next time the consumer is started. This has to be analysed and tested more detailled.

## Batch processing on the consumer side

In order to speed up processing, we will use batch delivery on the consumer side.

```
@EnableKafka
@Configuration
public class KafkaConsumerConfig
{
    @Autowired
    private KafkaProperties kafkaProperties;
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory()
    {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true); <== Insert this!!!
        return factory;
    }
}
```

Change the payload to a List of objects:
```
@Component
@Slf4j
public class KafkaListenerService
{

    @KafkaListener(topics = "eventlog", groupId = "group-01")
    public void listenToEventLogTopic(@Payload List<String> messages, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition)
    {
        for (String message : messages)
        {
            log.debug("Received Kafka message: " + message);
        }
    }
}
```

Now if you stop the consumer app and keep pushing some new messages, these will be delivered at once, instead of single messages.
```
2021-05-15 07:52:46.202 INFO  SPR [main           ] o.s.b.w.e.t.TomcatWebServer    220 : Tomcat started on port(s): 8400 (https) with context path '' 
2021-05-15 07:52:46.350 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  23 : Received Kafka message: alma 
2021-05-15 07:52:46.351 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  23 : Received Kafka message: körte 
2021-05-15 07:52:46.351 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  23 : Received Kafka message: szilva 
2021-05-15 07:52:46.351 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  23 : Received Kafka message: barack 
```

### Creating a performance tester application

