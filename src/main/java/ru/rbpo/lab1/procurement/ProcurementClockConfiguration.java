package ru.rbpo.lab1.procurement;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ProcurementClockConfiguration {
    @Bean
    public Clock procurementClock() {
        return Clock.systemUTC();
    }
}
