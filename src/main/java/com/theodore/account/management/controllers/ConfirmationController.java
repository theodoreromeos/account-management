package com.theodore.account.management.controllers;

import com.theodore.account.management.services.ConfirmationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/confirmation")
public class ConfirmationController {

    private final ConfirmationService confirmationService;

    public ConfirmationController(ConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/simple/user")
    public ResponseEntity<String> confirmSimpleUserEmail(@RequestParam @NotBlank String token) {

        confirmationService.confirmSimpleUserEmail(token);

        return ResponseEntity.status(HttpStatus.OK).body("Confirmation successful");
    }

    @PostMapping("/organization/user")
    public ResponseEntity<String> confirmOrganizationUserEmail(@RequestParam @NotBlank String token) {

        confirmationService.confirmOrganizationUserEmail(token);

        return ResponseEntity.status(HttpStatus.OK).body("Confirmation successful");
    }

    @PostMapping("/organization/admin")
    public ResponseEntity<String> organizationAdminConfirmationOnUserEmail(@RequestParam @NotBlank String token) {

        confirmationService.organizationAdminApprovalRequest(token);

        return ResponseEntity.status(HttpStatus.OK).body("Confirmation successful");
    }

}
