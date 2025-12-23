package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.entities.specifications.OrganizationRegistrationProcessSpecification;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.mappers.OrganizationMapper;
import com.theodore.account.management.mappers.OrganizationRegistrationProcessMapper;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.NewOrganizationRegistrationContext;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationAuthUserRequestDto;
import com.theodore.account.management.models.dto.requests.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.account.management.utils.SecurePasswordGenerator;
import com.theodore.infrastructure.common.entities.modeltypes.RoleType;
import com.theodore.infrastructure.common.exceptions.NotFoundException;
import com.theodore.infrastructure.common.models.SearchResponse;
import com.theodore.infrastructure.common.saga.SagaOrchestrator;
import com.theodore.queue.common.emails.EmailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationRegistrationProcessServiceImpl implements OrganizationRegistrationProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationRegistrationProcessServiceImpl.class);

    private static final String CREATE_REGISTRATION_PROCESS_STEP = "create-registration-process";
    private static final String CREATE_ORGANIZATION_STEP = "create-organization";
    private static final String CREATE_ORGANIZATION_AUTH_USER_STEP = "create-organization-auth-user";
    private static final String SAVE_USER_PROFILE_STEP = "save-user-profile";
    private static final String SEND_EMAIL_STEP = "send-to-email-service";

    private final OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    private final OrganizationRepository organizationRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper;
    private final UserProfileMapper userProfileMapper;
    private final OrganizationMapper organizationMapper;
    private final SagaCompensationActionService sagaCompensationActionService;
    private final EmailTokenService emailTokenService;
    private final MessagingService messagingService;

    public OrganizationRegistrationProcessServiceImpl(OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                                                      OrganizationRepository organizationRepository,
                                                      UserProfileRepository userProfileRepository,
                                                      AuthServerGrpcClient authServerGrpcClient,
                                                      OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper,
                                                      UserProfileMapper userProfileMapper,
                                                      OrganizationMapper organizationMapper,
                                                      SagaCompensationActionService sagaCompensationActionService,
                                                      EmailTokenService emailTokenService,
                                                      MessagingService messagingService) {
        this.organizationRegistrationProcessRepository = organizationRegistrationProcessRepository;
        this.organizationRepository = organizationRepository;
        this.userProfileRepository = userProfileRepository;
        this.authServerGrpcClient = authServerGrpcClient;
        this.organizationRegistrationProcessMapper = organizationRegistrationProcessMapper;
        this.userProfileMapper = userProfileMapper;
        this.organizationMapper = organizationMapper;
        this.sagaCompensationActionService = sagaCompensationActionService;
        this.emailTokenService = emailTokenService;
        this.messagingService = messagingService;
    }

    @Override
    public void saveOrganizationRegistrationProcess(OrganizationRegistrationProcess orgRegistrationProcess) {
        organizationRegistrationProcessRepository.save(orgRegistrationProcess);
    }

    @Override
    public SearchResponse<RegistrationProcessResponseDto> searchOrganizationRegistrationProcess(SearchRegistrationProcessRequestDto searchRequest,
                                                                                                int page,
                                                                                                int pageSize) {
        LOGGER.info("Request to search for organization registration processes");
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("dateCreated").descending());
        Page<OrganizationRegistrationProcess> filteredResults = organizationRegistrationProcessRepository.findAll(
                OrganizationRegistrationProcessSpecification.filterCriteria(searchRequest), pageable);

        List<RegistrationProcessResponseDto> results = filteredResults.stream()
                .map(organizationRegistrationProcessMapper::entityToResponseDto)
                .toList();

        SearchResponse<RegistrationProcessResponseDto> response = new SearchResponse<>();

        response.setData(results);
        response.setPageNumber(filteredResults.getNumber());
        response.setPageSize(filteredResults.getSize());
        response.setTotalPages(filteredResults.getTotalPages());
        response.setTotalElements(filteredResults.getTotalElements());
        response.setFirst(filteredResults.isFirst());
        response.setLast(filteredResults.isLast());

        return response;
    }

    @Override
    public void organizationRegistrationDecision(OrganizationRegistrationDecisionRequestDto requestDto) {

        LOGGER.info("Decision {} for organization registration process with id {}", requestDto.decision(), requestDto.id());

        OrganizationRegistrationProcess registrationProcess = organizationRegistrationProcessRepository.findById(requestDto.id())
                .orElseThrow(() -> new NotFoundException("OrganizationRegistrationProcess not found"));

        OrganizationRegistrationStatus decisionStatus = OrganizationRegistrationStatus.decisionToStatus(requestDto.decision());

        registrationProcess.setAdminApprovedStatus(decisionStatus);

        if (OrganizationRegistrationStatus.REJECTED.equals(decisionStatus)) {
            organizationRegistrationProcessRepository.save(registrationProcess);
            return;
        }

        String orgAdminEmail = registrationProcess.getOrgAdminEmail() != null ? registrationProcess.getOrgAdminEmail() : "unknown";

        var context = new NewOrganizationRegistrationContext();
        var sagaOrchestrator = new SagaOrchestrator();

        sagaOrchestrator
                .step(CREATE_REGISTRATION_PROCESS_STEP,
                        () -> {
                            var savedRegistrationProcess = organizationRegistrationProcessRepository.save(registrationProcess);
                            context.setRegistrationProcess(savedRegistrationProcess);
                            context.setTempPassword(SecurePasswordGenerator.generatePlaceholderPassword());
                        },
                        () -> organizationRegistrationProcessRepository.delete(context.getRegistrationProcess())
                )
                .step(CREATE_ORGANIZATION_STEP,
                        () -> {
                            Organization organization = saveOrganization(context.getRegistrationProcess());
                            context.setOrganization(organization);
                        },
                        () -> organizationRepository.delete(context.getOrganization())
                )
                .step(CREATE_ORGANIZATION_AUTH_USER_STEP,
                        () -> {
                            LOGGER.info("CONTEXT password is :{}", context.getTempPassword());
                            var orgAuthUserRequest = new CreateNewOrganizationAuthUserRequestDto(
                                    context.getRegistrationProcess().getOrgAdminEmail(),
                                    context.getRegistrationProcess().getOrgAdminPhone(),
                                    context.getTempPassword(),
                                    context.getOrganization().getRegistrationNumber()
                            );
                            //send to auth server and get id
                            var authUser = authServerGrpcClient.authServerNewOrganizationUserRegistration(
                                    orgAuthUserRequest, RoleType.ORGANIZATION_ADMIN
                            );
                            context.setAuthUserId(authUser.id());
                        },
                        () -> {
                            if (context.getAuthUserId() != null) {
                                String logMsg = "Organization user registration";
                                sagaCompensationActionService.authServerCredentialsRollback(context.getAuthUserId(),
                                        orgAdminEmail,
                                        logMsg);
                            }
                        }
                )
                .step(SAVE_USER_PROFILE_STEP,
                        () -> {
                            var newUser = saveUserProfile(context.getRegistrationProcess(),
                                    context.getOrganization(),
                                    context.getAuthUserId()
                            );
                            context.setSavedProfile(newUser);
                        },
                        () -> {
                        }
                )
                .step(SEND_EMAIL_STEP,
                        () -> {
                            // Send to email service
                            var emailToken = emailTokenService.createOrganizationAdminToken(
                                    context.getSavedProfile().getOrganization(),
                                    context.getSavedProfile().getId(),
                                    context.getSavedProfile().getEmail()
                            );
                            String body = emailToken + " " + context.getTempPassword();
                            var confirmationEmail = new EmailDto(List.of(context.getSavedProfile().getEmail()),
                                    "Organization Admin Account Confirmation", body);
                            messagingService.sendToEmailService(confirmationEmail);
                            LOGGER.info("the password is :{}", context.getTempPassword());//todo remove it
                        },
                        () -> {
                        }
                );

        sagaOrchestrator.run();
    }

    private UserProfile saveUserProfile(OrganizationRegistrationProcess registrationProcess, Organization organization, String userAuthId) {
        UserProfile userProfile = userProfileMapper.orgRegistrationProcessToUserProfile(registrationProcess, organization, userAuthId);
        return userProfileRepository.save(userProfile);
    }

    private Organization saveOrganization(OrganizationRegistrationProcess registrationProcess) {
        Organization organization = organizationMapper.orgRegistrationProcessToOrganization(registrationProcess);
        return organizationRepository.save(organization);
    }

}
