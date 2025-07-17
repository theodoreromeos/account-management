package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.models.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.RegistrationProcessResponseDto;
import com.theodore.account.management.models.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.SearchResponse;

public interface OrganizationRegistrationProcessService {

    void saveOrganizationRegistrationProcess(OrganizationRegistrationProcess orgRegistrationProcess);

    SearchResponse<RegistrationProcessResponseDto> searchOrganizationRegistrationProcess(SearchRegistrationProcessRequestDto searchRequest,
                                                                                         int page,
                                                                                         int pageSize);

    void organizationRegistrationDecision(OrganizationRegistrationDecisionRequestDto requestDto);

}
