package hu.perit.eventlogservice.metrics;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import hu.perit.eventlogservice.config.Constants;
import hu.perit.spvitamin.core.exception.UnexpectedConditionException;
import hu.perit.spvitamin.spring.metrics.DualMetric;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Service
@Getter
@Slf4j
public class MicrometerMetricsService
{
    private final List<HealthIndicator> indicators;

    private DualMetric metricMessageReceived;

    public MicrometerMetricsService(MeterRegistry registry, HealthContributorRegistry healthContributorRegistry)
    {
        final String METRIC_HEALTH = Constants.SUBSYSTEM_NAME.toLowerCase() + ".health";

        this.metricMessageReceived = new DualMetric(registry, Constants.SUBSYSTEM_NAME.toLowerCase(), "messagereceived");

        indicators = healthContributorRegistry.stream() //
            .map(c -> this.getIndicatorFromContributor(c)) //
            .collect(Collectors.toList());
        Gauge.builder(METRIC_HEALTH, indicators, MicrometerMetricsService::healthToCode) //
            .description("The current value of the composite health endpoint").register(registry);
    }


    private HealthIndicator getIndicatorFromContributor(NamedContributor<HealthContributor> namedContributor)
    {
        log.debug(String.format("Using health contributor: '%s'", namedContributor.getName()));

        HealthContributor contributor = namedContributor.getContributor();
        if (contributor instanceof HealthIndicator)
        {
            return (HealthIndicator) contributor;
        }

        if (contributor instanceof CompositeHealthContributor)
        {
            CompositeHealthContributor compositeHealthContributor = (CompositeHealthContributor) contributor;
            for (NamedContributor<HealthContributor> elementOfComposite : compositeHealthContributor)
            {
                return getIndicatorFromContributor(elementOfComposite);
            }
        }

        throw new UnexpectedConditionException();
    }


    private static int healthToCode(List<HealthIndicator> indicators)
    {
        for (HealthIndicator indicator : indicators)
        {
            Status status = indicator.health().getStatus();
            if (status.equals(Status.DOWN))
            {
                return 0;
            }
        }

        return 1;
    }
}
