package com.theodore.account.management.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.account.management.services.OrganizationRegistrationProcessService;
import com.theodore.account.management.services.ProfileManagementService;
import com.theodore.account.management.utils.EmailGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(AdminController.class)
@Import(ControllerTestConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OrganizationRegistrationProcessService organizationRegistrationProcessService;
    @MockitoBean
    ProfileManagementService profileManagementService;
    @MockitoBean(name = "emailGuard")
    EmailGuard emailGuard;

    private static final String URL = "/admin/manage";

    private static final String OLD_EMAIL = "old@mobilityapp.com";
    private static final String NEW_EMAIL = "new@mobilityapp.com";
    private static final String NAME = "name1";
    private static final String SURNAME = "name1";
    private static final String PHONE = "123456789";
    private static final String OLD_PASS = "oldP#s$wO0rd";
    private static final String NEW_PASS = "newP#s$wO0rd";

    @Test
    @WithMockUser(roles = "SYS_ADMIN")
    void givenAdminRole_whenManageAdminAccount_returnOk() throws Exception {
        when(emailGuard.isAllowed(any(), eq(OLD_EMAIL))).thenReturn(true);

        var dto = createUserChangeInformationRequestDto();

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(profileManagementService).adminProfileManagement(any(UserChangeInformationRequestDto.class));
    }


    @Test
    @WithMockUser(roles = "USER")
    void givenIncorrectRole_whenManageAdminAccount_returnForbidden() throws Exception {
        // given
        when(emailGuard.isAllowed(any(), anyString())).thenReturn(true);

        var dto = createUserChangeInformationRequestDto();

        // when and then
        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(profileManagementService);
    }

    @Test
    @WithMockUser(roles = "SYS_ADMIN")
    void givenInvalidOldEmail_whenManageAdminAccount_returnForbidden() throws Exception {
        // given
        when(emailGuard.isAllowed(any(), eq("old@example.com"))).thenReturn(false);

        var dto = createUserChangeInformationRequestDto();

        // when and then
        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(profileManagementService);
    }

    @Test
    @WithMockUser(roles = "SYS_ADMIN")
    void givenNoCsrf_whenManageAdminAccount_returnForbidden() throws Exception {
        // given
        when(emailGuard.isAllowed(any(), eq("old@example.com"))).thenReturn(true);

        var dto = createUserChangeInformationRequestDto();

        // when and then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(profileManagementService);
    }

    @Test
    @WithMockUser(roles = "SYS_ADMIN")
    void givenInvalidRequest_whenManageAdminAccount_returnBadRequest() throws Exception {
        // given
        when(emailGuard.isAllowed(any(), any())).thenReturn(true);

        var dto = createUserChangeInformationRequestDto();
        dto.setName("");

        // when and then
        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileManagementService);
    }

    private UserChangeInformationRequestDto createUserChangeInformationRequestDto(){
        var request = new UserChangeInformationRequestDto();
        request.setOldEmail(OLD_EMAIL);
        request.setNewEmail(NEW_EMAIL);
        request.setName(NAME);
        request.setSurname(SURNAME);
        request.setPhoneNumber(PHONE);
        request.setOldPassword(OLD_PASS);
        request.setNewPassword(NEW_PASS);
        return request;
    }

}
