package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.events.auth.EmailUpdatedEvent;
import io.github.joaosimsic.events.auth.UserRegisteredEvent;

public interface EventPublisherPort {
  void publishUserRegistered(UserRegisteredEvent event);

  void publishUserEmailUpdated(EmailUpdatedEvent event);
}
