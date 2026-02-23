package io.github.joaosimsic.infrastructure.adapters.input.web.requests;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MarkReadRequest(
    @NotNull UUID messageId
) {}
