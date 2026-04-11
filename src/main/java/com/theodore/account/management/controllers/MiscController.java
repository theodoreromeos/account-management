package com.theodore.account.management.controllers;

import com.theodore.account.management.services.ProfileManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/misc")
public class MiscController {

    private final ProfileManagementService profileManagementService;

    public MiscController(ProfileManagementService profileManagementService) {
        this.profileManagementService = profileManagementService;
    }

    @GetMapping("/driver-email/{userId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<String> getUserEmailFromId(@PathVariable String userId) {
        return ResponseEntity.ok().body(profileManagementService.getUserEmailByUserId(userId));
    }

}
