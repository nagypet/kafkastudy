package hu.perit.eventlogservicetester;

import java.util.UUID;

import hu.perit.eventlogservicetester.config.TesterProperties;
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

        TesterProperties testerProperties = SpringContext.getBean(TesterProperties.class);
    }


    @Override
    protected Boolean execute() throws Exception
    {
        try (Took took = new Took(false))
        {
            String processID = UUID.randomUUID().toString();

            // TODO
            Thread.sleep(10);

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
