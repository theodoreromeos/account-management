package com.theodore.account.management.integration;

import com.theodore.account.management.enums.OrganizationRegistrationStatus;
import com.theodore.account.management.models.dto.requests.SearchRegistrationProcessRequestDto;
import com.theodore.account.management.models.dto.responses.RegistrationProcessResponseDto;
import com.theodore.account.management.models.dto.responses.SearchResponse;
import com.theodore.account.management.repositories.OrganizationRegistrationProcessRepository;
import com.theodore.account.management.utils.AccountManagementTestConfigs;
import com.theodore.account.management.utils.JwtTestUtils;
import com.theodore.racingmodel.entities.modeltypes.RoleType;
import com.theodore.racingmodel.enums.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@Import(AccountManagementTestConfigs.class)
class AdminActionsIT extends BasePostgresTest {

    private static final String SYS_ADMIN_EMAIL = "admin@system.com";
    private static final String NON_ADMIN_EMAIL = "user@company.com";

    @Autowired
    TestDataFeeder testDataFeeder;
    @Autowired
    OrganizationRegistrationProcessRepository organizationRegistrationProcessRepository;
    @Autowired
    private JwtTestUtils jwtTestUtils;
    @MockitoBean
    JwtDecoder jwtDecoder;

    WebTestClient client;

    Jwt validToken;
    Jwt expiredToken;
    Jwt invalidToken;

    @BeforeEach
    void initClient() {
        client = WebTestClient.bindToServer().baseUrl(baseUrl()).build();

        validToken = jwtTestUtils.createSimpleUserToken(SYS_ADMIN_EMAIL, RoleType.SYS_ADMIN.getScopeValue());
        expiredToken = jwtTestUtils.createExpiredToken(SYS_ADMIN_EMAIL, RoleType.SYS_ADMIN.getScopeValue());
        invalidToken = jwtTestUtils.createSimpleUserToken(NON_ADMIN_EMAIL, RoleType.SIMPLE_USER.getScopeValue());
    }

    @Nested
    class SearchOrganizationRegistrationRequestsTest {

        @BeforeEach
        void initClient() {
            testDataFeeder.feedOrganizationRegistrationProcess();
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
                            .path("/admin/org-registration/search")
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
        void givenInvalidToken_whenSearchingOrgRegistrationProcesses_returnForbidden() {
            // given
            when(jwtDecoder.decode(anyString())).thenReturn(invalidToken);
            var req = new SearchRegistrationProcessRequestDto(SearchRegistrationProcessRequestDto.SearchOrganizationRegistrationStatus.ALL
                    , null, null, null);

            // when and then
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/org-registration/search")
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


}
