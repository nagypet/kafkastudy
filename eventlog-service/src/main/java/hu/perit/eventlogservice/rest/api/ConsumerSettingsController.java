package hu.perit.eventlogservice.rest.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import hu.perit.eventlogservice.config.Constants;
import hu.perit.eventlogservice.services.ConsumerSettingsService;
import hu.perit.spvitamin.core.took.Took;
import hu.perit.spvitamin.spring.logging.AbstractInterfaceLogger;
import hu.perit.spvitamin.spring.security.auth.AuthorizationService;

@RestController
public class ConsumerSettingsController extends AbstractInterfaceLogger implements ConsumerSettingsApi
{

    private final AuthorizationService authorizationService;
    private final ConsumerSettingsService consumerSettingsService;

    protected ConsumerSettingsController(HttpServletRequest httpRequest, AuthorizationService authorizationService,
        ConsumerSettingsService consumerSettingsService)
    {
        super(httpRequest);
        this.authorizationService = authorizationService;
        this.consumerSettingsService = consumerSettingsService;
    }


    //------------------------------------------------------------------------------------------------------------------
    // updateConsumerProcessingDelay
    //------------------------------------------------------------------------------------------------------------------
    @Override
    public void updateConsumerProcessingDelay(Long delayMillis)
    {
        UserDetails user = this.authorizationService.getAuthenticatedUser();
        try (Took took = new Took())
        {
            this.traceIn(null, user.getUsername(), getMyMethodName(), Constants.EVENT_ID_UPDATE_CONSUMERPROCESSING_DELAY,
                String.format("delayMillis: %d", delayMillis));

            this.consumerSettingsService.setProcessingDelayMillis(delayMillis);
        }
        catch (Error | RuntimeException ex)
        {
            this.traceOut(null, user.getUsername(), getMyMethodName(), Constants.EVENT_ID_UPDATE_CONSUMERPROCESSING_DELAY, ex);
            throw ex;
        }
    }


    @Override
    protected String getSubsystemName()
    {
        return Constants.SUBSYSTEM_NAME;
    }
}
