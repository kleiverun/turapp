package com.ole.turapp.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtils {

    private AuthUtils() {}

    public static Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Throws 403 if the authenticated user is not the owner of the resource. */
    public static void requireOwner(Long resourceOwnerId) {
        if (!currentUserId().equals(resourceOwnerId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
