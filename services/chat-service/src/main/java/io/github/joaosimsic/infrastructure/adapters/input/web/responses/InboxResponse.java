package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import java.util.List;

public record InboxResponse(
    List<InboxEntryResponse> conversations,
    int totalUnread
) {}
