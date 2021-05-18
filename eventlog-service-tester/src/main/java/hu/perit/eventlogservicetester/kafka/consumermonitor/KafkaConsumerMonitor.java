package hu.perit.eventlogservicetester.kafka.consumermonitor;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import hu.perit.eventlogservicetester.kafka.KafkaProperties;
import hu.perit.spvitamin.spring.config.SpringContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class KafkaConsumerMonitor
{

    @Getter
    @RequiredArgsConstructor
    public static class PartionOffsets
    {
        private final long endOffset;
        private final long currentOffset;
        private final int partion;
        private final String topic;
    }


    /**
     * getConsumerGroupOffsets()
     * 
     * @return Map<TopicPartition, PartionOffsets>
     */
    public Map<TopicPartition, PartionOffsets> getConsumerGroupOffsets()
    {
        KafkaProperties kafkaProperties = SpringContext.getBean(KafkaProperties.class);

        KafkaConsumer<?, ?> consumer = createNewConsumer(kafkaProperties.getBootstrapServers(), kafkaProperties.getGroupId());

        try
        {

            Map<TopicPartition, Long> logEndOffset = getLogEndOffset(consumer, kafkaProperties.getTopic());

            BinaryOperator<PartionOffsets> mergeFunction = (a, b) -> {
                throw new IllegalStateException();
            };

            Map<TopicPartition, OffsetAndMetadata> commitedOffsets = consumer.committed(logEndOffset.keySet());

            Map<TopicPartition, PartionOffsets> result = logEndOffset.entrySet().stream() //
                .collect(Collectors.toMap( //
                    entry -> (entry.getKey()), //
                    entry -> {
                        OffsetAndMetadata committedOffset = commitedOffsets.get(entry.getKey());
                        return new PartionOffsets(entry.getValue(), committedOffset != null ? committedOffset.offset() : 0, entry.getKey().partition(),
                            kafkaProperties.getTopic());
                    }, mergeFunction));

            return result;
        }
        finally
        {
            if (consumer != null)
            {
                consumer.close();
            }
        }
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
}