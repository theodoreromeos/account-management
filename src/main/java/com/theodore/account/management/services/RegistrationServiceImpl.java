package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.RegisteredUserResponseDto;
import com.theodore.account.management.models.UserProfileRegistrationContext;
import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.racingmodel.models.CreateNewOrganizationAuthUserRequestDto;
import com.theodore.racingmodel.models.CreateNewSimpleAuthUserRequestDto;
import com.theodore.racingmodel.saga.SagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final OrganizationService organizationService;
    private final EmailTokenService emailTokenService;
    private final OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;
    private final AuthServerClient authServerClient;
    private final UserManagementEmailMessagingService userManagementEmailMessagingService;
    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;

    public RegistrationServiceImpl(OrganizationService organizationService,
                                   EmailTokenService emailTokenService,
                                   OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService,
                                   AuthServerClient authServerClient,
                                   UserManagementEmailMessagingService userManagementEmailMessagingService,
                                   UserProfileService userProfileService,
                                   UserProfileMapper userProfileMapper) {
        this.organizationService = organizationService;
        this.emailTokenService = emailTokenService;
        this.organizationUserRegistrationRequestService = organizationUserRegistrationRequestService;
        this.authServerClient = authServerClient;
        this.userManagementEmailMessagingService = userManagementEmailMessagingService;
        this.userProfileService = userProfileService;
        this.userProfileMapper = userProfileMapper;
    }

    //removed @Transactional from here because the exception was thrown at the end so saga did not pick it
    @Override
    public RegisteredUserResponseDto registerNewSimpleUser(CreateNewSimpleUserRequestDto userRequestDto) {

        LOGGER.info("Registration process for simple user : {}", userRequestDto.email());

        if (userProfileService.userProfileExistsByEmailAndMobileNumber(userRequestDto.email(), userRequestDto.mobileNumber())) {
            return new RegisteredUserResponseDto(userRequestDto.email(), userRequestDto.mobileNumber());
        }

        var context = new UserProfileRegistrationContext();
        var sagaOrchestrator = new SagaOrchestrator();

        sagaOrchestrator
                .step(
                        () -> {
                            // 1) Create auth user
                            var authUser = authServerClient.authServerNewSimpleUserRegistration(
                                    new CreateNewSimpleAuthUserRequestDto(userRequestDto.email(), userRequestDto.mobileNumber(), userRequestDto.password())
                            );
                            context.setAuthUserId(authUser.id());
                        },
                        () -> {
                            // Compensation: rollback to user credentials from auth-server
                            if (context.getAuthUserId() != null) {
                                var email = context.getSavedProfile() != null ? context.getSavedProfile().getEmail() : "unknown";
                                LOGGER.info("Simple user registration process failed. Rolling back credentials from auth server for user : {} ", email);
                                var rollbackEvent = new CredentialsRollbackEventDto(context.getAuthUserId());
                                userManagementEmailMessagingService.rollbackCredentialsSave(rollbackEvent);
                            }
                        }
                )
                .step(
                        () -> {
                            // 2) Save user profile
                            var newUser = userProfileMapper.simpleUserDtoToUserProfile(context.getAuthUserId(), userRequestDto);
                            context.setSavedProfile(userProfileService.saveUserProfile(newUser));
                        },
                        () -> {
                        }
                )
                .step(
                        () -> {
                            // 3) Send email
                            var token = emailTokenService.createToken(context.getSavedProfile(), RegistrationEmailPurpose.PERSONAL.toString());
                            var link = String.format("%s/simple?token=%s", baseUrl(), token);
                            var email = new EmailDto(context.getSavedProfile().getEmail(), "User Registration Confirmation", link);
                            userManagementEmailMessagingService.sendToEmailService(email);
                            LOGGER.trace("THE LINK : {}", link);//todo: remove
                        },
                        () -> {
                        }
                );

        sagaOrchestrator.run();

        return new RegisteredUserResponseDto(context.getSavedProfile().getEmail(), context.getSavedProfile().getMobileNumber());
    }

    //removed @Transactional from here because the exception was thrown at the end so saga did not pick it
    @Override
    public RegisteredUserResponseDto registerNewOrganizationUser(CreateNewOrganizationUserRequestDto userRequestDto) {

        LOGGER.info("Registration process for user : {} working for organization : {}", userRequestDto.email(), userRequestDto.organizationRegNumber());

        if (userProfileService.userProfileExistsByEmailAndMobileNumber(userRequestDto.email(), userRequestDto.mobileNumber())) {
            // returns the dto normally so that no email can be guessed
            return new RegisteredUserResponseDto(userRequestDto.email(), userRequestDto.mobileNumber());
        }

        Organization organization;
        try {
            organization = organizationService.findByRegistrationNumber(userRequestDto.organizationRegNumber());
        } catch (NotFoundException e) {
            // returns the dto normally so that no organization registration number can be guessed
            return new RegisteredUserResponseDto(userRequestDto.email(), userRequestDto.mobileNumber());
        }

        var context = new UserProfileRegistrationContext();
        var sagaOrchestrator = new SagaOrchestrator();

        sagaOrchestrator
                .step(
                        () -> {
                            // 1) Create auth user
                            var authUser = authServerClient.authServerNewOrganizationUserRegistration(
                                    new CreateNewOrganizationAuthUserRequestDto(userRequestDto.email(),
                                            userRequestDto.mobileNumber(),
                                            userRequestDto.password(),
                                            organization.getRegistrationNumber())
                            );
                            context.setAuthUserId(authUser.id());
                        },
                        () -> {
                            // Compensation: rollback to user credentials from auth-server
                            if (context.getAuthUserId() != null) {
                                String email = context.getSavedProfile() != null ? context.getSavedProfile().getEmail() : "unknown";
                                LOGGER.info("Organization user registration process failed. Rolling back credentials from auth server for user : {} ", email);
                                var rollbackEvent = new CredentialsRollbackEventDto(context.getAuthUserId());
                                userManagementEmailMessagingService.rollbackCredentialsSave(rollbackEvent);
                            }
                        }
                )
                .step(
                        () -> {
                            // 2) Save user profile
                            var newUser = userProfileMapper.organizationUserDtoToUserProfile(context.getAuthUserId(), userRequestDto, organization);
                            context.setSavedProfile(userProfileService.saveUserProfile(newUser));
                        },
                        () -> {
                        }
                )
                .step(
                        () -> {
                            // 3) Save the registration request
                            OrganizationUserRegistrationRequest registrationRequest = new OrganizationUserRegistrationRequest();
                            registrationRequest.setCompanyEmail(organization.getEmail());
                            registrationRequest.setOrgUserEmail(context.getSavedProfile().getEmail());

                            organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);
                        },
                        () -> {
                        }
                )
                .step(
                        () -> {
                            // 4) Send to email service
                            var emailToken = emailTokenService.createToken(context.getSavedProfile(), RegistrationEmailPurpose.ORGANIZATION_USER.toString());
                            var link = String.format("%s/organization/user?token=%s", baseUrl(), emailToken);//todo
                            var email = new EmailDto(context.getSavedProfile().getEmail(), "User Registration Confirmation", link);
                            userManagementEmailMessagingService.sendToEmailService(email);
                            LOGGER.trace("THE LINK : {}", link);//todo: remove
                        },
                        () -> {
                        }
                );

        sagaOrchestrator.run();

        return new RegisteredUserResponseDto(context.getSavedProfile().getEmail(), context.getSavedProfile().getMobileNumber());
    }

    private String baseUrl() {//todo remove it
        return "http://localhost/account-management/confirmation";
    }

}
