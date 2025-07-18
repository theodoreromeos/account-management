package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.models.dto.requests.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.responses.SearchResponse;

public interface OrganizationRegistrationProcessService {

    void saveOrganizationRegistrationProcess(OrganizationRegistrationProcess orgRegistrationProcess);

    SearchResponse<RegistrationProcessResponseDto> searchOrganizationRegistrationProcess(SearchRegistrationProcessRequestDto searchRequest,
                                                                                         int page,
                                                                                         int pageSize);

    void organizationRegistrationDecision(OrganizationRegistrationDecisionRequestDto requestDto);

}
