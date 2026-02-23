package io.github.joaosimsic.infrastructure.adapters.input.web.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CreateGroupConversationRequest(
    @NotBlank String name,
    @NotEmpty List<UUID> participantIds
) {}
