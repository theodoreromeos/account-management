package com.theodore.account.management.controllers;

import com.theodore.account.management.models.dto.requests.ConfirmOrgAdminEmailRequestDto;
import com.theodore.account.management.services.ConfirmationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirmation")
public class ConfirmationController {

    private final ConfirmationService confirmationService;

    public ConfirmationController(ConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/simple-user")
    public ResponseEntity<Void> confirmSimpleUserEmail(@RequestParam @NotBlank String token) {
        confirmationService.confirmSimpleUserEmail(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/org-user")
    public ResponseEntity<Void> confirmOrganizationUserEmailByUser(@RequestParam @NotBlank String token) {
        confirmationService.confirmOrganizationUserEmailByUser(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/org-user/admin")
    public ResponseEntity<Void> organizationUserConfirmationByOrganization(@RequestParam @NotBlank String token) {
        confirmationService.confirmOrganizationUserEmailByOrganization(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/org-admin")
    public ResponseEntity<Void> organizationAdminEmailConfirmation(@RequestParam @NotBlank String token,
                                                                   @RequestBody @Valid ConfirmOrgAdminEmailRequestDto request) {
        confirmationService.confirmOrganizationAdminEmail(request, token);
        return ResponseEntity.noContent().build();
    }

}
