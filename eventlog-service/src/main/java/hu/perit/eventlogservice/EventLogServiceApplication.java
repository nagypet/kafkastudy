package hu.perit.eventlogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import hu.perit.spvitamin.spring.environment.EnvironmentPostProcessor;

@SpringBootApplication
@ComponentScan(basePackages = {"hu.perit.spvitamin", "hu.perit.eventlogservice"})
public class EventLogServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication application = new SpringApplication(EventLogServiceApplication.class);
        application.addListeners(new EnvironmentPostProcessor());
        application.run(args);
    }

}
