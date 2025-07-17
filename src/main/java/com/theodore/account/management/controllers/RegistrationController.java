package com.theodore.account.management.controllers;

import com.theodore.account.management.models.*;
import com.theodore.account.management.services.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/user/simple")
    public ResponseEntity<RegisteredUserResponseDto> registerNewSimpleUser(@RequestBody @Valid CreateNewSimpleUserRequestDto userRequestDto) {

        RegisteredUserResponseDto responseDto = registrationService.registerNewSimpleUser(userRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/user/organization")
    public ResponseEntity<RegisteredUserResponseDto> registerNewOrganizationUser(@RequestBody @Valid CreateNewOrganizationUserRequestDto userRequestDto) {

        RegisteredUserResponseDto responseDto = registrationService.registerNewOrganizationUser(userRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/organization")
    public ResponseEntity<RegisteredOrganizationResponseDto> registerNewOrganization(@RequestBody @Valid CreateNewOrganizationEntityRequestDto newOrganizationRequestDto) {

        RegisteredOrganizationResponseDto responseDto = registrationService.registerNewOrganizationEntity(newOrganizationRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

}
