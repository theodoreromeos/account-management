package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.mappers.OrganizationRegistrationProcessMapper;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationEntityRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateOrganizationAdminRequestDto;
import com.theodore.account.management.models.dto.responses.AuthUserIdResponseDto;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.infrastructure.common.entities.enums.Country;
import com.theodore.infrastructure.common.exceptions.NotFoundException;
import com.theodore.queue.common.services.MessagingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final String TOKEN = "test-token";
    private static final String USER_ID = "test-user-id";
    private static final String USER_EMAIL = "test@theodoreorg.com";
    private static final String USER_PHONE = "123456";
    private static final String USER_PASSWORD = "test-password";
    private static final String USER_NAME = "test_name";
    private static final String USER_SURNAME = "test_surname";
    private static final AuthUserIdResponseDto AUTH_USER = new AuthUserIdResponseDto(USER_ID);
    private static final String ORG_REG_NUMBER = "test-registration-number";

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private EmailTokenService emailTokenService;
    @Mock
    private OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    @Mock
    private AuthServerGrpcClient authServerGrpcClient;
    @Mock
    private MessagingService messagingService;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    @Mock
    private SagaCompensationActionService sagaCompensationActionService;

    @Spy
    private UserProfileMapper userProfileMapper;
    @Spy
    private OrganizationRegistrationProcessMapper organizationRegistrationProcessMapper;

    @Nested
    class RegisterNewSimpleUser {

        @DisplayName("registerNewSimpleUser: User already exists (negative scenario)")
        @Test
        void givenAlreadyExistingUser_whenRegisteringNewSimpleUser_thenReturnDtoWithoutDoingAnythingElse() {
            // given
            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE))
                    .thenReturn(true);

            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD);

            // when
            var response = registrationService.registerNewSimpleUser(dto);

            // then
            assertThat(response.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(USER_PHONE);

            then(userProfileRepository).should().existsByEmailAndMobileNumberAllIgnoreCase(any(), any());
            then(authServerGrpcClient).shouldHaveNoInteractions();
            then(emailTokenService).shouldHaveNoInteractions();
            then(messagingService).shouldHaveNoInteractions();
        }

        @DisplayName("registerNewSimpleUser: User is registered successfully (positive scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewSimpleUser_thenReturnDto() {
            // given
            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD);

            var savedProfile = new UserProfile(USER_ID, USER_EMAIL, USER_PHONE);
            savedProfile.setName(USER_NAME);
            savedProfile.setSurname(USER_SURNAME);

            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(authServerGrpcClient.authServerNewSimpleUserRegistration(any())).thenReturn(AUTH_USER);
            when(userProfileRepository.save(any())).thenReturn(savedProfile);
            when(emailTokenService.createSimpleUserToken(savedProfile)).thenReturn(TOKEN);

            // when
            var response = registrationService.registerNewSimpleUser(dto);

            // then
            assertThat(response.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(USER_PHONE);

            then(authServerGrpcClient).should().authServerNewSimpleUserRegistration(any());
            then(userProfileRepository).should().save(any());
            then(emailTokenService).should().createSimpleUserToken(savedProfile);
            then(messagingService).should().sendToEmailService(any());
        }

        @DisplayName("registerNewSimpleUser: User is not saved successfully and a compensation is triggered (negative scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewSimpleUser_thenTriggerCompensationIfSavingProfileFails() {
            // given
            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD);

            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(authServerGrpcClient.authServerNewSimpleUserRegistration(any())).thenReturn(AUTH_USER);
            when(userProfileRepository.save(any())).thenThrow(new RuntimeException("I did my best but it was not enough i guess"));

            // when
            assertThatThrownBy(() -> registrationService.registerNewSimpleUser(dto))
                    .isInstanceOf(RuntimeException.class);

            // then
            ArgumentCaptor<String> authUserIdCaptor = ArgumentCaptor.forClass(String.class);
            then(sagaCompensationActionService).should().authServerCredentialsRollback(authUserIdCaptor.capture(), any(), any());

            assertThat(authUserIdCaptor.getValue()).isEqualTo(USER_ID);
        }
    }

    @Nested
    class RegisterNewOrganizationUser {

        private final Organization organization = createMockOrganization();

        @DisplayName("registerNewOrganizationUser: User already exists then return dto (negative scenario)")
        @Test
        void givenAlreadyExistingUser_whenRegisteringNewOrganizationUser_thenReturnDtoWithoutDoingAnythingElse() {
            // given
            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE))
                    .thenReturn(true);

            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            // when
            var result = registrationService.registerNewOrganizationUser(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(USER_PHONE);

            then(userProfileRepository).should().existsByEmailAndMobileNumberAllIgnoreCase(any(), any());
            then(authServerGrpcClient).shouldHaveNoInteractions();
            then(emailTokenService).shouldHaveNoInteractions();
            then(messagingService).shouldHaveNoInteractions();
        }

        @DisplayName("registerNewOrganizationUser: User already exists then return dto (negative scenario)")
        @Test
        void givenOrganizationDoesNotExist_whenRegisteringNewOrganizationUser_thenReturnDtoWithoutDoingAnythingElse() {
            // given
            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE))
                    .thenReturn(false);
            when(organizationRepository.findByRegistrationNumberIgnoreCase(ORG_REG_NUMBER))
                    .thenThrow(new NotFoundException("Organization doesn't exist"));

            // when
            var result = registrationService.registerNewOrganizationUser(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(USER_PHONE);

            then(userProfileRepository).should().existsByEmailAndMobileNumberAllIgnoreCase(any(), any());
            then(organizationRepository).should().findByRegistrationNumberIgnoreCase(any());
            then(authServerGrpcClient).shouldHaveNoInteractions();
            then(emailTokenService).shouldHaveNoInteractions();
            then(messagingService).shouldHaveNoInteractions();
        }

        @DisplayName("registerNewOrganizationUser: User is registered successfully (positive scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewOrganizationUser_thenReturnDto() {
            // given
            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            var savedProfile = new UserProfile(USER_ID, USER_EMAIL, USER_PHONE);
            savedProfile.setName(USER_NAME);
            savedProfile.setSurname(USER_SURNAME);

            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(organizationRepository.findByRegistrationNumberIgnoreCase(ORG_REG_NUMBER)).thenReturn(Optional.of(organization));
            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any())).thenReturn(AUTH_USER);
            when(userProfileRepository.save(any())).thenReturn(savedProfile);
            when(emailTokenService.createOrganizationUserToken(any(), any(), any(), any())).thenReturn(TOKEN);

            // when
            var result = registrationService.registerNewOrganizationUser(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(USER_PHONE);

            then(userProfileRepository).should().existsByEmailAndMobileNumberAllIgnoreCase(any(), any());
            then(organizationRepository).should().findByRegistrationNumberIgnoreCase(any());
            then(authServerGrpcClient).should().authServerNewOrganizationUserRegistration(any(), any());
            then(userProfileRepository).should().save(any());
            then(emailTokenService).should().createOrganizationUserToken(any(), any(), any(), any());
            then(messagingService).should().sendToEmailService(any());
        }

        @DisplayName("registerNewOrganizationUser: User is not saved successfully and a compensation is triggered (negative scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewOrganizationUser_thenTriggerCompensationIfSavingProfileFails() {
            // given
            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            when(userProfileRepository.existsByEmailAndMobileNumberAllIgnoreCase(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(organizationRepository.findByRegistrationNumberIgnoreCase(ORG_REG_NUMBER)).thenReturn(Optional.of(organization));
            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any())).thenReturn(AUTH_USER);
            when(userProfileRepository.save(any())).thenThrow(new RuntimeException("I did my best but it was not enough i guess"));

            // when
            assertThatThrownBy(() -> registrationService.registerNewOrganizationUser(dto)).isInstanceOf(RuntimeException.class);

            // then
            ArgumentCaptor<String> authUserIdCaptor = ArgumentCaptor.forClass(String.class);
            then(sagaCompensationActionService).should().authServerCredentialsRollback(authUserIdCaptor.capture(), any(), any());

            assertThat(authUserIdCaptor.getValue()).isEqualTo(USER_ID);
        }

        private Organization createMockOrganization() {
            Organization org = new Organization();
            org.setRegistrationNumber(ORG_REG_NUMBER);
            org.setId("test-org-id");
            return org;
        }

    }

    @Nested
    class RegisterNewOrganizationEntity {

        private static final String ORG_NAME = "test-org-name";
        private static final Country ORG_COUNTRY = Country.GRC;
        private static final CreateNewOrganizationEntityRequestDto.OrganizationType ORG_TYPE = CreateNewOrganizationEntityRequestDto.OrganizationType.MANUFACTURER;

        @DisplayName("registerNewOrganizationEntity: Registers organization when registration number is not taken (positive scenario)")
        @Test
        void givenAvailableRegNumber_whenRegisteringNewOrganization_thenNewOrganizationRequestIsSaved() {
            // given
            var adminReqDto = new CreateOrganizationAdminRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME);
            var newOrganizationRequestDto = new CreateNewOrganizationEntityRequestDto(
                    adminReqDto, ORG_NAME, ORG_REG_NUMBER, ORG_COUNTRY, ORG_TYPE);
            when(organizationRepository.existsByRegistrationNumberIgnoreCase(ORG_REG_NUMBER)).thenReturn(false);

            // when
            var result = registrationService.registerNewOrganizationEntity(newOrganizationRequestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.organizationName()).isEqualTo(ORG_NAME);
            assertThat(result.registrationNumber()).isEqualTo(ORG_REG_NUMBER);

            then(organizationRepository).should().existsByRegistrationNumberIgnoreCase(ORG_REG_NUMBER);
            then(organizationRegistrationProcessRepository).should().save(any());
        }

        @DisplayName("registerNewOrganizationEntity: Skips registration when registration number already exists (negative scenario)")
        @Test
        void givenTakenRegNumber_whenRegisteringNewOrganization_thenNewOrganizationRequestIsNotSaved() {
            // given
            var adminReqDto = new CreateOrganizationAdminRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME);
            var newOrganizationRequestDto = new CreateNewOrganizationEntityRequestDto(
                    adminReqDto, ORG_NAME, ORG_REG_NUMBER, ORG_COUNTRY, ORG_TYPE);
            when(organizationRepository.existsByRegistrationNumberIgnoreCase(ORG_REG_NUMBER)).thenReturn(true);

            // when
            var result = registrationService.registerNewOrganizationEntity(newOrganizationRequestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.organizationName()).isEqualTo(ORG_NAME);
            assertThat(result.registrationNumber()).isEqualTo(ORG_REG_NUMBER);

            then(organizationRepository).should().existsByRegistrationNumberIgnoreCase(ORG_REG_NUMBER);
            then(organizationRegistrationProcessRepository).shouldHaveNoInteractions();
        }

    }

}
