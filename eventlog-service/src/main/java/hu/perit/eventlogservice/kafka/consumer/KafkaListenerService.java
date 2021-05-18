package hu.perit.eventlogservice.kafka.consumer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import hu.perit.eventlogservice.metrics.MicrometerMetricsService;
import hu.perit.eventlogservice.services.ConsumerSettingsService;
import hu.perit.spvitamin.spring.metrics.MeasurementItem;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KafkaListenerService
{
    @Autowired
    private MicrometerMetricsService metricsService;

    @Autowired
    private ConsumerSettingsService consumerSettingsService;

    @KafkaListener(topics = "eventlog", groupId = "group-01")
    public void listenToEventLogTopic(@Payload List<String> messages, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition)
        throws InterruptedException
    {
        MeasurementItem execTime = new MeasurementItem();

        log.debug(String.format("Received %d Kafka message", messages.size()));

        for (String message : messages)
        {
            this.metricsService.getMetricMessageReceived().increment();
            //log.debug("Received Kafka message: " + message);
        }

        TimeUnit.MILLISECONDS.sleep(this.consumerSettingsService.getProcessingDelayMillis());
        this.metricsService.getMetricMessageReceived().pushPerformance(execTime);
    }
}
