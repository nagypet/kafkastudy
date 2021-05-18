# kafkastudy

For demo purposes we will build an eventlog service, which is basically a spring boot application listening on a Kafka topic. The performance tester is also a spring boot app, but this one is a command line runner, which publishes messages in 30 threads. I have choosen this use case consciously, where a message queue pattern would be more appropriate to see, if a message-queue-like behaviour can be implemented with Kafka. Of course we also need DEO (Delivery Exactly Once) semantics.

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

```
@EnableKafka
@Configuration
public class KafkaProducerConfig
{

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, String> producerFactory()
    {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

Publishing a message:
```
String processID = UUID.randomUUID().toString();
KafkaProperties kafkaProperties = SpringContext.getBean(KafkaProperties.class);
KafkaTemplate<String, String> kafkaTemplate = SpringContext.getBean(KafkaTemplate.class);

kafkaTemplate.send(kafkaProperties.getTopic(), processID);
```

It seems to be real fast!!! The tester is configured to publish 500.000 messages in a batch in 30 threads. The processing of such a batch took less then 3 seconds.
```
                       _   _                                       _            _            _            
   _____   _____ _ __ | |_| | ___   __ _       ___  ___ _ ____   _(_) ___ ___  | |_ ___  ___| |_ ___ _ __ 
  / _ \ \ / / _ \ '_ \| __| |/ _ \ / _` |_____/ __|/ _ \ '__\ \ / / |/ __/ _ \ | __/ _ \/ __| __/ _ \ '__|
 |  __/\ V /  __/ | | | |_| | (_) | (_| |_____\__ \  __/ |   \ V /| | (_|  __/ | ||  __/\__ \ ||  __/ |   
  \___| \_/ \___|_| |_|\__|_|\___/ \__, |     |___/\___|_|    \_/ |_|\___\___|  \__\___||___/\__\___|_|   
                                   |___/                                                                  
                                        project

AdoptOpenJDK 11.0.9.1+1
Spring-Boot: 2.4.5
eventlog-service-tester: 1.0.0-SNAPSHOT

Author: Peter Nagy <nagy.peter.home@gmail.com>

2021-05-15 09:28:11.529 INFO  --- [main           ] entLogServicetesterApplication  55 : Starting EventLogServicetesterApplication v1.0.0-SNAPSHOT using Java 11.0.9.1 on NOT-042 with PID 1380 (C:\np\github\kafkastudy\eventlog-service-tester\build\install\eventlog-service-tester\lib\eventlog-service-tester-1.0.0-SNAPSHOT.jar started by NagyPeter in c:\np\github\kafkastudy\eventlog-service-tester\build\install\eventlog-service-tester\bin) 
2021-05-15 09:28:11.533 INFO  --- [main           ] entLogServicetesterApplication 675 : No active profile set, falling back to default profiles: default 
2021-05-15 09:28:11.611 WARN  SPR [kground-preinit] .j.Jackson2ObjectMapperBuilder 127 : For Jackson Kotlin classes support please add "com.fasterxml.jackson.module:jackson-module-kotlin" to the classpath 
2021-05-15 09:28:12.704 INFO  SPR [main           ] o.s.b.w.e.t.TomcatWebServer    108 : Tomcat initialized with port(s): 8080 (http) 
2021-05-15 09:28:13.302 INFO  SPR [main           ] o.s.b.w.e.t.TomcatWebServer    220 : Tomcat started on port(s): 8080 (http) with context path '' 
2021-05-15 09:28:13.314 INFO  --- [main           ] entLogServicetesterApplication  61 : Started EventLogServicetesterApplication in 2.093 seconds (JVM running for 2.455) 
2021-05-15 09:28:13.700 INFO  --- [main           ] h.p.e.MeasurementStats          61 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:13.700 INFO  --- [main           ] h.p.e.MeasurementStats          62 : |mode                |elapsed |success |failure | speed    |                    |average |max     |min     | 
2021-05-15 09:28:13.701 INFO  --- [main           ] h.p.e.MeasurementStats          64 : |                    |        |pcs     |pcs     | call/min |                    |ms      |ms      |ms      | 
2021-05-15 09:28:13.701 INFO  --- [main           ] h.p.e.MeasurementStats          67 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:17.337 INFO  --- [ool-2-thread-24] h.p.e.MeasurementStats          78 : |SERVICE             |00:00:04| 500,000|       0| 7,500,000|                   0|      27|      79|       0| 
2021-05-15 09:28:17.512 INFO  --- [main           ] h.p.e.Runner                    78 : Performance test took: 4.19 seconds. 
2021-05-15 09:28:17.513 INFO  --- [main           ] h.p.e.Runner                    47 : Waiting 10 seconds... 
2021-05-15 09:28:27.528 INFO  --- [main           ] h.p.e.MeasurementStats          61 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:27.528 INFO  --- [main           ] h.p.e.MeasurementStats          62 : |mode                |elapsed |success |failure | speed    |                    |average |max     |min     | 
2021-05-15 09:28:27.528 INFO  --- [main           ] h.p.e.MeasurementStats          64 : |                    |        |pcs     |pcs     | call/min |                    |ms      |ms      |ms      | 
2021-05-15 09:28:27.528 INFO  --- [main           ] h.p.e.MeasurementStats          67 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:30.282 INFO  --- [pool-3-thread-5] h.p.e.MeasurementStats          78 : |SERVICE             |00:00:02| 500,000|       0|15,000,000|                   0|       2|       8|       0| 
2021-05-15 09:28:30.385 INFO  --- [main           ] h.p.e.Runner                    78 : Performance test took: 2.87 seconds. 
2021-05-15 09:28:30.385 INFO  --- [main           ] h.p.e.Runner                    47 : Waiting 10 seconds... 
2021-05-15 09:28:40.410 INFO  --- [main           ] h.p.e.MeasurementStats          61 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:40.410 INFO  --- [main           ] h.p.e.MeasurementStats          62 : |mode                |elapsed |success |failure | speed    |                    |average |max     |min     | 
2021-05-15 09:28:40.410 INFO  --- [main           ] h.p.e.MeasurementStats          64 : |                    |        |pcs     |pcs     | call/min |                    |ms      |ms      |ms      | 
2021-05-15 09:28:40.410 INFO  --- [main           ] h.p.e.MeasurementStats          67 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:43.143 INFO  --- [pool-4-thread-5] h.p.e.MeasurementStats          78 : |SERVICE             |00:00:02| 500,000|       0|15,000,000|                   0|       2|       5|       0| 
2021-05-15 09:28:43.352 INFO  --- [main           ] h.p.e.Runner                    78 : Performance test took: 2.96 seconds. 
2021-05-15 09:28:43.352 INFO  --- [main           ] h.p.e.Runner                    47 : Waiting 10 seconds... 
2021-05-15 09:28:53.446 INFO  --- [main           ] h.p.e.MeasurementStats          61 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:53.446 INFO  --- [main           ] h.p.e.MeasurementStats          62 : |mode                |elapsed |success |failure | speed    |                    |average |max     |min     | 
2021-05-15 09:28:53.446 INFO  --- [main           ] h.p.e.MeasurementStats          64 : |                    |        |pcs     |pcs     | call/min |                    |ms      |ms      |ms      | 
2021-05-15 09:28:53.446 INFO  --- [main           ] h.p.e.MeasurementStats          67 : +--------------------+--------+--------+--------+----------+--------------------+--------+--------+--------+ 
2021-05-15 09:28:55.738 INFO  --- [ool-5-thread-30] h.p.e.MeasurementStats          78 : |SERVICE             |00:00:02| 500,000|       0|15,000,000|                   0|       4|      10|       0| 
2021-05-15 09:28:55.875 INFO  --- [main           ] h.p.e.Runner                    78 : Performance test took: 2.52 seconds. 
2021-05-15 09:28:55.877 INFO  --- [main           ] h.p.e.Runner                    47 : Waiting 10 seconds... 
Terminate batch job (Y/N)? 
```

The consumer receives batches of the size 500.
```
@Component
@Slf4j
public class KafkaListenerService
{

    @KafkaListener(topics = "eventlog", groupId = "group-01")
    public void listenToEventLogTopic(@Payload List<String> messages, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition)
    {
        log.debug(String.format("Received %d Kafka message", messages.size()));
        
//        for (String message : messages)
//        {
//            log.debug("Received Kafka message: " + message);
//        }
    }
}
```

```
2021-05-15 09:28:59.237 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.239 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.241 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.243 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.245 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.248 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.249 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.251 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.253 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.256 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 500 Kafka message 
2021-05-15 09:28:59.259 DEBUG --- [ntainer#0-0-C-1] h.p.e.k.c.KafkaListenerService  21 : Received 490 Kafka message 
```

Cool!

## Monitoring the consumer with Prometheus/Grafana

```
c:\np\github\kafkastudy\docker-compose\kafkastudy>docker ps
CONTAINER ID        IMAGE                                       COMMAND                  CREATED             STATUS                 PORTS                                                NAMES
b483beec477c        kafkastudy-eventlog-service                 "sh ./eventlog-servi…"   20 minutes ago      Up 20 minutes          8080/tcp, 0.0.0.0:8400->8400/tcp                     kafkastudy-eventlog-service
7758a4e62943        kafkastudy-grafana                          "/run.sh"                20 minutes ago      Up 20 minutes          0.0.0.0:3000->3000/tcp                               kafkastudy-grafana
3f2dd567080e        kafkastudy-prometheus                       "/bin/prometheus --c…"   2 hours ago         Up 2 hours             0.0.0.0:9090->9090/tcp                               kafkastudy-prometheus
0da08556e33e        prom/node-exporter:v0.18.1                  "/bin/node_exporter …"   2 hours ago         Up 2 hours             0.0.0.0:9100->9100/tcp                               kafkastudy-nodeexporter
4550bc961c68        gcr.io/google-containers/cadvisor:v0.36.0   "/usr/bin/cadvisor -…"   2 hours ago         Up 2 hours (healthy)   0.0.0.0:8080->8080/tcp                               kafkastudy-cadvisor
1e6421de65c1        wurstmeister/kafka:latest                   "start-kafka.sh"         2 hours ago         Up 2 hours             0.0.0.0:9092->9092/tcp                               kafkastudy-kafka
14665f98c50c        wurstmeister/zookeeper                      "/bin/sh -c '/usr/sb…"   2 hours ago         Up 2 hours             22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   kafkastudy-zookeper
```
The performance is awesome.
![](https://github.com/nagypet/kafkastudy/blob/main/doc/pics/eventlog-service.jpg)

The memory usage of the kafka container is a bit disappointing. I do not know if it ever releases the memory.
![](https://github.com/nagypet/kafkastudy/blob/main/doc/pics/kafka_memory_usage.jpg)

## Max retention size

```
KAFKA_LOG_RETENTION_BYTES: 1073741824
```

## Kafka-UI

```
#####################################################################################                 
kafka-ui:
#####################################################################################                 
	container_name: kafkastudy-kafka-ui
	image: provectuslabs/kafka-ui
	ports:
	  - "5500:8080"
	restart: always
	environment:
	  - KAFKA_CLUSTERS_0_NAME=local
	  - KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:9092
	  - KAFKA_CLUSTERS_0_ZOOKEEPER=zookeeper:2181
	networks: 
		- back-tier-net
	hostname: kafka-ui
```


## Flow control between the publisher and the consumer

Kafka is based on a publish-and-forget principle. The publisher will keep sending messages regardless of the messages were already processed by the consumer. If the log is full, oldest messages will be deleted even if they were never consumed. Of course for an event log service this is not acceptable. For implementing a message-queue-like behavour we need the followings:
- Flow control: publisher must stop sending new messages if the log is full
- Acknowledge on the publisher side
- Good quality commit mechanism on the consumer side to ensure DEO semantics

Flow control can be implemented by calculating the consumer lag of a topic. The lag is the difference between log-end-offset and committed-offset by the consumer. If the publisher is sending with a much higher rate, then the consumer is able to process, the lag will increase continually. 

### KafkaConsumerMonitor Service

This service deliveres the consumer lag on the publisher side.

```
@Service
public class KafkaConsumerMonitor
{
    private Long currentConsumerLag = 0L;
    private TimeoutLatch timeoutLatch = new TimeoutLatch(5000L);

    private final ThreadLocal<KafkaConsumer<?, ?>> threadLocalConsumer = new ThreadLocal<>()
    {
        protected KafkaConsumer<?, ?> initialValue()
        {
            KafkaProperties kafkaProperties = SpringContext.getBean(KafkaProperties.class);
            return createNewConsumer(kafkaProperties.getBootstrapServers(), kafkaProperties.getGroupId());
        }
    };


    /**
     * 
     * @return
     */
    public synchronized long getConsumerLag()
    {
        if (timeoutLatch.isOpen())
        {
            this.timeoutLatch.setClosed();

            Map<TopicPartition, PartionOffsets> offsets = getConsumerGroupOffsets();

            this.currentConsumerLag = offsets.values().stream() //
                .map(po -> (po.endOffset - po.currentOffset)).collect(Collectors.summingLong(Long::longValue));
        }

        return this.currentConsumerLag;
    }


    /**
     * getConsumerGroupOffsets()
     * 
     * @return Map<TopicPartition, PartionOffsets>
     */
    public Map<TopicPartition, PartionOffsets> getConsumerGroupOffsets()
    {
        KafkaProperties kafkaProperties = SpringContext.getBean(KafkaProperties.class);

        Map<TopicPartition, Long> logEndOffset = getLogEndOffset(threadLocalConsumer.get(), kafkaProperties.getTopic());

        BinaryOperator<PartionOffsets> mergeFunction = (a, b) -> {
            throw new IllegalStateException();
        };

        Map<TopicPartition, OffsetAndMetadata> commitedOffsets = threadLocalConsumer.get().committed(logEndOffset.keySet());

        Map<TopicPartition, PartionOffsets> result = logEndOffset.entrySet().stream() //
            .collect(Collectors.toMap( //
                entry -> (entry.getKey()), //
                entry -> {
                    OffsetAndMetadata committedOffset = commitedOffsets.get(entry.getKey());
                    return new PartionOffsets(entry.getValue(), committedOffset != null ? committedOffset.offset() : 0,
                        entry.getKey().partition(), kafkaProperties.getTopic());
                }, mergeFunction));

        return result;
    }


    private Map<TopicPartition, Long> getLogEndOffset(KafkaConsumer<?, ?> consumer, String topic)
    {
        Map<TopicPartition, Long> endOffsets = new ConcurrentHashMap<>();
        List<PartitionInfo> partitionInfoList = consumer.partitionsFor(topic);
        List<TopicPartition> topicPartitions = partitionInfoList.stream().map(pi -> new TopicPartition(topic, pi.partition())).collect(
            Collectors.toList());
        consumer.assign(topicPartitions);
        consumer.seekToEnd(topicPartitions);
        topicPartitions.forEach(topicPartition -> endOffsets.put(topicPartition, consumer.position(topicPartition)));
        return endOffsets;
    }


    private static KafkaConsumer<?, ?> createNewConsumer(String bootstrapServers, String groupId)
    {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }


    @Getter
    @RequiredArgsConstructor
    public static class PartionOffsets
    {
        private final long endOffset;
        private final long currentOffset;
        private final int partion;
        private final String topic;
    }
}
```

The publisher works like this:

```
    protected Boolean execute() throws Exception
    {
        try (Took took = new Took(false))
        {
            String processID = UUID.randomUUID().toString();
            KafkaProperties kafkaProperties = SpringContext.getBean(KafkaProperties.class);
            KafkaTemplate<String, String> kafkaTemplate = SpringContext.getBean(KafkaTemplate.class);

            KafkaConsumerMonitor monitor = SpringContext.getBean(KafkaConsumerMonitor.class);
            if (monitor.getConsumerLag() < 100_000)
            {
                ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(kafkaProperties.getTopic(), processID);

                // Waiting for the acknowledge from Kafka...
                future.get(kafkaProperties.getSendTimeoutSeconds(), TimeUnit.SECONDS);

                this.stats.incrementSuccessCount();
                this.stats.pushExecTimeMillis(took.getDuration());
                this.stats.logIt();
            }
            else
            {
                TimeUnit.SECONDS.sleep(1);
                this.stats.incrementFailureCount();
                this.stats.logIt();
            }
            return null; // NOSONAR
        }
        catch (Exception ex)
        {
            this.stats.incrementFailureCount();
            this.stats.logIt();
            throw ex;
        }
    }
```

And this is working just great! I have implemented a REST endpoint for setting consumer processing delay in the eventlog-service. I have tested with 0ms, 10ms, 50ms, 40ms and 20ms delays. See the results:

![](https://github.com/nagypet/kafkastudy/blob/main/doc/pics/eventlog-service_with_puback_and_lagcontrol.jpg)

What we can observe:
- The CPU load is higher if we run without any delay. This is because the listener polls Kafka continously.
- With 50ms delay, the lag jumps up to the 100.000 threadhold, sometimes it even get higher. This is because our monitor deliveres a new lag value only in every 5 seconds, and obviously within this 5 seconds the publisher is able to send another 50.000 messages to Kafka. But its not a problem, with the current maximal log size (1GB) the log can contain up to 30 millions of messages.

## Open questions
- Commiting offsets on the consumer side
- Memory consumption
