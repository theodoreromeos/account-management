package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.mappers.OrganizationRegistrationProcessMapper;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.UserProfileRegistrationContext;
import com.theodore.account.management.models.dto.requests.*;
import com.theodore.account.management.models.dto.responses.RegisteredOrganizationResponseDto;
import com.theodore.account.management.models.dto.responses.RegisteredUserResponseDto;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.racingmodel.saga.SagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final OrganizationService organizationService;
    private final EmailTokenService emailTokenService;
    private final OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final MessagingService messagingService;
    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;
    private final OrganizationRegistrationProcessService organizationRegistrationProcessService;
    private final OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper;
    private final SagaCompensationActionService sagaCompensationActionService;

    public RegistrationServiceImpl(OrganizationService organizationService,
                                   EmailTokenService emailTokenService,
                                   OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService,
                                   AuthServerGrpcClient authServerGrpcClient,
                                   MessagingService messagingService,
                                   UserProfileService userProfileService,
                                   UserProfileMapper userProfileMapper,
                                   OrganizationRegistrationProcessService organizationRegistrationProcessService,
                                   OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper,
                                   SagaCompensationActionService sagaCompensationActionService) {
        this.organizationService = organizationService;
        this.emailTokenService = emailTokenService;
        this.organizationUserRegistrationRequestService = organizationUserRegistrationRequestService;
        this.authServerGrpcClient = authServerGrpcClient;
        this.messagingService = messagingService;
        this.userProfileService = userProfileService;
        this.userProfileMapper = userProfileMapper;
        this.organizationRegistrationProcessService = organizationRegistrationProcessService;
        this.organizationRegistrationProcessMapper = organizationRegistrationProcessMapper;
        this.sagaCompensationActionService = sagaCompensationActionService;
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

        String userEmail = userRequestDto.email() != null ? userRequestDto.email() : "unknown";

        sagaOrchestrator
                .step(
                        () -> {
                            // 1) Create auth user
                            var authUser = authServerGrpcClient.authServerNewSimpleUserRegistration(
                                    new CreateNewSimpleAuthUserRequestDto(userEmail,
                                            userRequestDto.mobileNumber(),
                                            userRequestDto.password()
                                    )
                            );
                            context.setAuthUserId(authUser.id());
                        },
                        () -> {
                            // Compensation: rollback to user credentials from auth-server
                            if (context.getAuthUserId() != null) {
                                String logMsg = "Simple user registration";
                                sagaCompensationActionService.authServerCredentialsRollback(context.getAuthUserId(),
                                        userEmail,
                                        logMsg);
                            }
                        }
                )
                .step(
                        () -> {
                            // 2) Save user profile
                            var newUser = userProfileMapper.createSimpleUserDtoToUserProfile(context.getAuthUserId(), userRequestDto);
                            context.setSavedProfile(userProfileService.saveUserProfile(newUser));
                        },
                        () -> {
                            userProfileService.deleteUserProfile(context.getSavedProfile());
                        }
                )
                .step(
                        () -> {
                            // 3) Send email
                            var token = emailTokenService.createSimpleUserToken(context.getSavedProfile());
                            var link = String.format("%s/simple?token=%s", baseUrl(), token);
                            var confirmationEmail = new EmailDto(List.of(userEmail), "User Registration Confirmation", link);
                            messagingService.sendToEmailService(confirmationEmail);
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

        String userEmail = userRequestDto.email() != null ? userRequestDto.email() : "unknown";

        sagaOrchestrator
                .step(
                        () -> {
                            // 1) Create auth user
                            var orgAuthUserRequest = new CreateNewOrganizationAuthUserRequestDto(userEmail,
                                    userRequestDto.mobileNumber(),
                                    userRequestDto.password(),
                                    organization.getRegistrationNumber());
                            var authUser = authServerGrpcClient.authServerNewOrganizationUserRegistration(
                                    orgAuthUserRequest, RoleType.SIMPLE_USER
                            );
                            context.setAuthUserId(authUser.id());
                        },
                        () -> {
                            // Compensation: rollback to user credentials from auth-server
                            if (context.getAuthUserId() != null) {
                                String logMsg = "Organization user registration";
                                sagaCompensationActionService.authServerCredentialsRollback(context.getAuthUserId(),
                                        userEmail,
                                        logMsg);
                            }
                        }
                )
                .step(
                        () -> {
                            // 2) Save user profile
                            var newUser = userProfileMapper.createOrganizationUserDtoToUserProfile(context.getAuthUserId(),
                                    userRequestDto,
                                    organization
                            );
                            context.setSavedProfile(userProfileService.saveUserProfile(newUser));
                        },
                        () -> {
                            userProfileService.deleteUserProfile(context.getSavedProfile());
                        }
                )
                .step(
                        () -> {
                            // 3) Save the registration request
                            OrganizationUserRegistrationRequest registrationRequest = new OrganizationUserRegistrationRequest();
                            registrationRequest.setOrganizationRegistrationNumber(organization.getRegistrationNumber());
                            registrationRequest.setOrgUserEmail(userEmail);

                            var savedRegistrationRequest = organizationUserRegistrationRequestService
                                    .saveOrganizationUserRegistrationRequest(registrationRequest);

                            context.setRegistrationRequest(savedRegistrationRequest);
                        },
                        () -> {
                            organizationUserRegistrationRequestService.deleteOrganizationUserRegistrationRequest(context.getRegistrationRequest());
                        }
                )
                .step(
                        () -> {
                            // 4) Send to email service
                            var emailToken = emailTokenService.createOrganizationUserToken(
                                    context.getSavedProfile().getOrganization(),
                                    context.getSavedProfile().getId(),
                                    context.getSavedProfile().getEmail(),
                                    AccountConfirmedBy.USER
                            );
                            var link = String.format("%s/organization/user?token=%s", baseUrl(), emailToken);//todo
                            var confirmationEmail = new EmailDto(List.of(userEmail), "User Registration Confirmation", link);
                            messagingService.sendToEmailService(confirmationEmail);
                            LOGGER.trace("THE TOKEN : {}", emailToken);//todo: remove
                        },
                        () -> {
                        }
                );

        sagaOrchestrator.run();

        return new RegisteredUserResponseDto(context.getSavedProfile().getEmail(), context.getSavedProfile().getMobileNumber());
    }

    @Override
    public RegisteredOrganizationResponseDto registerNewOrganizationEntity(CreateNewOrganizationEntityRequestDto newOrganizationRequestDto) {
        LOGGER.info("Registration process for new organization : {}", newOrganizationRequestDto.organizationName());

        var response = new RegisteredOrganizationResponseDto(newOrganizationRequestDto.organizationName(),
                newOrganizationRequestDto.registrationNumber());

        if (organizationService.existsByRegistrationNumber(newOrganizationRequestDto.registrationNumber())) {
            // returns the dto normally so that no organization registration number can be guessed
            return response;
        }
        OrganizationRegistrationProcess orgRegistrationProcess = organizationRegistrationProcessMapper
                .requestDtoToEntity(newOrganizationRequestDto);

        organizationRegistrationProcessService.saveOrganizationRegistrationProcess(orgRegistrationProcess);
        // organization registration request successful
        return response;
    }

    private String baseUrl() {//todo remove it
        return "http://localhost/account-management/confirmation";
    }

}
