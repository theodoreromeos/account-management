package com.theodore.account.management.integration;

import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.account.management.models.dto.responses.AuthUserIdResponseDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.models.dto.responses.SearchResponse;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.services.AuthServerGrpcClient;
import com.theodore.account.management.services.SagaCompensationActionService;
import com.theodore.account.management.services.UserProfileService;
import com.theodore.account.management.utils.AccountManagementTestConfigs;
import com.theodore.account.management.utils.JwtTestUtils;
import com.theodore.account.management.utils.TestData;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
import com.theodore.racingmodel.enums.Country;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import(AccountManagementTestConfigs.class)
class AdminActionsIT extends BasePostgresTest {

    @Autowired
    TestDataFeeder testDataFeeder;
    @Autowired
    OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    @Autowired
    private JwtTestUtils jwtTestUtils;

    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    AuthServerGrpcClient authServerGrpcClient;
    @MockitoBean
    SagaCompensationActionService sagaCompensationActionService;

    @MockitoSpyBean
    UserProfileService userProfileService;

    WebTestClient client;

    Jwt validToken;
    Jwt invalidToken;

    JwtValidationException expiredException;

    @BeforeAll
    void initClient() {
        client = WebTestClient.bindToServer().baseUrl(baseUrl()).build();
        validToken = jwtTestUtils.createSimpleUserToken(TestData.SYS_ADMIN_EMAIL, RoleType.SYS_ADMIN.getScopeValue());
        invalidToken = jwtTestUtils.createSimpleUserToken(TestData.NON_ADMIN_EMAIL, RoleType.SIMPLE_USER.getScopeValue());

        OAuth2Error error = new OAuth2Error("invalid_token", "Jwt expired at some point", null);
        expiredException = new JwtValidationException("JWT expired", List.of(error));
    }

    @Nested
    class SearchOrganizationRegistrationRequestsTest {

        private static final String URL = "/admin/org-registration/search";

        @BeforeEach
        void initClient() {
            testDataFeeder.feedOrganizationRegistrationProcess();
        }

        @AfterEach
        void cleanClient() {
            testDataFeeder.cleanOrganizationRegistrationProcess();
        }

        private static final int DEFAULT_PAGE = 0;
        private static final int DEFAULT_PAGE_SIZE = 10;

        private void assertSearchResults(Integer pageNumberParam,
                                         Integer pageSizeParam,
                                         SearchRegistrationProcessRequestDto request,
                                         int expectedTotalElements,
                                         int expectedCount,
                                         List<String> companies,
                                         OrganizationRegistrationStatus status) {
            //when
            int pageNumber = pageNumberParam != null ? pageNumberParam : DEFAULT_PAGE;
            int pageSize = pageSizeParam != null ? pageSizeParam : DEFAULT_PAGE_SIZE;

            SearchResponse<RegistrationProcessResponseDto> response = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(URL)
                            .queryParam("page", pageNumber)
                            .queryParam("pageSize", pageSize)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<SearchResponse<RegistrationProcessResponseDto>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getPageNumber()).isEqualTo(pageNumber);
            assertThat(response.getPageSize()).isEqualTo(pageSize);
            assertThat(response.getTotalElements()).isEqualTo(expectedTotalElements);

            var responseData = response.getData();

            assertThat(responseData).isNotNull();
            assertThat(responseData).hasSize(expectedCount);

            if (request.country() != null) {
                assertThat(responseData).allMatch(dto -> dto.getCountry().equals(request.country()));
            }
            if (status != null) {
                assertThat(responseData).allMatch(dto -> status.equals(dto.getAdminApproved()));
            }
            if (request.registrationNumber() != null) {
                assertThat(responseData)
                        .allMatch(dto -> request.registrationNumber().equals(dto.getRegistrationNumber()));
            }
            if (request.organizationName() != null) {
                assertThat(responseData)
                        .allMatch(dto -> request.organizationName().equals(dto.getOrganizationName()));
            }

            assertThat(response.getData())
                    .extracting(RegistrationProcessResponseDto::getOrganizationName)
                    .containsExactlyInAnyOrderElementsOf(companies);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: given invalid token forbidden is returned (negative scenario)")
        void givenInvalidToken_whenSearchingOrgRegistrationProcesses_returnForbidden() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(invalidToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL
                    , null, null, null);

            // when and then
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(URL)
                            .queryParam("page", DEFAULT_PAGE)
                            .queryParam("pageSize", DEFAULT_PAGE_SIZE)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: given expired token unauthorized is returned (negative scenario)")
        void givenExpiredToken_whenSearchingOrgRegistrationProcesses_returnUnauthorized() {
            // given
            when(jwtDecoder.decode(anyString())).thenThrow(expiredException);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL
                    , null, null, null);

            // when and then
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(URL)
                            .queryParam("page", DEFAULT_PAGE)
                            .queryParam("pageSize", DEFAULT_PAGE_SIZE)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: mismatched criteria no results are found and ok is returned (positive scenario)")
        void givenMismatchedCriteria_whenSearchingOrgRegistrationProcesses_returnOkAndNoResults() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL,
                    "CompanyName1", "REG-2", null);
            int expectedTotalElements = 0;
            int expectedCount = 0;
            List<String> companies = List.of();

            // when and then
            assertSearchResults(null,
                    null,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    null);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: status PENDING and country USA criteria results are returned (positive scenario)")
        void givenCriteriaStatusCountry_whenSearchingOrgRegistrationProcesses_returnResults() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.PENDING,
                    null, null, Country.USA);
            int expectedTotalElements = 3;
            int expectedCount = 3;
            List<String> companies = List.of("CompanyName3", "CompanyName2", "CompanyName1");
            OrganizationRegistrationStatus status = OrganizationRegistrationStatus.PENDING;

            // when and then
            assertSearchResults(null,
                    null,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    status);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: organizationName criteria result is returned (positive scenario)")
        void givenCriteriaOrgName_whenSearchingOrgRegistrationProcesses_returnResult() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL,
                    "CompanyName2", null, null);
            int expectedTotalElements = 1;
            int expectedCount = 1;
            List<String> companies = List.of("CompanyName2");

            // when and then
            assertSearchResults(null,
                    null,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    null);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: organizationRegistrationNumber criteria result is returned (positive scenario)")
        void givenCriteriaOrgRegNumber_whenSearchingOrgRegistrationProcesses_returnResult() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL,
                    null, "REG-5", null);
            int expectedTotalElements = 1;
            int expectedCount = 1;
            List<String> companies = List.of("CompanyName5");

            // when and then
            assertSearchResults(null,
                    null,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    null);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: no criteria - all results are returned (positive scenario)")
        void givenCriteriaAllResults_whenSearchingOrgRegistrationProcesses_returnResults() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL
                    , null, null, null);
            int expectedTotalElements = 15;
            int expectedCount = 10;
            List<String> companies = List.of("CompanyName6", "CompanyName7", "CompanyName8", "CompanyName9", "CompanyName10",
                    "CompanyName11", "CompanyName12", "CompanyName13", "CompanyName14", "CompanyName15");

            // when and then
            assertSearchResults(null,
                    null,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    null);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: no criteria - page 2 results are returned (positive scenario)")
        void givenCriteriaAllResults_whenSearchingOrgRegistrationProcessesPage2_returnResults() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL
                    , null, null, null);
            int expectedTotalElements = 15;
            int expectedCount = 5;
            List<String> companies = List.of("CompanyName6", "CompanyName7", "CompanyName8", "CompanyName9", "CompanyName10");

            // when and then
            assertSearchResults(1,
                    5,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    null);
        }

        @Test
        @DisplayName("searchOrgRegistrationProcesses: no criteria - page 3 results are returned (positive scenario)")
        void givenCriteriaAllResults_whenSearchingOrgRegistrationProcessesPage3_returnResults() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL
                    , null, null, null);
            int expectedTotalElements = 15;
            int expectedCount = 5;
            List<String> companies = List.of("CompanyName1", "CompanyName2", "CompanyName3", "CompanyName4", "CompanyName5");

            // when and then
            assertSearchResults(2,
                    5,
                    req,
                    expectedTotalElements,
                    expectedCount,
                    companies,
                    null);
        }


    }

    @Nested
    class AdminProfileManagementTest {

        private static final String URL = "/admin/manage";

        private static final String EXISTING_PWD = "Passw0rd$$$1";
        private static final String NEW_PWD = "Passw0rd$$$2";

        @BeforeEach
        void initClient() {
            testDataFeeder.feedUserProfileTable();
            reset(authServerGrpcClient, jwtDecoder, userProfileService);
        }

        @AfterEach
        void cleanClient() {
            testDataFeeder.cleanUserProfileTable();
        }

        @Test
        @DisplayName("adminProfileManagement: with invalid token forbidden is returned (negative scenario)")
        void givenInvalidToken_whenManagingAdminProfile_returnForbidden() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(invalidToken);
            var request = createUserChangeInformationRequestDto();

            // when and then
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(URL)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("adminProfileManagement: with expired token unauthorized is returned (negative scenario)")
        void givenExpiredToken_whenManagingAdminProfile_returnForbidden() {
            // given
            when(jwtDecoder.decode(anyString())).thenThrow(expiredException);
            var request = createUserChangeInformationRequestDto();

            // when and then
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(URL)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("adminProfileManagement: update successful with valid data (positive scenario)")
        void givenValidData_whenManagingAdminProfile_returnOk() {
            // given
            var userProfile = userProfileService.findByEmail(TestData.SYS_ADMIN_EMAIL).orElse(null);
            assertThat(userProfile).isNotNull();

            when(jwtDecoder.decode(anyString())).thenReturn(validToken);
            var request = createUserChangeInformationRequestDto();
            when(authServerGrpcClient.manageAuthServerUserAccount(any())).thenReturn(new AuthUserIdResponseDto(userProfile.getId()));

            // when
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(URL)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk();

            // then
            var updatedUserProfile = userProfileService.findByEmail(TestData.NEW_EMAIL).orElse(null);
            assertThat(updatedUserProfile).isNotNull();
            assertThat(updatedUserProfile.getEmail()).isEqualTo(TestData.NEW_EMAIL);
            assertThat(updatedUserProfile.getMobileNumber()).isEqualTo(TestData.NEW_MOBILE);
            assertThat(updatedUserProfile.getName()).isEqualTo(TestData.NEW_NAME);
            assertThat(updatedUserProfile.getSurname()).isEqualTo(TestData.NEW_SURNAME);

            verify(authServerGrpcClient, times(1)).manageAuthServerUserAccount(any());
            verify(userProfileService, times(1)).saveUserProfile(any());
            verifyNoInteractions(sagaCompensationActionService);
        }

        private UserChangeInformationRequestDto createUserChangeInformationRequestDto() {
            var request = new UserChangeInformationRequestDto();
            request.setOldEmail(TestData.SYS_ADMIN_EMAIL);
            request.setNewEmail(TestData.NEW_EMAIL);
            request.setOldPassword(EXISTING_PWD);
            request.setNewPassword(NEW_PWD);
            request.setName(TestData.NEW_NAME);
            request.setSurname(TestData.NEW_SURNAME);
            request.setPhoneNumber(TestData.NEW_MOBILE);
            return request;
        }

    }


}
