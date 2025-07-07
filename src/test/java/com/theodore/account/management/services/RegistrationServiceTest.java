package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.queue.common.authserver.CredentialsRollbackEventDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private RegistrationService registrationService;

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private EmailTokenService emailTokenService;
    @Mock
    private OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    @Mock
    private AuthServerClient authServerClient;
    @Mock
    private UserManagementEmailMessagingService userManagementEmailMessagingService;
    @Mock
    private UserProfileService userProfileService;

    @Spy
    private UserProfileMapper userProfileMapper;

    @BeforeEach
    public void setup() {
        registrationService = new RegistrationServiceImpl(organizationRepository,
                emailTokenService,
                organizationUserRegistrationRequestRepository,
                authServerClient,
                userManagementEmailMessagingService,
                userProfileService,
                userProfileMapper);
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
            assertEquals(USER_EMAIL, response.getEmail());
            assertEquals(USER_PHONE, response.getPhoneNumber());
            verify(userProfileService, times(1)).userProfileExistsByEmailAndMobileNumber(any(), any());
            verifyNoInteractions(authServerClient, emailTokenService, userManagementEmailMessagingService);
        }

        @DisplayName("registerNewSimpleUser - User is registered successfully (positive scenario)")
        @Test
        void givenCorrectUserData_whenRegisteringNewSimpleUser_thenReturnDto() {
            // given
            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_PASSWORD, USER_NAME, USER_SURNAME);

            var authUser = new AuthUserCreatedResponseDto(USER_ID);
            var savedProfile = new UserProfile(USER_ID, USER_EMAIL, USER_PHONE);
            savedProfile.setName(USER_NAME);
            savedProfile.setSurname(USER_SURNAME);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(authServerClient.authServerNewSimpleUserRegistration(any())).thenReturn(authUser);
            when(userProfileService.saveUserProfile(any())).thenReturn(savedProfile);
            when(emailTokenService.createToken(eq(savedProfile), any())).thenReturn(TOKEN);

            // when
            var response = registrationService.registerNewSimpleUser(dto);

            // then
            assertEquals(USER_EMAIL, response.getEmail());
            assertEquals(USER_PHONE, response.getPhoneNumber());
            verify(authServerClient, times(1)).authServerNewSimpleUserRegistration(any());
            verify(userProfileService, times(1)).saveUserProfile(any());
            verify(emailTokenService, times(1)).createToken(savedProfile, RegistrationEmailPurpose.PERSONAL.toString());
            verify(userManagementEmailMessagingService, times(1)).sendToEmailService(any());
        }

        @Test
        void givenCorrectUserData_whenRegisteringNewSimpleUser_thenTriggerCompensationIfSavingProfileFails() {
            // given
            var dto = new CreateNewSimpleUserRequestDto(USER_EMAIL, USER_PHONE, USER_PASSWORD, USER_NAME, USER_SURNAME);

            var authUser = new AuthUserCreatedResponseDto(USER_ID);

            when(userProfileService.userProfileExistsByEmailAndMobileNumber(USER_EMAIL, USER_PHONE)).thenReturn(false);
            when(authServerClient.authServerNewSimpleUserRegistration(any())).thenReturn(authUser);
            when(userProfileService.saveUserProfile(any())).thenThrow(new RuntimeException("I did my best but it was not enough i guess"));

            // when
            assertThrows(RuntimeException.class, () -> registrationService.registerNewSimpleUser(dto));

            // then
            ArgumentCaptor<CredentialsRollbackEventDto> rollbackCaptor = ArgumentCaptor.forClass(CredentialsRollbackEventDto.class);
            verify(userManagementEmailMessagingService).rollbackCredentialsSave(rollbackCaptor.capture());

            assertEquals(USER_ID, rollbackCaptor.getValue().userId());
        }
    }

    @Nested
    class RegisterNewOrganizationUser {
        //todo
    }

}
