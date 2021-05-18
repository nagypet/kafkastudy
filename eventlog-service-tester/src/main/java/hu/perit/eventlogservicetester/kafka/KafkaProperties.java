package hu.perit.eventlogservicetester.kafka;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Component
@ConfigurationProperties("kafka")
@Slf4j
public class KafkaProperties
{
    private String bootstrapServers = "kafka:9092";
    private String groupId = "group-01";
    private String topic;
    private int sendTimeoutSeconds = 10;
    
    @PostConstruct
    private void postConstruct()
    {
        if (StringUtils.isBlank(topic))
        {
            log.error("kafka.topic is not set!");
        }
    }
}
