package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.entities.specifications.OrganizationRegistrationProcessSpecification;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.mappers.OrganizationMapper;
import com.theodore.account.management.mappers.OrganizationRegistrationProcessMapper;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.dto.requests.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.models.dto.responses.SearchResponse;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.utils.SecurePasswordGenerator;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.racingmodel.models.CreateNewOrganizationAuthUserRequestDto;
import jakarta.transaction.Transactional;
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

    private final OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    private final OrganizationService organizationService;
    private final UserProfileService userProfileService;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper;
    private final UserProfileMapper userProfileMapper;
    private final OrganizationMapper organizationMapper;

    public OrganizationRegistrationProcessServiceImpl(OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                                                      OrganizationService organizationService,
                                                      UserProfileService userProfileService,
                                                      AuthServerGrpcClient authServerGrpcClient,
                                                      OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper,
                                                      UserProfileMapper userProfileMapper,
                                                      OrganizationMapper organizationMapper) {
        this.organizationRegistrationProcessRepository = organizationRegistrationProcessRepository;
        this.organizationService = organizationService;
        this.userProfileService = userProfileService;
        this.authServerGrpcClient = authServerGrpcClient;
        this.organizationRegistrationProcessMapper = organizationRegistrationProcessMapper;
        this.userProfileMapper = userProfileMapper;
        this.organizationMapper = organizationMapper;
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
                .map(organizationRegistrationProcessMapper::mapEntityToResponseDto)
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
    @Transactional
    public void organizationRegistrationDecision(OrganizationRegistrationDecisionRequestDto requestDto) {//todo: saga patern is needed

        LOGGER.info("Decision {} for organization registration process with id {}", requestDto.decision(), requestDto.id());

        OrganizationRegistrationProcess registrationProcess = organizationRegistrationProcessRepository.findById(requestDto.id())
                .orElseThrow(() -> new NotFoundException("OrganizationRegistrationProcess not found"));

        registrationProcess.setAdminApprovedStatus(OrganizationRegistrationStatus.decisionToStatus(requestDto.decision()));

        OrganizationRegistrationProcess savedRegistrationProcess = organizationRegistrationProcessRepository.save(registrationProcess);

        if (OrganizationRegistrationStatus.APPROVED.equals(savedRegistrationProcess.getAdminApprovedStatus())) {
            Organization organization = saveOrganization(savedRegistrationProcess);
            String password = SecurePasswordGenerator.generatePlaceholderPassword();

            var orgAuthUserRequest = new CreateNewOrganizationAuthUserRequestDto(savedRegistrationProcess.getOrgAdminEmail(),
                    savedRegistrationProcess.getOrgAdminPhone(),
                    password,
                    organization.getRegistrationNumber());
            //send to auth server and get id
            var authUser = authServerGrpcClient.authServerNewOrganizationUserRegistration(orgAuthUserRequest, RoleType.ORGANIZATION_ADMIN);

            saveUserProfile(savedRegistrationProcess, organization, authUser.id());

        }

    }

    private void saveUserProfile(OrganizationRegistrationProcess registrationProcess, Organization organization, String userAuthId) {
        UserProfile userProfile = userProfileMapper.orgRegistrationProcessToUserProfile(registrationProcess, organization, userAuthId);
        userProfileService.saveUserProfile(userProfile);
    }

    private Organization saveOrganization(OrganizationRegistrationProcess registrationProcess) {
        Organization organization = organizationMapper.orgRegistrationProcessToOrganization(registrationProcess);
        return organizationService.saveOrganization(organization);
    }

}
