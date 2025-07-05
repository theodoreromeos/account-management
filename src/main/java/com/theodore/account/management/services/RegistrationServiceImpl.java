package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.models.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.RegisteredUserResponseDto;
import com.theodore.account.management.models.UserProfileRegistrationContext;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
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

    private final OrganizationRepository organizationRepository;
    private final EmailTokenServiceImpl emailTokenService;
    private final OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    private final AuthServerClient authServerClient;
    private final UserManagementEmailMessagingService userManagementEmailMessagingService;
    private final UserProfileService userProfileService;

    public RegistrationServiceImpl(OrganizationRepository organizationRepository,
                                   EmailTokenServiceImpl emailTokenService,
                                   OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository,
                                   AuthServerClient authServerClient,
                                   UserManagementEmailMessagingService userManagementEmailMessagingService,
                                   UserProfileService userProfileService) {
        this.organizationRepository = organizationRepository;
        this.emailTokenService = emailTokenService;
        this.organizationUserRegistrationRequestRepository = organizationUserRegistrationRequestRepository;
        this.authServerClient = authServerClient;
        this.userManagementEmailMessagingService = userManagementEmailMessagingService;
        this.userProfileService = userProfileService;
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
                            determineRollbackCredentials(context.getAuthUserId(), context.getSavedProfile().getEmail());
                        }
                )
                .step(
                        () -> {
                            // 2) Save user profile
                            var profile = new UserProfile(context.getAuthUserId(), userRequestDto.email(), userRequestDto.mobileNumber());
                            profile.setName(userRequestDto.name());
                            profile.setSurname(userRequestDto.surname());//todo : mapper
                            context.setSavedProfile(userProfileService.saveUserProfile(profile));
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
                            System.out.println("THE LINK: " + link);
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
            return new RegisteredUserResponseDto(userRequestDto.email(), userRequestDto.mobileNumber());
        }

        Organization organization = organizationRepository.findByRegistrationNumberIgnoreCase(userRequestDto.organizationRegNumber())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

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
                            determineRollbackCredentials(context.getAuthUserId(), context.getSavedProfile().getEmail());
                        }
                )
                .step(
                        () -> {
                            // 2) Save user profile
                            var newUser = new UserProfile(context.getAuthUserId(), userRequestDto.email(), userRequestDto.mobileNumber(), organization);
                            newUser.setName(userRequestDto.name());
                            newUser.setSurname(userRequestDto.surname());
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

                            organizationUserRegistrationRequestRepository.save(registrationRequest);
                        },
                        () -> {
                        }
                )
                .step(
                        () -> {
                            // 4) Send to email service
                            var emailToken = emailTokenService.createToken(context.getSavedProfile(), RegistrationEmailPurpose.ORGANIZATION_USER.toString());
                            var link = String.format("%s/organization/user?token=%s", baseUrl(), emailToken);//todo
                            LOGGER.trace("THE LINK : {}", link);//todo: remove
                        },
                        () -> {
                        }
                );

        sagaOrchestrator.run();

        return new RegisteredUserResponseDto(context.getSavedProfile().getEmail(), context.getSavedProfile().getMobileNumber());
    }

    private void determineRollbackCredentials(String userId, String email) {
        if (userId != null) {
            LOGGER.info("Registration process failed.Rolling back credentials from auth server for user : {} ", email);
            var rollbackEvent = new CredentialsRollbackEventDto(userId);
            userManagementEmailMessagingService.rollbackCredentialsSave(rollbackEvent);
        }
    }

    private String baseUrl() {//todo remove it
        return "http://localhost/account-management/confirmation";
    }

}
