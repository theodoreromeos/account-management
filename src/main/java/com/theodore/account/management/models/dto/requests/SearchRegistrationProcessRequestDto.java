package com.theodore.account.management.models.dto.requests;

import com.theodore.racingmodel.enums.Country;

public record SearchRegistrationProcessRequestDto(SearchOrganizationRegistrationStatus status,
                                                  String organizationName,
                                                  String registrationNumber,
                                                  Country country) {

    public enum SearchOrganizationRegistrationStatus {
        ALL, PENDING, APPROVED, REJECTED;
    }
}
