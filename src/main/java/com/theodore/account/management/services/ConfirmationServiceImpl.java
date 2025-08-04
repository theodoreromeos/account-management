package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.enums.RegistrationStatus;
import com.theodore.account.management.models.dto.requests.ConfirmOrgAdminEmailRequestDto;
import com.theodore.account.management.models.dto.responses.OrgAdminInfoResponseDto;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.user.ConfirmationStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConfirmationServiceImpl implements ConfirmationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationServiceImpl.class);

    private static final String PURPOSE = "purpose";
    private static final String EMAIL = "email";


    private final EmailTokenService emailTokenService;
    private final UserProfileService userProfileService;
    private final OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final MessagingService messagingService;

    public ConfirmationServiceImpl(EmailTokenService emailTokenService,
                                   UserProfileService userProfileService,
                                   OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService,
                                   AuthServerGrpcClient authServerGrpcClient,
                                   MessagingService messagingService) {
        this.emailTokenService = emailTokenService;
        this.userProfileService = userProfileService;
        this.organizationUserRegistrationRequestService = organizationUserRegistrationRequestService;
        this.authServerGrpcClient = authServerGrpcClient;
        this.messagingService = messagingService;
    }

    @Override
    public void confirmSimpleUserEmail(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.PERSONAL.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        String userId = claims.getBody().getSubject();

        String email = claims.getBody().get(EMAIL, String.class);

        checkUserProfileDetails(userId, email);

        //send to auth server that user is authenticated
        var response = authServerGrpcClient.authServerNewUserConfirmation(userId);
        if (!response.getConfirmationStatus().equals(ConfirmationStatus.CONFIRMED)) {
            throw new RuntimeException("confirmation failed");//todo: better exception or no exception
        }
        // send successful confirmation email - with rabbitmq to email service
        var successfulConfirmationEmail = new EmailDto(List.of(email),
                "User Registration Confirmation Successful",
                "User Account created successfully");
        messagingService.sendToEmailService(successfulConfirmationEmail);
    }

    @Override
    @Transactional
    public void confirmOrganizationUserEmailByUser(String token) {

        LOGGER.trace("confirmOrganizationUserEmail - token: {}", token);

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_USER.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        String userId = claims.getBody().getSubject();
        String email = claims.getBody().get(EMAIL, String.class);

        UserProfile user = checkAndGetUserProfile(userId, email);

        OrganizationUserRegistrationRequest registrationRequest = getOrganizationUserRegistrationRequest(email);

        if (!RegistrationStatus.PENDING_EMPLOYEE.equals(registrationRequest.getStatus())) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }
        registrationRequest.setStatus(RegistrationStatus.PENDING_COMPANY);

        organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);

        var adminInfoList = authServerGrpcClient
                .getOrganizationAdminInfoFromAuthServer(registrationRequest.getOrganizationRegistrationNumber());

        LOGGER.info("printing admin info list");
        if (adminInfoList != null) {
            adminInfoList.forEach(adminInfo -> System.out.println("id :" + adminInfo.orgAdminId()));
        }

        var emailList = new ArrayList<EmailDto>();

        String emailToken = emailTokenService.createOrganizationUserToken(user.getOrganization(),
                user.getId(),
                user.getEmail(),
                AccountConfirmedBy.ORGANIZATION);

        LOGGER.info("ORG USER TOKEN : {}", emailToken);

        for (OrgAdminInfoResponseDto adminInfo : adminInfoList) {
            String link = String.format("%s/organization/confirm?token=%s", baseUrl(), emailToken);//todo
            //send to email service for the organization to approve
            LOGGER.info("SENDING EMAIL TO : {}", adminInfo.email());//todo remove it later
            emailList.add(new EmailDto(List.of(adminInfo.email()), "User Registration Confirmation", link));
        }

    }

    @Override
    @Transactional
    public void confirmOrganizationUserEmailByOrganization(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_USER.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        String userId = claims.getBody().getSubject();

        String email = claims.getBody().get(EMAIL, String.class);

        String orgRegistrationNumber = claims.getBody().get("organization", String.class);

        checkOrganizationUserProfile(userId, email, orgRegistrationNumber);

        OrganizationUserRegistrationRequest registrationRequest = getOrganizationUserRegistrationRequest(email);

        if (!RegistrationStatus.PENDING_COMPANY.equals(registrationRequest.getStatus())) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        registrationRequest.setStatus(RegistrationStatus.APPROVED);

        organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);

        //send to auth server that user is authenticated
        var response = authServerGrpcClient.authServerNewUserConfirmation(userId);

        if (ConfirmationStatus.CONFIRMED.equals(response.getConfirmationStatus())) {
            LOGGER.info("EMAIL {} CONFIRMED", email);
            // send successful confirmation email - rabbitmq to email service
        } else {
            LOGGER.info("EMAIL {} CONFIRMATION FAILED", email);
            // throw exception
        }
    }

    @Override
    public void confirmOrganizationAdminEmail(ConfirmOrgAdminEmailRequestDto request, String token) {
        LOGGER.info("CONFIRMING ADMIN ACCOUNT");
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new JwtException("Password mismatch");//todo
        }
        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_ADMIN.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");//todo
        }
        String userId = claims.getBody().getSubject();

        String email = claims.getBody().get(EMAIL, String.class);

        checkUserProfileDetails(userId, email);

        var response = authServerGrpcClient.confirmAdminAccount(userId, request.oldPassword(), request.newPassword());
        LOGGER.info("response was  : {}", response.getConfirmationStatus());
    }


    private void checkUserProfileDetails(String userId, String email) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch");//todo: better exception
        }
    }

    private UserProfile checkAndGetUserProfile(String userId, String email) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch");//todo: better exception
        }
        return user;
    }

    private void checkOrganizationUserProfile(String userId, String email, String orgRegistrationNumber) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (email == null || orgRegistrationNumber == null) {
            throw new JwtException("email or reg number cannot be null");//todo: better exception
        }
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch : email different");//todo: better exception
        }
        if (user.getOrganization() == null
                || !orgRegistrationNumber.equals(user.getOrganization().getRegistrationNumber())) {
            throw new JwtException("Token mismatch : org registration number different");//todo: better exception
        }
    }

    private OrganizationUserRegistrationRequest getOrganizationUserRegistrationRequest(String email) {
        Optional<OrganizationUserRegistrationRequest> optionalRegistrationRequest = organizationUserRegistrationRequestService
                .findByOrganizationUserEmail(email);

        if (optionalRegistrationRequest.isEmpty()) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        return optionalRegistrationRequest.get();
    }

    private String baseUrl() {//todo remove it
        return "https://your-domain.com";  // or inject via @Value
    }

}
