package io.github.joaosimsic.core.exceptions.business;

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

import java.util.UUID;

public class MediaNotFoundException extends BusinessException {
    
    public MediaNotFoundException(UUID mediaId) {
        super("Media not found: " + mediaId);
    }
}
