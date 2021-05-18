package hu.perit.eventlogservice.services;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class ConsumerSettingsService
{
    private long processingDelayMillis = 0;
}
