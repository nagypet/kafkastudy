package hu.perit.eventlogservice.rest.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

import hu.perit.spvitamin.spring.logging.EventLogId;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

public interface ConsumerSettingsApi
{
    String BASE_URL_CONSUMER = "/consumer";

    //------------------------------------------------------------------------------------------------------------------
    // updateConsumerProcessingDelay
    //------------------------------------------------------------------------------------------------------------------
    @PutMapping(BASE_URL_CONSUMER)
    @ApiOperation(value = "updateConsumerProcessingDelay() - Set a new processing time")
    @ApiResponses(value = { //
        @ApiResponse(code = 200, message = "Success"), //
        @ApiResponse(code = 500, message = "Internal server error") //
    })
    @ResponseStatus(value = HttpStatus.OK)
    @EventLogId(eventId = 4)
    void updateConsumerProcessingDelay(@RequestHeader Long delayMillis);
}
