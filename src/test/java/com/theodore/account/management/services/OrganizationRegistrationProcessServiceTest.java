package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.OrganizationRegistrationProcess;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.OrganizationRegistrationDecision;
import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.mappers.OrganizationMapper;
import com.theodore.account.management.mappers.OrganizationRegistrationProcessMapper;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.dto.requests.OrganizationRegistrationDecisionRequestDto;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.racingmodel.enums.Country;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.account.management.models.dto.responses.AuthUserCreatedResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationRegistrationProcessServiceTest {

    private static final String USER_ID = "test-user-id";
    private static final String USER_EMAIL = "test@theodoreorg.com";
    private static final String USER_PHONE = "123456";
    private static final String USER_NAME = "test_name";
    private static final String USER_SURNAME = "test_surname";
    private static final AuthUserCreatedResponseDto AUTH_USER = new AuthUserCreatedResponseDto(USER_ID);
    private static final String ORG_REG_NUMBER = "test-registration-number";
    private static final String ORG_NAME = "test-organization-name";
    private static final Country ORG_COUNTRY = Country.GRC;

    @InjectMocks
    private OrganizationRegistrationProcessServiceImpl organizationRegistrationProcessService;

    @Mock
    private OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private AuthServerGrpcClient authServerGrpcClient;
    @Mock
    private SagaCompensationActionService sagaCompensationActionService;

    @Spy
    private OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper = Mappers.getMapper(OrganizationRegistrationProcessMapper.class);
    @Spy
    private UserProfileMapper userProfileMapper = Mappers.getMapper(UserProfileMapper.class);
    @Spy
    private OrganizationMapper organizationMapper = Mappers.getMapper(OrganizationMapper.class);

    @Nested
    class OrganizationRegistrationDecisionTest {

        private static final OrganizationRegistrationDecision REJECTED = OrganizationRegistrationDecision.REJECTED;
        private static final OrganizationRegistrationDecision APPROVED = OrganizationRegistrationDecision.APPROVED;
        private static final Long ID = 1L;

        @DisplayName("organizationRegistrationDecision: organization registration process not found (negative scenario)")
        @Test
        void givenNonExistentRegistrationProcessId_whenDecidingOnOrganizationRegistration_thenThrowNotFoundException() {
            // given
            when(organizationRegistrationProcessRepository.findById(ID))
                    .thenReturn(Optional.empty());

            var dto = new OrganizationRegistrationDecisionRequestDto(ID, APPROVED);

            // when
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                organizationRegistrationProcessService.organizationRegistrationDecision(dto);
            });

            // then
            assertThat(exception.getMessage()).contains("OrganizationRegistrationProcess not found");
            verify(organizationRegistrationProcessRepository, times(1)).findById(any());
            verifyNoMoreInteractions(organizationRegistrationProcessRepository);
        }

        @DisplayName("organizationRegistrationDecision: registration failed and all rollbacks are triggered (negative scenario)")
        @Test
        void givenFailureDuringUserProfileSave_whenDecidingOnOrganizationRegistration_thenTriggerAllRollbacks() {
            // given
            var dto = new OrganizationRegistrationDecisionRequestDto(ID, APPROVED);

            OrganizationRegistrationProcess registrationProcess = createNewOrganizationRegistrationProcess();

            Organization org = createNewOrganization(registrationProcess);

            when(organizationRegistrationProcessRepository.findById(ID))
                    .thenReturn(Optional.of(registrationProcess));

            when(organizationRegistrationProcessRepository.save(registrationProcess))
                    .thenReturn(registrationProcess);

            when(organizationService.saveOrganization(any())).thenReturn(org);

            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any())).thenReturn(AUTH_USER);

            when(userProfileService.saveUserProfile(any())).thenThrow(new RuntimeException("I did my best but it was not enough i guess"));

            // when
            assertThatThrownBy(() -> organizationRegistrationProcessService.organizationRegistrationDecision(dto))
                    .isInstanceOf(RuntimeException.class);

            // then
            verify(organizationRegistrationProcessRepository, times(1)).save(any());
            verify(organizationService, times(1)).saveOrganization(any());
            verify(authServerGrpcClient, times(1)).authServerNewOrganizationUserRegistration(any(), any());
            verify(userProfileService, times(1)).saveUserProfile(any());
            verify(sagaCompensationActionService, times(1)).authServerCredentialsRollback(any(), any(), any());
            verify(organizationService, times(1)).deleteOrganization(any());
            verify(organizationRegistrationProcessRepository, times(1)).delete(any(OrganizationRegistrationProcess.class));

            ArgumentCaptor<String> authUserIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            verify(sagaCompensationActionService).authServerCredentialsRollback(authUserIdCaptor.capture(), emailCaptor.capture(), any());

            assertThat(authUserIdCaptor.getValue()).isEqualTo(USER_ID);
            assertThat(emailCaptor.getValue()).isEqualTo(USER_EMAIL);
        }

        @DisplayName("organizationRegistrationDecision: registration failed and rollback only to organizationRegistrationProcess is triggered (negative scenario)")
        @Test
        void givenAuthServerError_whenDecidingOnOrganizationRegistration_thenRollbackOrganizationRegistrationProcessOnly() {
            // given
            var dto = new OrganizationRegistrationDecisionRequestDto(ID, APPROVED);

            OrganizationRegistrationProcess registrationProcess = createNewOrganizationRegistrationProcess();

            Organization org = createNewOrganization(registrationProcess);

            when(organizationRegistrationProcessRepository.findById(ID))
                    .thenReturn(Optional.of(registrationProcess));

            when(organizationRegistrationProcessRepository.save(registrationProcess))
                    .thenReturn(registrationProcess);

            when(organizationService.saveOrganization(any())).thenReturn(org);

            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any()))
                    .thenThrow(new RuntimeException("auth server down"));

            // when
            assertThatThrownBy(() -> organizationRegistrationProcessService.organizationRegistrationDecision(dto))
                    .isInstanceOf(RuntimeException.class);

            // then
            verify(organizationRegistrationProcessRepository, times(1)).save(any());
            verify(organizationService, times(1)).saveOrganization(any());
            verify(authServerGrpcClient, times(1)).authServerNewOrganizationUserRegistration(any(), any());
            verify(organizationService, times(1)).deleteOrganization(any());
            verify(organizationRegistrationProcessRepository, times(1))
                    .delete(any(OrganizationRegistrationProcess.class));
            verifyNoInteractions(userProfileService);
            verifyNoInteractions(sagaCompensationActionService);
        }

        @DisplayName("organizationRegistrationDecision: registration rejected (positive scenario)")
        @Test
        void givenRejectionDecision_whenDecidingOnOrganizationRegistration_thenRejectOrganizationRegistration() {
            // given
            var dto = new OrganizationRegistrationDecisionRequestDto(ID, REJECTED);

            OrganizationRegistrationProcess registrationProcess = createNewOrganizationRegistrationProcess();

            when(organizationRegistrationProcessRepository.findById(ID))
                    .thenReturn(Optional.of(registrationProcess));

            // when
            organizationRegistrationProcessService.organizationRegistrationDecision(dto);

            // then
            verify(organizationRegistrationProcessRepository, times(1)).save(any());
            verifyNoInteractions(sagaCompensationActionService);
            verifyNoInteractions(userProfileService);
        }

        @DisplayName("organizationRegistrationDecision: registration approved  (positive scenario)")
        @Test
        void givenApprovalDecision_whenDecidingOnOrganizationRegistration_thenApproveOrganizationRegistration() {
            // given
            var dto = new OrganizationRegistrationDecisionRequestDto(ID, APPROVED);

            OrganizationRegistrationProcess registrationProcess = createNewOrganizationRegistrationProcess();

            Organization org = createNewOrganization(registrationProcess);

            UserProfile newUser = createNewUserProfile(registrationProcess, org);

            when(organizationRegistrationProcessRepository.findById(ID))
                    .thenReturn(Optional.of(registrationProcess));

            when(organizationRegistrationProcessRepository.save(registrationProcess))
                    .thenReturn(registrationProcess);

            when(organizationService.saveOrganization(any())).thenReturn(org);

            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any())).thenReturn(AUTH_USER);

            when(userProfileService.saveUserProfile(any())).thenReturn(newUser);

            // when
            organizationRegistrationProcessService.organizationRegistrationDecision(dto);

            // then
            verify(organizationRegistrationProcessRepository, times(1)).save(any());
            verify(organizationService, times(1)).saveOrganization(any());
            verify(authServerGrpcClient, times(1)).authServerNewOrganizationUserRegistration(any(), any());
            verify(userProfileService, times(1)).saveUserProfile(any());
            verifyNoInteractions(sagaCompensationActionService);
        }


        private OrganizationRegistrationProcess createNewOrganizationRegistrationProcess() {
            OrganizationRegistrationProcess registrationProcess = new OrganizationRegistrationProcess();
            registrationProcess.setRegistrationNumber(ORG_REG_NUMBER);
            registrationProcess.setOrganizationName(ORG_NAME);
            registrationProcess.setCountry(ORG_COUNTRY);
            registrationProcess.setOrgAdminEmail(USER_EMAIL);
            registrationProcess.setOrgAdminPhone(USER_PHONE);
            registrationProcess.setOrgAdminName(USER_NAME);
            registrationProcess.setOrgAdminSurname(USER_SURNAME);
            registrationProcess.setId(ID);
            registrationProcess.setAdminApprovedStatus(OrganizationRegistrationStatus.PENDING);
            return registrationProcess;
        }

        private Organization createNewOrganization(OrganizationRegistrationProcess registrationProcess) {
            return organizationMapper.orgRegistrationProcessToOrganization(registrationProcess);
        }

        private UserProfile createNewUserProfile(OrganizationRegistrationProcess registrationProcess,
                                                 Organization organization) {
            return userProfileMapper.orgRegistrationProcessToUserProfile(registrationProcess, organization, USER_ID);
        }
    }

}
