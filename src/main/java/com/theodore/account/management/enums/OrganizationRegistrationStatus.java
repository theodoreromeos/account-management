package com.theodore.account.management.enums;

public enum OrganizationRegistrationStatus {

    PENDING, APPROVED, REJECTED;

    public static OrganizationRegistrationStatus decisionToStatus(OrganizationRegistrationDecision decision) {
        return switch (decision) {
            case OrganizationRegistrationDecision.APPROVED -> OrganizationRegistrationStatus.APPROVED;
            case OrganizationRegistrationDecision.REJECTED -> OrganizationRegistrationStatus.REJECTED;
        };
    }

}
