package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.models.dto.requests.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.models.dto.responses.SearchResponse;

public interface OrganizationRegistrationProcessService {

    void saveOrganizationRegistrationProcess(OrganizationRegistrationProcess orgRegistrationProcess);

    /**
     * Search for Organization Registration Processes with criteria.
     *
     * @param searchRequest is the criteria of the search.
     * @param page          which page to show
     * @param pageSize      how many elements are in a page
     */
    SearchResponse<RegistrationProcessResponseDto> searchOrganizationRegistrationProcess(SearchRegistrationProcessRequestDto searchRequest,
                                                                                         int page,
                                                                                         int pageSize);

    /**
     * Approve or Reject decision from a system admin for an organization registration process.
     *
     * @param requestDto contains the id of the Organization Registration Processes and the decision made by the system admin
     */
    void organizationRegistrationDecision(OrganizationRegistrationDecisionRequestDto requestDto);

}
