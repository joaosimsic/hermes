package io.github.joaosimsic.infrastructure.adapters.input.messaging;

import io.github.joaosimsic.core.domain.User;
import io.github.joaosimsic.core.exceptions.business.ConflictException;
import io.github.joaosimsic.core.ports.input.UserUseCase;
import io.github.joaosimsic.events.auth.EmailUpdatedEvent;
import io.github.joaosimsic.events.auth.UserRegisteredEvent;
import io.github.joaosimsic.infrastructure.config.RabbitConfig;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

  private final UserUseCase userUseCase;

  @RabbitListener(queues = RabbitConfig.AUTH_USER_REGISTERED_QUEUE)
  public void handleUserRegistered(UserRegisteredEvent event) {
    try {
      setTraceId(event.getTraceId());

      log.info(
          "Received AuthUserRegisteredEvent for user: {} with email: {}",
          event.getExternalId(),
          event.getEmail());

      var user =
          User.builder()
              .externalId(event.getExternalId())
              .email(event.getEmail())
              .name(event.getName())
              .build();

      userUseCase.createUser(user);

      log.info("Successfully created local user for external ID: {}", event.getExternalId());
    } catch (ConflictException e) {
      log.info(
          "User already exists for external ID: {}, acknowledging message",
          event.getExternalId());
    } catch (Exception e) {
      log.error(
          "Error processing AuthUserRegisteredEvent for user {}: {}",
          event.getExternalId(),
          e.getMessage());

      throw e;
    } finally {
      MDC.remove("traceId");
    }
  }

  @RabbitListener(queues = RabbitConfig.AUTH_USER_EMAIL_UPDATED_QUEUE)
  public void handleUserEmailUpdated(EmailUpdatedEvent event) {
    try {
      setTraceId(event.getTraceId());

      log.info(
          "Received AuthUserEmailUpdatedEvent for user: {} with new email: {}",
          event.getExternalId(),
          event.getNewEmail());

      userUseCase.updateEmailByExternalId(event.getExternalId(), event.getNewEmail());

      log.info("Successfully updated email for user with external ID: {}", event.getExternalId());
    } catch (Exception e) {
      log.error(
          "Error processing AuthUserEmailUpdatedEvent for user {}: {}",
          event.getExternalId(),
          e.getMessage());

      throw e;
    } finally {
      MDC.remove("traceId");
    }
  }

  private void setTraceId(String traceId) {
    if (traceId != null && !traceId.isBlank()) {
      MDC.put("traceId", traceId);
    } else {
      MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
    }
  }
}
