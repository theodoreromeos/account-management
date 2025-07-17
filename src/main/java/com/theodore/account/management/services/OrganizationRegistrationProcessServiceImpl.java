package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.entities.specifications.OrganizationRegistrationProcessSpecification;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.models.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.models.RegistrationProcessResponseDto;
import com.theodore.account.management.models.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.SearchResponse;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationRegistrationProcessServiceImpl implements OrganizationRegistrationProcessService {

    private final static Logger LOGGER = LoggerFactory.getLogger(OrganizationRegistrationProcessServiceImpl.class);

    private final OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    private final OrganizationService organizationService;
    private final UserProfileService userProfileService;
    private final AuthServerGrpcClient authServerGrpcClient;

    public OrganizationRegistrationProcessServiceImpl(OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository,
                                                      OrganizationService organizationService,
                                                      UserProfileService userProfileService,
                                                      AuthServerGrpcClient authServerGrpcClient) {
        this.organizationRegistrationProcessRepository = organizationRegistrationProcessRepository;
        this.organizationService = organizationService;
        this.userProfileService = userProfileService;
        this.authServerGrpcClient = authServerGrpcClient;
    }

    @Override
    public void saveOrganizationRegistrationProcess(OrganizationRegistrationProcess orgRegistrationProcess) {
        organizationRegistrationProcessRepository.save(orgRegistrationProcess);
    }

    @Override
    public SearchResponse<RegistrationProcessResponseDto> searchOrganizationRegistrationProcess(SearchRegistrationProcessRequestDto searchRequest,
                                                                                                int page,
                                                                                                int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("dateCreated").descending());
        Page<OrganizationRegistrationProcess> filteredResults = organizationRegistrationProcessRepository.findAll(Specification
                .where(OrganizationRegistrationProcessSpecification.filterCriteria(searchRequest)), pageable);//todo : change the deprecated

        List<RegistrationProcessResponseDto> results = filteredResults.stream()
                .map(r -> {
                    RegistrationProcessResponseDto dto = new RegistrationProcessResponseDto();
                    dto.setId(r.getId());
                    dto.setOrganizationName(r.getOrganizationName());
                    dto.setRegistrationNumber(r.getRegistrationNumber());
                    dto.setAdminApproved(r.getAdminApprovedStatus());
                    dto.setCountry(r.getCountry());
                    dto.setOrgAdminName(r.getOrgAdminName());
                    dto.setOrgAdminSurname(r.getOrgAdminSurname());
                    dto.setOrgAdminEmail(r.getOrgAdminEmail());
                    dto.setOrgAdminPhone(r.getOrgAdminPhone());
                    return dto;
                }).toList(); //todo make a mapper


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

    //todo with mapper
    private void saveUserProfile(OrganizationRegistrationProcess registrationProcess, Organization organization, String userAuthId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setName(registrationProcess.getOrgAdminName());
        userProfile.setSurname(registrationProcess.getOrgAdminSurname());
        userProfile.setEmail(registrationProcess.getOrgAdminEmail());
        userProfile.setMobileNumber(registrationProcess.getOrgAdminPhone());
        userProfile.setOrganization(organization);
        userProfile.setId(userAuthId);
        userProfileService.saveUserProfile(userProfile);
    }

    //todo with mapper
    private Organization saveOrganization(OrganizationRegistrationProcess registrationProcess) {
        Organization organization = new Organization();
        organization.setRegistrationNumber(registrationProcess.getRegistrationNumber());
        organization.setCountry(registrationProcess.getCountry());
        organization.setName(registrationProcess.getOrganizationName());

        return organizationService.saveOrganization(organization);
    }


}
