package com.datatrust.application.ports;

import java.util.UUID;

import com.datatrust.domain.enums.UserRole;

public interface CurrentUserProvider {
    UUID currentUserId();
    UserRole currentUserRole();
}