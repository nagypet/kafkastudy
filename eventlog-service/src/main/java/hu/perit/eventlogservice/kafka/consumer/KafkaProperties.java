package hu.perit.eventlogservice.kafka.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("kafka")
public class KafkaProperties
{
    private String bootstrapServers = "kafka:9092";
    private String groupId = "group-01";
    private String topic;
}
