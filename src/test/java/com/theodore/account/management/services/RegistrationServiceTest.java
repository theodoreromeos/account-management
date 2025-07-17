package com.theodore.account.management.services;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.racingmodel.models.AuthUserCreatedResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    private static final String TOKEN = "test-token";
    private static final String USER_ID = "test-user-id";
    private static final String USER_EMAIL = "test@theodoreorg.com";
    private static final String USER_PHONE = "123456";
    private static final String USER_PASSWORD = "test-password";
    private static final String USER_NAME = "test_name";
    private static final String USER_SURNAME = "test_surname";
    private AuthUserCreatedResponseDto AUTH_USER = new AuthUserCreatedResponseDto(USER_ID);

    private RegistrationService registrationService;

    @Mock
    private OrganizationService organizationService;
    @Mock
    private EmailTokenService emailTokenService;
    @Mock
    private OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;
    @Mock
    private AuthServerGrpcClient authServerGrpcClient;
    @Mock
    private MessagingService messagingService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private OrganizationRegistrationProcessService organizationRegistrationProcessService;


    @Spy
    private UserProfileMapper userProfileMapper;

    @BeforeEach
    public void setup() {
        registrationService = new RegistrationServiceImpl(organizationService,
                emailTokenService,
                organizationUserRegistrationRequestService,
                authServerGrpcClient,
                messagingService,
                userProfileService,
                userProfileMapper,
                organizationRegistrationProcessService);
    }

    @Nested
    class RegisterNewSimpleUser {

        @DisplayName("registerNewSimpleUser - User already exists (negative scenario)")
        @Test
        void givenAlreadyExistingUser_whenRegisteringNewSimpleUser_thenReturnDtoWithoutDoingAnythingElse() {
            // given
            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE))
                    .thenReturn(true);

            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD);

            // when
            var response = registrationService.registerNewSimpleUser(dto);

            // then
            assertThat(response.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(USER_PHONE);
            verify(userProfileService, times(1)).userProfileExistsByEmailAndMobileNumber(any(), any());
            verifyNoInteractions(authServerGrpcClient, emailTokenService, messagingService);
        }

        @DisplayName("registerNewSimpleUser - User is registered successfully (positive scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewSimpleUser_thenReturnDto() {
            // given
            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD);

            var savedProfile = new UserProfile(USER_ID, USER_EMAIL, USER_PHONE);
            savedProfile.setName(USER_NAME);
            savedProfile.setSurname(USER_SURNAME);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(authServerGrpcClient.authServerNewSimpleUserRegistration(any())).thenReturn(AUTH_USER);
            when(userProfileService.saveUserProfile(any())).thenReturn(savedProfile);
            when(emailTokenService.createSimpleUserToken(eq(savedProfile), any())).thenReturn(TOKEN);

            // when
            var response = registrationService.registerNewSimpleUser(dto);

            // then
            assertThat(response.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(USER_PHONE);
            verify(authServerGrpcClient, times(1)).authServerNewSimpleUserRegistration(any());
            verify(userProfileService, times(1)).saveUserProfile(any());
            verify(emailTokenService, times(1)).createSimpleUserToken(savedProfile, RegistrationEmailPurpose.PERSONAL.toString());
            verify(messagingService, times(1)).sendToEmailService(any());
        }

        @DisplayName("registerNewSimpleUser - User is not saved successfully and a compensation is triggered (negative scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewSimpleUser_thenTriggerCompensationIfSavingProfileFails() {
            // given
            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(authServerGrpcClient.authServerNewSimpleUserRegistration(any())).thenReturn(AUTH_USER);
            when(userProfileService.saveUserProfile(any())).thenThrow(new RuntimeException("I did my best but it was not enough i guess"));

            // when
            assertThatThrownBy(() -> registrationService.registerNewSimpleUser(dto))
                    .isInstanceOf(RuntimeException.class);

            // then
            ArgumentCaptor<CredentialsRollbackEventDto> rollbackCaptor = ArgumentCaptor.forClass(CredentialsRollbackEventDto.class);
            verify(messagingService).rollbackCredentialsSave(rollbackCaptor.capture());

            assertThat(rollbackCaptor.getValue().userId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    class RegisterNewOrganizationUser {

        private static final String ORG_REG_NUMBER = "test-registration-number";

        private Organization ORGANIZATION;

        @BeforeEach
        public void newOrganizationUserTestSetup() {
            ORGANIZATION = createMockOrganization();
        }

        @DisplayName("registerNewOrganizationUser - User already exists then return dto (negative scenario)")
        @Test
        void givenAlreadyExistingUser_whenRegisteringNewOrganizationUser_thenReturnDtoWithoutDoingAnythingElse() {
            // given
            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE))
                    .thenReturn(true);

            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            // when
            var result = registrationService.registerNewOrganizationUser(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(USER_PHONE);
            verify(userProfileService, times(1)).userProfileExistsByEmailAndMobileNumber(any(), any());
            verifyNoInteractions(authServerGrpcClient, emailTokenService, messagingService);
        }

        @DisplayName("registerNewOrganizationUser - User already exists then return dto (negative scenario)")
        @Test
        void givenOrganizationDoesNotExist_whenRegisteringNewOrganizationUser_thenReturnDtoWithoutDoingAnythingElse() {
            // given
            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE))
                    .thenReturn(false);
            when(organizationService.findByRegistrationNumber(ORG_REG_NUMBER))
                    .thenThrow(new NotFoundException("Organization doesn't exist"));

            // when
            var result = registrationService.registerNewOrganizationUser(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(USER_PHONE);
            verify(userProfileService, times(1)).userProfileExistsByEmailAndMobileNumber(any(), any());
            verify(organizationService, times(1)).findByRegistrationNumber(any());
            verifyNoInteractions(authServerGrpcClient, emailTokenService, messagingService);
        }

        @DisplayName("registerNewOrganizationUser - User is registered successfully (positive scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewOrganizationUser_thenReturnDto() {
            // given
            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            var savedProfile = new UserProfile(USER_ID, USER_EMAIL, USER_PHONE);
            savedProfile.setName(USER_NAME);
            savedProfile.setSurname(USER_SURNAME);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(organizationService.findByRegistrationNumber(ORG_REG_NUMBER)).thenReturn(ORGANIZATION);
            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any())).thenReturn(AUTH_USER);
            when(userProfileService.saveUserProfile(any())).thenReturn(savedProfile);
            when(emailTokenService.createOrganizationUserToken(eq(savedProfile), any())).thenReturn(TOKEN);

            // when
            var result = registrationService.registerNewOrganizationUser(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(USER_PHONE);
            verify(userProfileService, times(1)).userProfileExistsByEmailAndMobileNumber(any(), any());
            verify(organizationService, times(1)).findByRegistrationNumber(any());
            verify(authServerGrpcClient, times(1)).authServerNewOrganizationUserRegistration(any(), any());
            verify(userProfileService, times(1)).saveUserProfile(any());
            verify(emailTokenService, times(1)).createOrganizationUserToken(savedProfile, RegistrationEmailPurpose.ORGANIZATION_USER.toString());
            verify(messagingService, times(1)).sendToEmailService(any());
        }

        @DisplayName("registerNewOrganizationUser - User is not saved successfully and a compensation is triggered (negative scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewOrganizationUser_thenTriggerCompensationIfSavingProfileFails() {
            // given
            var dto = new CreateNewOrganizationUserRequestDto(USER_EMAIL, USER_PHONE, USER_NAME, USER_SURNAME, USER_PASSWORD, ORG_REG_NUMBER);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(organizationService.findByRegistrationNumber(ORG_REG_NUMBER)).thenReturn(ORGANIZATION);
            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), any())).thenReturn(AUTH_USER);
            when(userProfileService.saveUserProfile(any())).thenThrow(new RuntimeException("I did my best but it was not enough i guess"));

            // when
            assertThatThrownBy(() -> registrationService.registerNewOrganizationUser(dto)).isInstanceOf(RuntimeException.class);

            // then
            ArgumentCaptor<CredentialsRollbackEventDto> rollbackCaptor = ArgumentCaptor.forClass(CredentialsRollbackEventDto.class);
            verify(messagingService).rollbackCredentialsSave(rollbackCaptor.capture());

            assertThat(rollbackCaptor.getValue().userId()).isEqualTo(USER_ID);
        }

        private Organization createMockOrganization() {
            Organization organization = new Organization();
            organization.setRegistrationNumber(ORG_REG_NUMBER);
            organization.setId("test-org-id");
            return organization;
        }

    }

}
