package com.theodore.account.management.models.dto.requests;

import com.theodore.account.management.enums.OrganizationRegistrationDecision;

public record OrganizationRegistrationDecisionRequestDto(Long id, OrganizationRegistrationDecision decision) {
}
