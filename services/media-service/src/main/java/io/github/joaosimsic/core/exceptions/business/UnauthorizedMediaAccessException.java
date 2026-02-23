package io.github.joaosimsic.core.exceptions.business;

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

import java.util.UUID;

public class UnauthorizedMediaAccessException extends BusinessException {
    
    public UnauthorizedMediaAccessException(UUID mediaId, UUID userId) {
        super("User " + userId + " is not authorized to access media " + mediaId);
    }
}
