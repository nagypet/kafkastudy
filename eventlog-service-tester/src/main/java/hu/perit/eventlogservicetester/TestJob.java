package hu.perit.eventlogservicetester;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;

import hu.perit.eventlogservicetester.kafka.KafkaProperties;
import hu.perit.spvitamin.core.batchprocessing.BatchJob;
import hu.perit.spvitamin.core.exception.ExceptionWrapper;
import hu.perit.spvitamin.core.took.Took;
import hu.perit.spvitamin.spring.config.SpringContext;

public class TestJob extends BatchJob
{

    private final MeasurementStats stats;

    public TestJob(MeasurementStats stats)
    {
        this.stats = stats;
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Boolean execute() throws Exception
    {
        try (Took took = new Took(false))
        {
            String processID = UUID.randomUUID().toString();
            KafkaProperties kafkaProperties = SpringContext.getBean(KafkaProperties.class);
            KafkaTemplate<String, String> kafkaTemplate = SpringContext.getBean(KafkaTemplate.class);

            kafkaTemplate.send(kafkaProperties.getTopic(), processID);

            this.stats.incrementSuccessCount();
            this.stats.pushExecTimeMillis(took.getDuration());
            this.stats.logIt();
            return null; // NOSONAR
        }
        catch (Exception ex)
        {
            this.stats.incrementFailureCount();
            this.stats.logIt();
            throw ex;
        }
    }


    @Override
    public boolean isFatalException(Throwable ex)
    {
        ExceptionWrapper exception = ExceptionWrapper.of(ex);

        if (exception.causedBy("org.apache.http.conn.ConnectTimeoutException")
            || exception.causedBy("org.apache.http.NoHttpResponseException")
            || exception.causedBy("org.apache.http.conn.HttpHostConnectException"))
        {
            return false;
        }

        return true;
    }
}
