package com.theodore.account.management.controllers;

import com.theodore.account.management.models.dto.requests.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.models.dto.responses.SearchResponse;
import com.theodore.account.management.services.OrganizationRegistrationProcessService;
import com.theodore.account.management.services.ProfileManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final OrganizationRegistrationProcessService organizationRegistrationProcessService;
    private final ProfileManagementService profileManagementService;

    public AdminController(OrganizationRegistrationProcessService organizationRegistrationProcessService,
                           ProfileManagementService profileManagementService) {
        this.organizationRegistrationProcessService = organizationRegistrationProcessService;
        this.profileManagementService = profileManagementService;
    }

    @PostMapping("/manage")
    public ResponseEntity<String> manageAdminInfo(@RequestBody @Valid UserChangeInformationRequestDto requestDto) {

        profileManagementService.adminProfileManagement(requestDto);

        return ResponseEntity.status(HttpStatus.OK).body("ok");
    }

    @PostMapping("/org-registration/search")
    public ResponseEntity<SearchResponse<RegistrationProcessResponseDto>> searchOrganizationRegistrationRequests(
            @RequestParam int page,
            @RequestParam int pageSize,
            @RequestBody @Valid SearchRegistrationProcessRequestDto searchRequest
    ) {

        var response = organizationRegistrationProcessService.searchOrganizationRegistrationProcess(searchRequest, page, pageSize);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/org-registration/decision")
    public ResponseEntity<String> organizationRegistrationDecision(@RequestBody @Valid OrganizationRegistrationDecisionRequestDto requestDto) {

        organizationRegistrationProcessService.organizationRegistrationDecision(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
    }
}
