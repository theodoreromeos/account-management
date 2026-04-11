package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.mappers.OrganizationRegistrationProcessMapper;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.RefreshTokenDataModel;
import com.theodore.account.management.models.UserProfileRegistrationContext;
import com.theodore.account.management.models.dto.requests.*;
import com.theodore.account.management.models.dto.responses.OrgAdminInfoResponseDto;
import com.theodore.account.management.models.dto.responses.RegisteredOrganizationResponseDto;
import com.theodore.account.management.models.dto.responses.RegisteredUserResponseDto;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.infrastructure.common.entities.enums.RoleType;
import com.theodore.infrastructure.common.exceptions.NotFoundException;
import com.theodore.infrastructure.common.saga.SagaOrchestrator;
import com.theodore.infrastructure.common.utils.MobilityUtils;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.queue.common.services.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private static final String USER_NOT_FOUND = "User not found";
    private static final String SUBJECT_REG_CONFIRM = "User Registration Confirmation";

    private static final String CREATE_AUTH_USER_STEP = "create-auth-user";
    private static final String SAVE_USER_PROFILE_STEP = "save-user-profile";
    private static final String SAVE_REGISTRATION_REQUEST_STEP = "save-registration-request";
    private static final String SEND_EMAIL_STEP = "send-to-email-service";


    private final OrganizationRepository organizationRepository;
    private final EmailTokenService emailTokenService;
    private final OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final MessagingService messagingService;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    private final OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper;
    private final SagaCompensationActionService sagaCompensationActionService;

    public RegistrationServiceImpl(OrganizationRepository organizationRepository,
                                   EmailTokenService emailTokenService,
                                   OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository,
                                   AuthServerGrpcClient authServerGrpcClient,
                                   MessagingService messagingService,
                                   UserProfileRepository userProfileRepository,
                                   UserProfileMapper userProfileMapper,
                                   OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                                   OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper,
                                   SagaCompensationActionService sagaCompensationActionService) {
        this.organizationRepository = organizationRepository;
        this.emailTokenService = emailTokenService;
        this.organizationUserRegistrationRequestRepository = organizationUserRegistrationRequestRepository;
        this.authServerGrpcClient = authServerGrpcClient;
        this.messagingService = messagingService;
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.organizationRegistrationProcessRepository = organizationRegistrationProcessRepository;
        this.organizationRegistrationProcessMapper = organizationRegistrationProcessMapper;
        this.sagaCompensationActionService = sagaCompensationActionService;
    }

    //removed @Transactional from here because the exception was thrown at the end so saga did not pick it
    @Override
    public RegisteredUserResponseDto registerNewSimpleUser(CreateNewSimpleUserRequestDto userRequestDto) {

        String email = MobilityUtils.normalizeEmail(userRequestDto.email());

        LOGGER.info("Registration process for simple user : {}", email);

        if (userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(email, userRequestDto.mobileNumber())) {
            return new RegisteredUserResponseDto(email, userRequestDto.mobileNumber());
        }

        var context = new UserProfileRegistrationContext();
        var sagaOrchestrator = new SagaOrchestrator();

        String userEmail = email != null ? email : "unknown";
        //simulateLag();

        sagaOrchestrator
                .step(CREATE_AUTH_USER_STEP,
                        () -> {
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
                .step(SAVE_USER_PROFILE_STEP,
                        () -> {
                            var newUser = userProfileMapper.createSimpleUserDtoToUserProfile(context.getAuthUserId(), userRequestDto);
                            context.setSavedProfile(userProfileRepository.save(newUser));
                        },
                        () -> userProfileRepository.delete(context.getSavedProfile())

                )
                .step(SEND_EMAIL_STEP,
                        () -> {
                            // 3) Send email
                            var token = emailTokenService.createSimpleUserToken(context.getSavedProfile());
                            var link = String.format("%s/simple?token=%s", baseUrl(), token);
                            var confirmationEmail = new EmailDto(List.of(userEmail), SUBJECT_REG_CONFIRM, link);
                            messagingService.sendToEmailService(confirmationEmail);
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

        String email = MobilityUtils.normalizeEmail(userRequestDto.email());

        LOGGER.info("Registration process for user : {} working for organization : {}", email, userRequestDto.organizationRegNumber());

        if (userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(email, userRequestDto.mobileNumber())) {
            // returns the dto normally so that no email can be guessed
            return new RegisteredUserResponseDto(email, userRequestDto.mobileNumber());
        }

        Organization organization;
        try {
            organization = findByRegistrationNumber(userRequestDto.organizationRegNumber());
        } catch (Exception e) {
            // returns the dto normally so that no organization registration number can be guessed
            return new RegisteredUserResponseDto(email, userRequestDto.mobileNumber());
        }

        var context = new UserProfileRegistrationContext();
        var sagaOrchestrator = new SagaOrchestrator();

        String userEmail = email != null ? email : "unknown";

        sagaOrchestrator
                .step(CREATE_AUTH_USER_STEP,
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
                .step(SAVE_USER_PROFILE_STEP,
                        () -> {
                            // 2) Save user profile
                            var newUser = userProfileMapper.createOrganizationUserDtoToUserProfile(context.getAuthUserId(),
                                    userRequestDto,
                                    organization
                            );
                            context.setSavedProfile(userProfileRepository.save(newUser));
                        },
                        () -> userProfileRepository.delete(context.getSavedProfile())

                )
                .step(SAVE_REGISTRATION_REQUEST_STEP,
                        () -> {
                            OrganizationUserRegistrationRequest registrationRequest = new OrganizationUserRegistrationRequest();
                            registrationRequest.setOrganizationRegistrationNumber(organization.getRegistrationNumber());
                            registrationRequest.setOrgUserEmail(userEmail);

                            var savedRegistrationRequest = organizationUserRegistrationRequestRepository
                                    .save(registrationRequest);

                            context.setRegistrationRequest(savedRegistrationRequest);
                        },
                        () -> organizationUserRegistrationRequestRepository.delete(context.getRegistrationRequest())

                )
                .step(SEND_EMAIL_STEP,
                        () -> {
                            var emailToken = emailTokenService.createOrganizationUserToken(
                                    context.getSavedProfile().getOrganization(),
                                    context.getSavedProfile().getId(),
                                    context.getSavedProfile().getEmail(),
                                    AccountConfirmedBy.USER
                            );
                            var link = String.format("%s/confirmation/org-user?token=%s", baseUrl(), emailToken);
                            var confirmationEmail = new EmailDto(List.of(userEmail), SUBJECT_REG_CONFIRM, link);
                            messagingService.sendToEmailService(confirmationEmail);
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

        if (organizationRepository.existsByRegistrationNumberIgnoreCase(newOrganizationRequestDto.registrationNumber())) {
            // returns the dto normally so that no organization registration number can be guessed
            return response;
        }
        OrganizationRegistrationProcess orgRegistrationProcess = organizationRegistrationProcessMapper
                .requestDtoToEntity(newOrganizationRequestDto);

        organizationRegistrationProcessRepository.save(orgRegistrationProcess);
        // organization registration request successful
        return response;
    }

    @Override
    public void resendEmailVerificationToken(String emailRequest) {
        String email = MobilityUtils.normalizeEmail(emailRequest);
        LOGGER.info("Resend email verification token for email : {}", email);
        UserProfile user = userProfileRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        var refreshToken = emailTokenService.refreshEmailVerificationToken(user.getId());
        messagingService.sendToEmailService(getEmailData(refreshToken, user));
    }


    private EmailDto getEmailData(RefreshTokenDataModel refreshToken, UserProfile user) {
        var confirmedByOptional = refreshToken.confirmedBy();
        if (user.getOrganization() == null || user.getOrganization().getRegistrationNumber() == null
                || confirmedByOptional.isEmpty()) {
            return emailForUser(refreshToken, user, "simple");
        }

        AccountConfirmedBy confirmedBy = AccountConfirmedBy
                .getAccountConfirmedByFromString(confirmedByOptional.get());

        return switch (confirmedBy) {
            case USER -> emailForUser(refreshToken, user, "confirmation/org-user");
            case ORGANIZATION -> emailForOrgAdmins(refreshToken, user);
        };
    }

    private EmailDto emailForUser(RefreshTokenDataModel refreshToken, UserProfile user, String path) {
        return new EmailDto(List.of(user.getEmail()), SUBJECT_REG_CONFIRM, buildLink(path, refreshToken.token()));
    }

    private EmailDto emailForOrgAdmins(RefreshTokenDataModel refreshToken, UserProfile user) {
        var adminInfos = authServerGrpcClient
                .getOrganizationAdminInfoFromAuthServer(user.getOrganization().getRegistrationNumber());

        List<String> adminEmails = adminInfos.stream()
                .map(OrgAdminInfoResponseDto::email)
                .distinct()
                .toList();

        var link = buildLink("confirmation/org-user/admin", refreshToken.token());

        return new EmailDto(adminEmails, SUBJECT_REG_CONFIRM, link);
    }

    private String buildLink(String path, String token) {
        return String.format("%s/%s?token=%s", baseUrl(), path, token);
    }

    private Organization findByRegistrationNumber(String registrationNumber) {
        return organizationRepository.findByRegistrationNumberIgnoreCase(registrationNumber)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private String baseUrl() {//todo remove it
        return "http://localhost/account-management/confirmation";
    }

    //testing slow service
    private void simulateLag() {
        try {
            Thread.sleep(3100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
