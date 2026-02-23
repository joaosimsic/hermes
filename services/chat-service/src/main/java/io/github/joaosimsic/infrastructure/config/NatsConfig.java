package io.github.joaosimsic.infrastructure.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
@Slf4j
public class NatsConfig {

    @Value("${nats.url}")
    private String natsUrl;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        Options options = new Options.Builder()
            .server(natsUrl)
            .connectionTimeout(Duration.ofSeconds(5))
            .reconnectWait(Duration.ofSeconds(1))
            .maxReconnects(-1)
            .connectionListener((conn, type) -> 
                log.info("NATS connection event: {}", type))
            .errorListener(ex -> 
                log.error("NATS error", ex))
            .build();

        Connection connection = Nats.connect(options);
        log.info("Connected to NATS at {}", natsUrl);
        return connection;
    }
}
