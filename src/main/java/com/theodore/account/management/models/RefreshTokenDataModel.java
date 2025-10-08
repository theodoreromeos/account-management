package com.theodore.account.management.models;

import java.util.Optional;

public record RefreshTokenDataModel(Optional<String> confirmedBy, String token) {
}
