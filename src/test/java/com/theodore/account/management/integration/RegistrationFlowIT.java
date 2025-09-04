package com.theodore.account.management.integration;

import com.theodore.account.management.entities.Organization;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationAuthUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewOrganizationUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleAuthUserRequestDto;
import com.theodore.account.management.models.dto.requests.CreateNewSimpleUserRequestDto;
import com.theodore.account.management.models.dto.responses.AuthUserIdResponseDto;
import com.theodore.account.management.models.dto.responses.RegisteredUserResponseDto;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.repositories.OrganizationRepository;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.account.management.services.*;
import com.theodore.account.management.utils.AccountManagementTestConfigs;
import com.theodore.account.management.utils.AccountManagementTestUtils;
import com.theodore.account.management.utils.TestData;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Import(AccountManagementTestConfigs.class)
class RegistrationFlowIT extends BasePostgresTest {

    private static final String PWD = "123paSsW0$rd";

    private static final String NEW_EMAIL = "SanjiVinsmoke@mobilitymail.com";
    private static final String NEW_MOBILE = "69109865";
    private static final String NEW_NAME = "Sanji";
    private static final String NEW_SURNAME = "Vinsmoke";

    private static final String AUTH_USER_ID = "test-id-123";
    private static final String TEST_TOKEN = "test-token-abc123";

    @Autowired
    UserProfileRepository userProfileRepository;
    @Autowired
    OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    @Autowired
    OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    TestDataFeeder testDataFeeder;

    @MockitoSpyBean
    UserProfileService userProfileService;
    @MockitoSpyBean
    OrganizationService organizationService;
    @MockitoSpyBean
    OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;

    @MockitoBean
    AuthServerGrpcClient authServerGrpcClient;
    @MockitoBean
    MessagingService messagingService;
    @MockitoBean
    EmailTokenService emailTokenService;
    @MockitoBean
    SagaCompensationActionService sagaCompensationActionService;

    WebTestClient client;

    @BeforeAll
    void initClient() {
        client = WebTestClient.bindToServer().baseUrl(baseUrl()).build();
        reset(authServerGrpcClient, messagingService, emailTokenService, sagaCompensationActionService);
    }

    @BeforeEach
    void feedUserProfile() {
        testDataFeeder.feedUserProfileTable();
    }

    @Nested
    class RegisterNewSimpleUserTests {

        @BeforeEach
        void simpleUserRegistrationSetup() {
            when(emailTokenService.createSimpleUserToken(any(UserProfile.class)))
                    .thenReturn(TEST_TOKEN);
        }

        @AfterEach
        void cleanUp() {
            testDataFeeder.cleanUserProfileTable();
        }

        @Test
        @DisplayName("registerNewSimpleUser: given null email validation fails and a bad request is returned (negative scenario)")
        void givenEmptyEmail_whenRegisteringNewSimpleUser_thenThrowBadRequest() {
            // given
            var request = createCreateNewSimpleUserRequestDto(null, NEW_MOBILE, NEW_NAME, NEW_SURNAME);

            // when
            client.post()
                    .uri("/register/user/simple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();

            // then
            verifyNoInteractions(authServerGrpcClient);
            verifyNoInteractions(emailTokenService);
            verifyNoInteractions(messagingService);
        }

        @Test
        @DisplayName("registerNewSimpleUser: Registration fails when user already exists (negative scenario)")
        void givenExistingUserData_whenRegisteringNewSimpleUser_thenExistingUserIsReturnedAndNoDuplicateCreated() {

            var request = createCreateNewSimpleUserRequestDto(TestData.EXISTING_EMAIL, TestData.EXISTING_MOBILE,
                    TestData.EXISTING_NAME, TestData.EXISTING_SURNAME);

            client.post().uri("/register/user/simple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo(TestData.EXISTING_EMAIL)
                    .jsonPath("$.phoneNumber").isEqualTo(TestData.EXISTING_MOBILE);

            long count = AccountManagementTestUtils.countProfilesByEmailAndMobile(userProfileRepository,
                    TestData.EXISTING_EMAIL, TestData.EXISTING_MOBILE);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("registerNewSimpleUser: New simple user is registered successfully (positive scenario)")
        void givenValidUserData_whenRegisteringNewSimpleUser_thenUserIsSavedAndDtoReturned() {
            // given
            var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);

            when(authServerGrpcClient.authServerNewSimpleUserRegistration(any(CreateNewSimpleAuthUserRequestDto.class)))
                    .thenReturn(authUserResponse);

            doNothing().when(messagingService).sendToEmailService(any(EmailDto.class));

            long initialCount = userProfileRepository.count();

            var request = createCreateNewSimpleUserRequestDto(NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME);

            // when
            var response = client.post()
                    .uri("/register/user/simple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(RegisteredUserResponseDto.class)
                    .returnResult()
                    .getResponseBody();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(NEW_EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(NEW_MOBILE);

            long finalCount = userProfileRepository.count();
            assertThat(finalCount).isEqualTo(initialCount + 1);

            var savedProfile = userProfileRepository.findByEmail(NEW_EMAIL);
            assertThat(savedProfile).isPresent();
            assertThat(savedProfile.get().getEmail()).isEqualTo(NEW_EMAIL);
            assertThat(savedProfile.get().getMobileNumber()).isEqualTo(NEW_MOBILE);
            assertThat(savedProfile.get().getName()).isEqualTo(NEW_NAME);
            assertThat(savedProfile.get().getSurname()).isEqualTo(NEW_SURNAME);
            assertThat(savedProfile.get().getId()).isEqualTo(AUTH_USER_ID);

            verify(authServerGrpcClient, times(1))
                    .authServerNewSimpleUserRegistration(argThat(dto ->
                            dto.email().equals(NEW_EMAIL) &&
                                    dto.mobileNumber().equals(NEW_MOBILE) &&
                                    dto.password().equals(PWD)
                    ));

            verify(emailTokenService, times(1))
                    .createSimpleUserToken(argThat(profile ->
                            profile.getEmail().equals(NEW_EMAIL) &&
                                    profile.getMobileNumber().equals(NEW_MOBILE)
                    ));

            verify(messagingService, times(1)).sendToEmailService(any());
            verifyNoInteractions(sagaCompensationActionService);
        }

        @Nested
        class SimpleUserSagaStepFailureTests {

            @Test
            @DisplayName("registerNewSimpleUser - Saga step 1 fail: Auth server registration fails - No compensation needed")
            void whenAuthServerRegistrationFails_thenNoCompensationTriggered() {
                // given
                var request = createCreateNewSimpleUserRequestDto(NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME);

                when(authServerGrpcClient.authServerNewSimpleUserRegistration(any(CreateNewSimpleAuthUserRequestDto.class)))
                        .thenThrow(new RuntimeException("Auth server unavailable"));

                long initialCount = userProfileRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalCount = userProfileRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalCount).isEqualTo(initialCount);

                verify(authServerGrpcClient, times(1))
                        .authServerNewSimpleUserRegistration(argThat(dto ->
                                dto.email().equals(NEW_EMAIL) &&
                                        dto.mobileNumber().equals(NEW_MOBILE) &&
                                        dto.password().equals(PWD)
                        ));

                verify(userProfileService, never()).saveUserProfile(any());
                verify(emailTokenService, never()).createSimpleUserToken(any());
                verify(messagingService, never()).sendToEmailService(any());

                verifyNoInteractions(sagaCompensationActionService);
            }

            @Test
            @DisplayName("registerNewSimpleUser - Saga step 2 fail: User profile save fails - Auth server rollback triggered")
            void whenUserProfileSaveFails_thenAuthServerRollbackTriggered() {
                // given
                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                var request = createCreateNewSimpleUserRequestDto(NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME);

                when(authServerGrpcClient.authServerNewSimpleUserRegistration(any(CreateNewSimpleAuthUserRequestDto.class)))
                        .thenReturn(authUserResponse);

                // EXCEPTION WHEN SAVING USER PROFILE
                doThrow(new DataIntegrityViolationException("DB IS DED"))
                        .when(userProfileService).saveUserProfile(any(UserProfile.class));

                long initialCount = userProfileRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalCount = userProfileRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalCount).isEqualTo(initialCount);

                verify(authServerGrpcClient, times(1))
                        .authServerNewSimpleUserRegistration(any());
                verify(userProfileService, times(1))
                        .saveUserProfile(any());
                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Simple user registration");
                verify(emailTokenService, never()).createSimpleUserToken(any());
                verify(messagingService, never()).sendToEmailService(any());
            }

            @Test
            @DisplayName("registerNewSimpleUser - Saga step 3 fail: Email token generation fails - Full rollback triggered")
            void whenEmailTokenGenerationFails_thenFullRollbackTriggered() {
                // given
                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                var request = createCreateNewSimpleUserRequestDto(NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME);

                when(authServerGrpcClient.authServerNewSimpleUserRegistration(any(CreateNewSimpleAuthUserRequestDto.class)))
                        .thenReturn(authUserResponse);
                when(emailTokenService.createSimpleUserToken(any(UserProfile.class)))
                        .thenThrow(new RuntimeException("Token generation failed"));

                long initialCount = userProfileRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalCount = userProfileRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalCount).isEqualTo(initialCount);

                verify(authServerGrpcClient, times(1))
                        .authServerNewSimpleUserRegistration(any());
                verify(userProfileService, times(1))
                        .saveUserProfile(any());
                verify(emailTokenService, times(1))
                        .createSimpleUserToken(any());
                verify(messagingService, never()).sendToEmailService(any());

                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Simple user registration");
                verify(userProfileService, times(1)).deleteUserProfile(any());
            }

            @Test
            @DisplayName("registerNewSimpleUser - Saga step 4 fail: Message service fails - Full rollback triggered")
            void whenMessageServiceFails_thenFullRollbackTriggered() {
                // given
                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                var request = createCreateNewSimpleUserRequestDto(NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME);

                when(authServerGrpcClient.authServerNewSimpleUserRegistration(any(CreateNewSimpleAuthUserRequestDto.class)))
                        .thenReturn(authUserResponse);

                when(emailTokenService.createSimpleUserToken(any(UserProfile.class)))
                        .thenReturn(TEST_TOKEN);

                // Failure point 4: Message service fails
                doThrow(new RuntimeException("Message broker unavailable"))
                        .when(messagingService).sendToEmailService(any(EmailDto.class));

                long initialCount = userProfileRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalCount = userProfileRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalCount).isEqualTo(initialCount);

                verify(authServerGrpcClient, times(1)).authServerNewSimpleUserRegistration(any());
                verify(emailTokenService, times(1)).createSimpleUserToken(any());
                verify(messagingService, times(1)).sendToEmailService(any());

                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Simple user registration");
                verify(userProfileService, times(1)).deleteUserProfile(any());
            }
        }

        private CreateNewSimpleUserRequestDto createCreateNewSimpleUserRequestDto(String email,
                                                                                  String mobile,
                                                                                  String name,
                                                                                  String surname) {
            return new CreateNewSimpleUserRequestDto(email, mobile, name, surname, PWD);
        }

    }

    @Nested
    class RegisterNewOrganizationUserTests {

        private static final String NON_EXISTENT_ORG_NUMBER = "ORG-999999";

        @BeforeEach
        void orgUserRegistrationSetup() {
            when(emailTokenService
                    .createOrganizationUserToken(any(Organization.class), anyString(), anyString(), any()))
                    .thenReturn(TEST_TOKEN);
            testDataFeeder.feedOrganizationTable();
        }

        @AfterEach
        void cleanUp() {
            testDataFeeder.cleanUserProfileTable();
            testDataFeeder.cleanOrganizationTable();
        }

        @Test
        @DisplayName("registerNewOrganizationUser: given null email validation fails and a bad request is returned (negative scenario)")
        void givenNullEmail_whenRegisteringNewOrgUser_thenBadRequest() {
            // given
            var request = createCreateNewOrganizationUserRequestDto(null, NEW_MOBILE, NEW_NAME, NEW_SURNAME, TestData.ORG_REG_NUMBER);

            // when
            client.post()
                    .uri("/register/user/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();

            // then
            verifyNoInteractions(organizationService);
            verifyNoInteractions(authServerGrpcClient);
            verifyNoInteractions(organizationUserRegistrationRequestService);
            verifyNoInteractions(emailTokenService);
            verifyNoInteractions(messagingService);
        }

        @Test
        @DisplayName("registerNewOrganizationUser: Registration short-circuits when user already exists (negative scenario)")
        void givenExistingUserData_whenRegisteringNewOrgUser_thenExistingUserIsReturnedAndNoDuplicateCreated() {
            // given
            var request = createCreateNewOrganizationUserRequestDto(
                    TestData.EXISTING_EMAIL, TestData.EXISTING_MOBILE, TestData.EXISTING_NAME, TestData.EXISTING_SURNAME,
                    TestData.ORG_REG_NUMBER
            );

            // when
            client.post()
                    .uri("/register/user/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo(TestData.EXISTING_EMAIL)
                    .jsonPath("$.phoneNumber").isEqualTo(TestData.EXISTING_MOBILE);

            // then
            verifyNoInteractions(organizationService);
            verifyNoInteractions(authServerGrpcClient);
            verifyNoInteractions(organizationUserRegistrationRequestService);
            verifyNoInteractions(emailTokenService);
            verifyNoInteractions(messagingService);
        }

        @Test
        @DisplayName("registerNewOrganizationUser: Unknown organization returns generic response (negative scenario)")
        void givenUnknownOrganization_whenRegisteringNewOrgUser_thenReturnGenericDtoAndNoSideEffects() {
            // given
            var request = createCreateNewOrganizationUserRequestDto(
                    NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME, NON_EXISTENT_ORG_NUMBER
            );

            // when
            client.post()
                    .uri("/register/user/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo(NEW_EMAIL)
                    .jsonPath("$.phoneNumber").isEqualTo(NEW_MOBILE);

            // then
            verify(userProfileService, times(1)).userProfileExistsByEmailAndMobileNumber(NEW_EMAIL, NEW_MOBILE);
            verify(organizationService, times(1)).findByRegistrationNumber(NON_EXISTENT_ORG_NUMBER);
            verifyNoInteractions(authServerGrpcClient);
            verifyNoInteractions(organizationUserRegistrationRequestService);
            verifyNoInteractions(emailTokenService);
            verifyNoInteractions(messagingService);
        }

        @Test
        @DisplayName("registerNewOrganizationUser: New organization user is registered successfully (positive scenario)")
        void givenValidOrgUserData_whenRegistering_thenEverythingIsSavedAndDtoReturned() {
            // given
            var request = createCreateNewOrganizationUserRequestDto(
                    NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME,
                    TestData.ORG_REG_NUMBER
            );

            var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
            when(authServerGrpcClient.authServerNewOrganizationUserRegistration(
                    any(CreateNewOrganizationAuthUserRequestDto.class),
                    eq(RoleType.SIMPLE_USER))
            ).thenReturn(authUserResponse);

            doNothing().when(messagingService).sendToEmailService(any(EmailDto.class));

            long initialCount = userProfileRepository.count();

            // when
            var response = client.post()
                    .uri("/register/user/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(RegisteredUserResponseDto.class)
                    .returnResult()
                    .getResponseBody();

            long finalCount = userProfileRepository.count();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(NEW_EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(NEW_MOBILE);
            assertThat(finalCount).isEqualTo(initialCount + 1);

            var savedProfile = userProfileRepository.findByEmail(NEW_EMAIL);
            assertThat(savedProfile).isPresent();
            assertThat(savedProfile.get().getEmail()).isEqualTo(NEW_EMAIL);
            assertThat(savedProfile.get().getMobileNumber()).isEqualTo(NEW_MOBILE);
            assertThat(savedProfile.get().getName()).isEqualTo(NEW_NAME);
            assertThat(savedProfile.get().getSurname()).isEqualTo(NEW_SURNAME);
            assertThat(savedProfile.get().getId()).isEqualTo(AUTH_USER_ID);

            verify(organizationService, times(1)).findByRegistrationNumber(TestData.ORG_REG_NUMBER);

            verify(authServerGrpcClient, times(1))
                    .authServerNewOrganizationUserRegistration(
                            argThat(dto ->
                                    dto.email().equals(NEW_EMAIL) &&
                                            dto.mobileNumber().equals(NEW_MOBILE) &&
                                            dto.password().equals(PWD) &&
                                            dto.organizationRegNumber().equals(TestData.ORG_REG_NUMBER)
                            ),
                            eq(RoleType.SIMPLE_USER)
                    );

            verify(emailTokenService, times(1))
                    .createOrganizationUserToken(any(), eq(AUTH_USER_ID), eq(NEW_EMAIL), eq(AccountConfirmedBy.USER));

            verify(organizationUserRegistrationRequestService, times(1))
                    .saveOrganizationUserRegistrationRequest(argThat(req ->
                            TestData.ORG_REG_NUMBER.equals(req.getOrganizationRegistrationNumber()) &&
                                    NEW_EMAIL.equals(req.getOrgUserEmail())
                    ));

            verify(messagingService, times(1)).sendToEmailService(any(EmailDto.class));
            verifyNoInteractions(sagaCompensationActionService);
        }

        @Nested
        class OrganizationUserSagaStepFailureTests {

            @Test
            @DisplayName("registerNewOrganizationUser - Step 1 fail: Auth server registration fails - No compensation needed")
            void whenAuthServerRegistrationFails_thenNoCompensationTriggered() {
                // given
                var request = createCreateNewOrganizationUserRequestDto(
                        NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME, TestData.ORG_REG_NUMBER
                );

                when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER)))
                        .thenThrow(new RuntimeException("Auth server unavailable"));

                long initialProfiles = userProfileRepository.count();
                long initialRequests = organizationUserRegistrationRequestRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalProfiles = userProfileRepository.count();
                long finalRequests = organizationUserRegistrationRequestRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalProfiles).isEqualTo(initialProfiles);
                assertThat(finalRequests).isEqualTo(initialRequests);

                verify(organizationService, times(1)).findByRegistrationNumber(TestData.ORG_REG_NUMBER);
                verify(authServerGrpcClient, times(1)).authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER));

                verify(userProfileService, never()).saveUserProfile(any());
                verify(organizationUserRegistrationRequestService, never()).saveOrganizationUserRegistrationRequest(any());
                verify(emailTokenService, never()).createOrganizationUserToken(any(), anyString(), anyString(), any());
                verify(messagingService, never()).sendToEmailService(any());

                verifyNoInteractions(sagaCompensationActionService);
            }

            @Test
            @DisplayName("registerNewOrganizationUser - Step 2 fail: User profile save fails - Auth server rollback triggered")
            void whenUserProfileSaveFails_thenAuthServerRollbackTriggered() {
                // given
                var request = createCreateNewOrganizationUserRequestDto(
                        NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME, TestData.ORG_REG_NUMBER
                );

                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER)))
                        .thenReturn(authUserResponse);

                doThrow(new DataIntegrityViolationException("DB DED"))
                        .when(userProfileService).saveUserProfile(any(UserProfile.class));

                long initialProfiles = userProfileRepository.count();
                long initialRequests = organizationUserRegistrationRequestRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalProfiles = userProfileRepository.count();
                long finalRequests = organizationUserRegistrationRequestRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalProfiles).isEqualTo(initialProfiles);
                assertThat(finalRequests).isEqualTo(initialRequests);

                verify(authServerGrpcClient, times(1)).authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER));
                verify(userProfileService, times(1)).saveUserProfile(any());
                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Organization user registration");

                verify(organizationUserRegistrationRequestService, never()).saveOrganizationUserRegistrationRequest(any());
                verify(emailTokenService, never()).createOrganizationUserToken(any(), anyString(), anyString(), any());
                verify(messagingService, never()).sendToEmailService(any());
            }

            @Test
            @DisplayName("registerNewOrganizationUser - Step 3 fail: Registration request save fails - Profile delete + Auth rollback")
            void whenRegistrationRequestSaveFails_thenProfileDeletedAndAuthRolledBack() {
                // given
                var request = createCreateNewOrganizationUserRequestDto(
                        NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME, TestData.ORG_REG_NUMBER
                );

                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER)))
                        .thenReturn(authUserResponse);

                doThrow(new RuntimeException("request table unavailable"))
                        .when(organizationUserRegistrationRequestService).saveOrganizationUserRegistrationRequest(any());

                long initialProfiles = userProfileRepository.count();
                long initialRequests = organizationUserRegistrationRequestRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalProfiles = userProfileRepository.count();
                long finalRequests = organizationUserRegistrationRequestRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalProfiles).isEqualTo(initialProfiles);
                assertThat(finalRequests).isEqualTo(initialRequests);

                verify(organizationUserRegistrationRequestService, times(1)).saveOrganizationUserRegistrationRequest(any());
                verify(userProfileService, times(1)).deleteUserProfile(any());
                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Organization user registration");

                verify(emailTokenService, never()).createOrganizationUserToken(any(), anyString(), anyString(), any());
                verify(messagingService, never()).sendToEmailService(any());
            }

            @Test
            @DisplayName("registerNewOrganizationUser - Step 4 fail: Token generation fails - Full rollback triggered")
            void whenTokenGenerationFails_thenFullRollbackTriggered() {
                // given
                var request = createCreateNewOrganizationUserRequestDto(
                        NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME, TestData.ORG_REG_NUMBER
                );

                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER)))
                        .thenReturn(authUserResponse);

                when(emailTokenService.createOrganizationUserToken(any(Organization.class), eq(AUTH_USER_ID), eq(NEW_EMAIL), eq(AccountConfirmedBy.USER)))
                        .thenThrow(new RuntimeException("Token generation failed"));

                long initialProfiles = userProfileRepository.count();
                long initialRequests = organizationUserRegistrationRequestRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalProfiles = userProfileRepository.count();
                long finalRequests = organizationUserRegistrationRequestRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalProfiles).isEqualTo(initialProfiles);
                assertThat(finalRequests).isEqualTo(initialRequests);

                verify(organizationUserRegistrationRequestService, times(1))
                        .deleteOrganizationUserRegistrationRequest(any());
                verify(userProfileService, times(1)).deleteUserProfile(any());
                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Organization user registration");

                verify(messagingService, never()).sendToEmailService(any());
            }

            @Test
            @DisplayName("registerNewOrganizationUser - Step 4 fail: Message service fails - Full rollback triggered")
            void whenMessageServiceFails_thenFullRollbackTriggered() {
                // given
                var request = createCreateNewOrganizationUserRequestDto(
                        NEW_EMAIL, NEW_MOBILE, NEW_NAME, NEW_SURNAME, TestData.ORG_REG_NUMBER
                );

                var authUserResponse = new AuthUserIdResponseDto(AUTH_USER_ID);
                when(authServerGrpcClient.authServerNewOrganizationUserRegistration(any(), eq(RoleType.SIMPLE_USER)))
                        .thenReturn(authUserResponse);

                when(emailTokenService.createOrganizationUserToken(any(Organization.class), eq(AUTH_USER_ID), eq(NEW_EMAIL), eq(AccountConfirmedBy.USER)))
                        .thenReturn(TEST_TOKEN);

                doThrow(new RuntimeException("Message broker unavailable"))
                        .when(messagingService).sendToEmailService(any(EmailDto.class));

                long initialProfiles = userProfileRepository.count();
                long initialRequests = organizationUserRegistrationRequestRepository.count();

                // when
                var response = client.post()
                        .uri("/register/user/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange();

                long finalProfiles = userProfileRepository.count();
                long finalRequests = organizationUserRegistrationRequestRepository.count();

                // then
                response.expectStatus().is5xxServerError();
                assertThat(finalProfiles).isEqualTo(initialProfiles);
                assertThat(finalRequests).isEqualTo(initialRequests);

                verify(emailTokenService, times(1))
                        .createOrganizationUserToken(any(Organization.class), eq(AUTH_USER_ID), eq(NEW_EMAIL), eq(AccountConfirmedBy.USER));
                verify(messagingService, times(1)).sendToEmailService(any());

                verify(organizationUserRegistrationRequestService, times(1))
                        .deleteOrganizationUserRegistrationRequest(any());
                verify(userProfileService, times(1)).deleteUserProfile(any());
                verify(sagaCompensationActionService, times(1))
                        .authServerCredentialsRollback(AUTH_USER_ID, NEW_EMAIL, "Organization user registration");
            }


        }


        private CreateNewOrganizationUserRequestDto createCreateNewOrganizationUserRequestDto(String email,
                                                                                              String mobileNumber,
                                                                                              String name,
                                                                                              String surname,
                                                                                              String organizationRegNumber) {
            return new CreateNewOrganizationUserRequestDto(email, mobileNumber, name, surname, PWD, organizationRegNumber);
        }

    }

}
