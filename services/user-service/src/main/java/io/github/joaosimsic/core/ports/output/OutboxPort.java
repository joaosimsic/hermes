package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.OutboxEntry;
import java.util.List;
import java.util.UUID;

public interface OutboxPort {

  void save(Object event, String aggregateId, String aggregateType, String eventType);

  List<OutboxEntry> findUnprocessed(int batchSize);

  void markAsProcessed(List<UUID> ids);

  void markAsFailed(UUID id, String reason);

  void incrementAttempt(UUID id, String lastError);
}

