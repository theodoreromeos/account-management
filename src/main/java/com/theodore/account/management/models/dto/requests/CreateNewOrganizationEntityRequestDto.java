package com.theodore.account.management.models.dto.requests;

import com.theodore.racingmodel.enums.Country;

public record CreateNewOrganizationEntityRequestDto(CreateOrganizationAdminRequestDto organizationAdmin,
                                                    String organizationName,
                                                    String registrationNumber,
                                                    Country country,
                                                    OrganizationType organizationType) {
    public enum OrganizationType {
        MANUFACTURER, INSURANCE_COMPANY
    }
}
